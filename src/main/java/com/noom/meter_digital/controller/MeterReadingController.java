package com.noom.meter_digital.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.noom.meter_digital.dto.RoomMeterSummary;
import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.entity.PendingMeterPhoto;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.GatewayDeviceRepository;
import com.noom.meter_digital.repository.MeterReadingRepository;
import com.noom.meter_digital.repository.PendingMeterPhotoRepository;
import com.noom.meter_digital.repository.RoomRepository;
import com.noom.meter_digital.service.LineMessagingService;
import com.noom.meter_digital.service.MeterOcrService;

@RestController
@RequestMapping("/api/meter")
public class MeterReadingController {

    private static final Path PENDING_UPLOAD_DIR = Paths.get("uploads", "pending").toAbsolutePath().normalize();

    @Autowired
    private MeterReadingRepository repository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MeterOcrService ocrService;

    @Autowired
    private LineMessagingService lineMessagingService;

    @Autowired
    private PendingMeterPhotoRepository pendingPhotoRepository;

    @Autowired
    private GatewayDeviceRepository deviceRepository;

    @PostMapping(value = "/ocr-scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ocrScan(@RequestParam("file") MultipartFile file) {
        return ocrService.processMeterPhotoOffline(file);
    }

    @PostMapping(value = "/ocr-scan-cloud", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ocrScanCloud(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "apiKey", required = false) String apiKey) {
        return ocrService.processMeterPhotoCloudAI(file, apiKey);
    }

