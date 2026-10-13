package com.noom.meter_digital.controller;

import com.noom.meter_digital.dto.MeterSingleReading;
import com.noom.meter_digital.dto.MeterTelemetryRequest;
import com.noom.meter_digital.entity.GatewayDevice;
import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.GatewayDeviceRepository;
import com.noom.meter_digital.repository.MeterReadingRepository;
import com.noom.meter_digital.repository.RoomRepository;
import com.noom.meter_digital.service.MeterNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/meters")
public class MeterTelemetryController {

    private static final Logger log = LoggerFactory.getLogger(MeterTelemetryController.class);

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GatewayDeviceRepository deviceRepository;

    @Autowired
    private MeterNotificationService notificationService;

    /**
     * Mode 1 (ESP32 Wi-Fi Push) & Mode 2 (ESP32 LAN Push) Endpoint
     */
    @PostMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> receiveTelemetry(@RequestBody MeterTelemetryRequest payload) {
        if (payload == null || payload.getReadings() == null || payload.getReadings().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload must contain telemetry readings");
        }

        String deviceId = payload.getDeviceId();
        String connType = payload.getConnectionType(); // WIFI, LAN
        LocalDateTime timestamp = LocalDateTime.now();

        if (payload.getTimestamp() != null && !payload.getTimestamp().isBlank()) {
            try {
                timestamp = LocalDateTime.parse(payload.getTimestamp(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                // Fallback to server now
            }
        }

        // Update or register Gateway Device status
        if (deviceId != null && !deviceId.isBlank()) {
            GatewayDevice device = deviceRepository.findByDeviceId(deviceId).orElseGet(() -> {
                GatewayDevice newDev = new GatewayDevice();
                newDev.setDeviceId(deviceId);
                newDev.setDeviceName("ESP32 Device " + deviceId);
                return newDev;
            });

            String mode = "LAN".equalsIgnoreCase(connType) ? "MODE_ESP32_LAN" : "MODE_ESP32_WIFI";
            device.setReaderMode(mode);
            device.setStatus("ONLINE");
            device.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device);
        }

        List<MeterReading> savedReadings = new ArrayList<>();

        for (MeterSingleReading r : payload.getReadings()) {
            String roomNumber = r.getRoomNumber();
            if (roomNumber == null || roomNumber.isBlank()) continue;

            Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(roomNumber.trim()).stream()
                    .findFirst()
                    .orElseGet(() -> {
                        Room newRoom = new Room();
                        newRoom.setRoomNumber(roomNumber.trim());
                        newRoom.setTenantName("ว่าง");
                        newRoom.setRatePerUnit(8.0);
                        return roomRepository.save(newRoom);
                    });

            MeterReading reading = new MeterReading();
            reading.setRoom(room);
            reading.setRoomNumber(roomNumber.trim());
            reading.setMeterId(r.getMeterId() != null ? r.getMeterId() : "METER-" + roomNumber);
            reading.setUnit(r.getEnergyKwh());
            reading.setEnergy(r.getEnergyKwh());
            reading.setValue(r.getEnergyKwh());
            reading.setVoltage(r.getVoltage());
            reading.setCurrent(r.getCurrent());

            Double power = r.getPower();
            if (power == null && r.getVoltage() != null && r.getCurrent() != null) {
                power = (r.getVoltage() * r.getCurrent()) / 1000.0;
            }
            reading.setPower(power);
            reading.setTimestamp(timestamp);
            reading.setRecordDate(LocalDate.now());
            reading.setMonth(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM")));

            MeterReading saved = meterReadingRepository.save(reading);
            savedReadings.add(saved);

            // Broadcast real-time update
            notificationService.broadcastTelemetryUpdate(saved);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("processedCount", savedReadings.size());
        response.put("serverTime", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * SSE Stream Endpoint for Real-time Dashboard Updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMeterTelemetry() {
        return notificationService.subscribe();
    }
}