    /**
     * Unified Mobile Meter Photo Submit Endpoint
     * Option 1: targetMode = DIRECT_OCR -> Send photo directly to Spring Boot OCR
     * Option 2: targetMode = LINE_FLEX_MESSAGE -> Send photo to LINE Owner via Interactive Flex Message
     */
    @PostMapping(value = "/submit-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> submitMeterPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "roomNumber", defaultValue = "101") String roomNumber,
            @RequestParam(value = "targetMode", defaultValue = "DIRECT_OCR") String targetMode,
            @RequestParam(value = "manualReading", required = false) Double manualReading,
            @RequestParam(value = "useCloudAI", required = false) Boolean useCloudAI,
            @RequestParam(value = "apiKey", required = false) String apiKey) {

        Map<String, Object> result = new HashMap<>();

        if (file == null || file.isEmpty()) {
            result.put("success", false);
            result.put("message", "กรุณาถ่ายรูปหรือเลือกรูปภาพมิเตอร์ก่อนส่ง");
            return result;
        }

        String cleanedRoom = roomNumber != null ? roomNumber.trim() : "101";
        boolean effectiveCloud = (useCloudAI != null) ? useCloudAI.booleanValue() : (ocrService.isCloudAiConfigured() || (apiKey != null && !apiKey.isBlank()));

        if ("LINE_FLEX_MESSAGE".equalsIgnoreCase(targetMode) || "LINE_OWNER".equalsIgnoreCase(targetMode)) {
            // Option 2: Send photo to Dorm Owner's LINE Flex Message for approval
            try {
                Files.createDirectories(PENDING_UPLOAD_DIR);
                String timestampStr = String.valueOf(System.currentTimeMillis());
                String photoId = "PENDING_" + timestampStr + "_" + cleanedRoom;
                String fileName = photoId + ".jpg";
                Path targetPath = PENDING_UPLOAD_DIR.resolve(fileName);

                file.transferTo(targetPath.toFile());

                // Run OCR immediately or use manual override if technician edited it
                Double detectedReading = manualReading;
                String rawText = manualReading != null ? manualReading.toString() : "";

                if (detectedReading == null) {
                    Map<String, Object> ocrRes = effectiveCloud ? ocrService.processMeterPhotoCloudAI(targetPath, apiKey) : ocrService.processMeterPhoto(targetPath);
                    if (ocrRes.get("meterReading") instanceof Number) {
                        detectedReading = ((Number) ocrRes.get("meterReading")).doubleValue();
                    }
                    rawText = (String) ocrRes.getOrDefault("rawText", "");
                }

                PendingMeterPhoto pendingPhoto = new PendingMeterPhoto(
                        photoId,
                        cleanedRoom,
                        targetPath.toString(),
                        file.getOriginalFilename()
                );
                pendingPhoto.setDetectedReading(detectedReading);
                pendingPhoto.setRawText(rawText);
                pendingPhotoRepository.save(pendingPhoto);

                // Dispatch LINE Flex Message with Interactive Buttons & OCR Reading Value
                Map<String, Object> lineRes = lineMessagingService.sendFlexMessageToOwner(pendingPhoto);

                result.put("success", true);
                result.put("targetMode", "LINE_FLEX_MESSAGE");
                result.put("photoId", photoId);
                result.put("roomNumber", cleanedRoom);
                result.put("detectedReading", detectedReading);
                result.put("rawText", rawText);
                result.put("message", lineRes.get("message"));
                result.put("simulated", lineRes.get("simulated"));
                result.put("flexPayload", lineRes.get("flexPayload"));
                return result;

            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "เกิดข้อผิดพลาดในการบันทึกรูปภาพรออนุมัติ: " + e.getMessage());
                return result;
            }
        } else {
            // Option 1: Process Direct OCR via Spring Boot immediately
            Map<String, Object> ocrResult;
            if (effectiveCloud) {
                ocrResult = ocrService.processMeterPhotoCloudAI(file, apiKey);
            } else {
                ocrResult = ocrService.processMeterPhotoOffline(file);
            }

            if (Boolean.TRUE.equals(ocrResult.get("success"))) {
                Double readingVal = 0.0;
                if (ocrResult.get("meterReading") instanceof Number) {
                    readingVal = ((Number) ocrResult.get("meterReading")).doubleValue();
                }

                // Auto save to DB
                Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(cleanedRoom).stream().findFirst().orElse(null);
                MeterReading meter = new MeterReading();
                meter.setRoom(room);
                meter.setRoomNumber(cleanedRoom);
                meter.setMeterId("METER-" + cleanedRoom);
                meter.setUnit(readingVal);
                meter.setEnergy(readingVal);
                meter.setMetervalue(readingVal);
                meter.setTimestamp(LocalDateTime.now());
                meter.setMonth(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));

                repository.save(meter);

                ocrResult.put("savedToDb", true);
                ocrResult.put("roomNumber", cleanedRoom);
                ocrResult.put("targetMode", "DIRECT_OCR");
            }

            return ocrResult;
        }
    }

    /**
     * Webhook for LINE Messaging API
     */
    @PostMapping(value = "/line/webhook")
    public Map<String, Object> lineWebhook(@RequestBody Map<String, Object> payload) {
        return lineMessagingService.handleWebhookEvent(payload);
    }

    @GetMapping("/pending-photos")
    public List<PendingMeterPhoto> getPendingPhotos() {
        return pendingPhotoRepository.findByStatusOrderByCreatedAtDesc("PENDING_APPROVAL");
    }

    @PostMapping("/approve-pending/{photoId}")
    public Map<String, Object> approvePendingPhoto(@PathVariable String photoId) {
        return lineMessagingService.approveAndProcessOcr(photoId);
    }

    @GetMapping(value = "/image/{photoId}", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public byte[] getPendingPhotoJpeg(@PathVariable String photoId) {
        try {
            java.util.Optional<PendingMeterPhoto> opt = pendingPhotoRepository.findByPhotoId(photoId);
            if (opt.isPresent() && opt.get().getImagePath() != null) {
                Path p = Paths.get(opt.get().getImagePath());
                if (Files.exists(p)) {
                    return Files.readAllBytes(p);
                }
            }
            Path lastPhoto = Paths.get("uploads", "ocr", "last_meter.jpg").toAbsolutePath();
            if (Files.exists(lastPhoto)) {
                return Files.readAllBytes(lastPhoto);
            }
        } catch (Exception e) {
            // ignore
        }
        return new byte[0];
    }

    @PostMapping
    public MeterReading save(@RequestBody MeterReading meter) {
        String requestedRoom = meter.getRoomNumber();
        if ((requestedRoom == null || requestedRoom.isBlank()) && meter.getRoom() != null) {
            requestedRoom = meter.getRoom().getRoomNumber();
        }
        if (requestedRoom == null || requestedRoom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomNumber is required (example: 101)");
        }
        String roomNumber = requestedRoom.trim();
        Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(roomNumber).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room " + roomNumber + " not found"));

        meter.setRoom(room);
        meter.setRoomNumber(roomNumber);
        meter.setTimestamp(LocalDateTime.now());

        if (meter.getMeterId() == null || meter.getMeterId().isBlank()) {
            meter.setMeterId("METER-" + roomNumber);
        }
        if (meter.getUnit() == null && meter.getEnergy() != null) {
            meter.setUnit(meter.getEnergy());
        }

        return repository.save(meter);
    }

    @GetMapping
    public List<MeterReading> getAll() {
        return repository.findTop50ByOrderByTimestampDesc();
    }

    @GetMapping("/latest")
    public MeterReading latest(@RequestParam(required = false) String roomNumber) {
        if (roomNumber != null && !roomNumber.isBlank()) {
            Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(roomNumber.trim()).stream()
                    .findFirst()
                    .orElse(null);
            if (room != null) {
                MeterReading roomReading = repository.findTopByRoomIdOrderByTimestampDesc(room.getId());
                if (roomReading != null) {
                    return roomReading;
                }
            }
        }
        return repository.findTopByOrderByTimestampDesc();
    }

    @GetMapping("/latest-by-room")
    public List<RoomMeterSummary> latestByRoom() {
        return roomRepository.findAll().stream()
                .map(room -> toSummary(room, repository.findTopByRoomIdOrderByTimestampDesc(room.getId())))
                .collect(Collectors.toList());
    }

    @GetMapping("/network-mode")
    public Map<String, String> getNetworkMode(@RequestParam(defaultValue = "METER001") String meterId) {
        return java.util.Collections.singletonMap("mode", "WIFI");
    }

    private RoomMeterSummary toSummary(Room room, MeterReading reading) {
        String mode = "MODE_ESP32_WIFI";
        String label = "ESP32 (Wi-Fi)";

        if (room != null) {
            com.noom.meter_digital.entity.GatewayDevice device = deviceRepository.findAll().stream()
                    .filter(d -> room.getRoomNumber().equals(d.getTargetRoomNumber()))
                    .findFirst()
                    .orElse(null);

            if (device != null && device.getReaderMode() != null) {
                mode = device.getReaderMode();
                if ("MODE_ESP32_LAN".equalsIgnoreCase(mode)) label = "ESP32 (LAN)";
                else if ("MODE_MODBUS_WIFI".equalsIgnoreCase(mode)) label = "Converter (Modbus TCP Wi-Fi)";
                else if ("MODE_MODBUS_LAN".equalsIgnoreCase(mode)) label = "Converter (Modbus TCP LAN)";
                else if ("MODE_MOXA_SERIAL".equalsIgnoreCase(mode) || (mode != null && mode.startsWith("MODE_MOXA"))) label = "MOXA Gateway (Modbus Serial)";
                else label = "ESP32 (Wi-Fi)";
            }
        }

        if (reading == null) {
            return new RoomMeterSummary(room.getId(), room.getRoomNumber(), "METER-" + room.getRoomNumber(),
                    null, null, null, null, null, mode, label);
        }
        return new RoomMeterSummary(room.getId(), room.getRoomNumber(), reading.getMeterId(), reading.getUnit(),
                reading.getVoltage(), reading.getCurrent(), reading.getPower(), reading.getTimestamp(), mode, label);
    }
}
