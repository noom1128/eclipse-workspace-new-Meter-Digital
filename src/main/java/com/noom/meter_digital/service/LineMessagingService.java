package com.noom.meter_digital.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.entity.PendingMeterPhoto;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.MeterReadingRepository;
import com.noom.meter_digital.repository.PendingMeterPhotoRepository;
import com.noom.meter_digital.repository.RoomRepository;

@Service
public class LineMessagingService {

    private static final Logger log = LoggerFactory.getLogger(LineMessagingService.class);
    private static final String LINE_PUSH_API = "https://api.line.me/v2/bot/message/push";
    private static final String LINE_REPLY_API = "https://api.line.me/v2/bot/message/reply";

    @Value("${line.bot.channel-access-token:qe28MsrrF5/upOBMIbH5s9kPCrxtL8JOCOO+zJ8I5DX5Nv97xCldYmbCjPvWlgnUh18zovWruuKJ5TuLkMp9hWHmaAiaNLIRmraH/QFk4Ft5nevTtY7411mmdcwxlmXV5I9Bcxu/k9L2L9FdEpk+hgdB04t89/1O/w1cDnyilFU=}")
    private String channelAccessToken;

    @Value("${line.bot.owner-user-id:U0bb9e8d05c915824ccfd2dc375cbb675}")
    private String ownerUserId;

    @Autowired
    private PendingMeterPhotoRepository pendingPhotoRepository;

    @Autowired
    private MeterOcrService meterOcrService;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Send LINE Flex Message to Dorm Owner containing the photo metadata & Interactive Buttons
     */
    public Map<String, Object> sendFlexMessageToOwner(PendingMeterPhoto pendingPhoto) {
        Map<String, Object> response = new HashMap<>();

        log.info("Preparing LINE Flex Message for Room {} (PhotoID: {})", pendingPhoto.getRoomNumber(), pendingPhoto.getPhotoId());

        String flexJson = buildFlexMessageJson(pendingPhoto);

        if (channelAccessToken == null || channelAccessToken.isBlank() || ownerUserId == null || ownerUserId.isBlank()) {
            log.warn("⚠️ LINE Channel Access Token or Owner User ID not configured. Simulating Flex Message dispatch.");
            response.put("success", true);
            response.put("simulated", true);
            response.put("photoId", pendingPhoto.getPhotoId());
            response.put("flexPayload", flexJson);
            response.put("message", "📲 จำลองการส่ง LINE Flex Message สำเร็จ (กรอก Token ใน application.properties หรือ UI เพื่อเปิดใช้งานยิง LINE จริง)");
            return response;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(channelAccessToken.trim());

            String body = "{"
                    + "\"to\": \"" + ownerUserId.trim() + "\","
                    + "\"messages\": [" + flexJson + "]"
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(LINE_PUSH_API, entity, String.class);

            response.put("success", true);
            response.put("simulated", false);
            response.put("photoId", pendingPhoto.getPhotoId());
            response.put("message", "✅ ส่ง LINE Flex Message ให้เจ้าของหอพักเรียบร้อยแล้ว");
        } catch (Exception e) {
            log.error("Failed to send LINE Flex Message: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "เกิดข้อผิดพลาดในการส่ง LINE Message: " + e.getMessage());
            response.put("simulatedFallbackPayload", flexJson);
        }

        return response;
    }

    /**
     * Handle LINE Webhook events (Postback / Message)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleWebhookEvent(Map<String, Object> webhookBody) {
        Map<String, Object> result = new HashMap<>();
        result.put("handled", false);

        if (webhookBody == null || !webhookBody.containsKey("events")) {
            result.put("message", "No events found in payload");
            return result;
        }

        List<Map<String, Object>> events = (List<Map<String, Object>>) webhookBody.get("events");
        for (Map<String, Object> event : events) {
            String type = (String) event.get("type");
            String replyToken = (String) event.get("replyToken");

            if ("postback".equalsIgnoreCase(type) && event.containsKey("postback")) {
                Map<String, Object> postback = (Map<String, Object>) event.get("postback");
                String dataStr = (String) postback.get("data");

                Map<String, String> queryParams = parseQueryString(dataStr);
                String action = queryParams.get("action");
                String photoId = queryParams.get("photoId");

                if ("APPROVE_OCR".equalsIgnoreCase(action) && photoId != null) {
                    Map<String, Object> approveRes = approveAndProcessOcr(photoId);
                    if (Boolean.TRUE.equals(approveRes.get("success"))) {
                        replyLineMessage(replyToken, "✅ เจ้าของหอพักกดอนุมัติรูปมิเตอร์แล้ว!\n"
                                + "📍 ห้อง: " + approveRes.get("roomNumber") + "\n"
                                + "⚡ ค่ามิเตอร์ที่อ่านได้ (OCR): " + approveRes.get("meterReading") + " kWh\n"
                                + "💾 บันทึกเข้าระบบเรียบร้อยแล้ว");
                    } else {
                        replyLineMessage(replyToken, "⚠️ เกิดข้อผิดพลาดในการประมวลผล OCR: " + approveRes.get("message"));
                    }
                    result.put("handled", true);
                    result.put("action", "APPROVE_OCR");
                    result.put("photoId", photoId);
                } else if ("REJECT_OCR".equalsIgnoreCase(action) && photoId != null) {
                    rejectPendingPhoto(photoId);
                    replyLineMessage(replyToken, "❌ เจ้าของหอพักปฏิเสธรูปภาพมิเตอร์ไอดี: " + photoId + "\n(เจ้าหน้าที่โปรดถ่ายใหม่อีกครั้ง)");
                    result.put("handled", true);
                    result.put("action", "REJECT_OCR");
                    result.put("photoId", photoId);
                }
            }
        }

        return result;
    }

    /**
     * Approve pending photo & process OCR immediately
     */
    public Map<String, Object> approveAndProcessOcr(String photoId) {
        Map<String, Object> result = new HashMap<>();

        Optional<PendingMeterPhoto> opt = pendingPhotoRepository.findByPhotoId(photoId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "ไม่พบข้อมูลรูปภาพมิเตอร์รหัส: " + photoId);
            return result;
        }

        PendingMeterPhoto pending = opt.get();
        if ("APPROVED".equalsIgnoreCase(pending.getStatus())) {
            result.put("success", true);
            result.put("message", "รูปภาพนี้ได้รับการอนุมัติไปแล้ว");
            result.put("meterReading", pending.getDetectedReading());
            result.put("roomNumber", pending.getRoomNumber());
            return result;
        }

        try {
            Path imagePath = Paths.get(pending.getImagePath());
            if (!Files.exists(imagePath)) {
                result.put("success", false);
                result.put("message", "ไม่พบไฟล์รูปภาพในดิสก์: " + pending.getImagePath());
                return result;
            }

            File file = imagePath.toFile();
            MultipartFile multipartFile = new FileMultipartFile(file, pending.getOriginalFileName(), "image/jpeg");

            // Run OCR Service Process
            Map<String, Object> ocrResult = meterOcrService.processMeterPhoto(multipartFile);

            Double readingVal = 0.0;
            if (ocrResult.get("meterReading") instanceof Number) {
                readingVal = ((Number) ocrResult.get("meterReading")).doubleValue();
            }

            // Save MeterReading to DB
            String roomNum = pending.getRoomNumber();
            Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(roomNum).stream().findFirst().orElse(null);

            MeterReading meterReading = new MeterReading();
            meterReading.setRoom(room);
            meterReading.setRoomNumber(roomNum);
            meterReading.setMeterId("METER-" + roomNum);
            meterReading.setUnit(readingVal);
            meterReading.setEnergy(readingVal);
            meterReading.setMetervalue(readingVal);
            meterReading.setTimestamp(LocalDateTime.now());
            meterReading.setMonth(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));

            meterReadingRepository.save(meterReading);

            // Update Pending Status
            pending.setStatus("APPROVED");
            pending.setDetectedReading(readingVal);
            pending.setRawText((String) ocrResult.get("rawText"));
            pending.setApprovedAt(LocalDateTime.now());
            pendingPhotoRepository.save(pending);

            result.put("success", true);
            result.put("photoId", photoId);
            result.put("roomNumber", roomNum);
            result.put("meterReading", readingVal);
            result.put("ocrMessage", ocrResult.get("message"));
            result.put("message", "อนุมัติและประมวลผล OCR สำเร็จ บันทึกหน่วยไฟ " + readingVal + " kWh");

        } catch (Exception e) {
            log.error("Failed to approve and process OCR for photoId {}: {}", photoId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "เกิดข้อผิดพลาด: " + e.getMessage());
        }

        return result;
    }

    public boolean rejectPendingPhoto(String photoId) {
        Optional<PendingMeterPhoto> opt = pendingPhotoRepository.findByPhotoId(photoId);
        if (opt.isPresent()) {
            PendingMeterPhoto pending = opt.get();
            pending.setStatus("REJECTED");
            pendingPhotoRepository.save(pending);
            return true;
        }
        return false;
    }

    private void replyLineMessage(String replyToken, String text) {
        if (replyToken == null || replyToken.isBlank() || channelAccessToken == null || channelAccessToken.isBlank()) {
            log.info("Line reply message simulated: {}", text);
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(channelAccessToken.trim());

            String body = "{"
                    + "\"replyToken\": \"" + replyToken + "\","
                    + "\"messages\": [{\"type\": \"text\", \"text\": \"" + escapeJson(text) + "\"}]"
                    + "}";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(LINE_REPLY_API, entity, String.class);
        } catch (Exception e) {
            log.error("Failed to send LINE reply: {}", e.getMessage());
        }
    }

    private String buildFlexMessageJson(PendingMeterPhoto photo) {
        String timeStr = photo.getCreatedAt() != null ? photo.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "-";
        String imgUrl = "http://10.221.143.108:8080/api/meter/image/" + photo.getPhotoId();
        String readingDisplay = photo.getDetectedReading() != null ? photo.getDetectedReading() + " kWh" : "รอประมวลผล";

        return "{"
                + "\"type\": \"flex\","
                + "\"altText\": \"📸 ตรวจสอบรูปภาพมิเตอร์ ห้อง " + photo.getRoomNumber() + "\","
                + "\"contents\": {"
                + "  \"type\": \"bubble\","
                + "  \"header\": {"
                + "    \"type\": \"box\","
                + "    \"layout\": \"vertical\","
                + "    \"backgroundColor\": \"#0f172a\","
                + "    \"contents\": ["
                + "      {\"type\": \"text\", \"text\": \"📸 ตรวจสอบรูปภาพมิเตอร์\", \"weight\": \"bold\", \"color\": \"#38bdf8\", \"size\": \"lg\"},"
                + "      {\"type\": \"text\", \"text\": \"คำขอจากเจ้าหน้าที่เดินจดมิเตอร์\", \"color\": \"#94a3b8\", \"size\": \"xs\", \"margin\": \"xs\"}"
                + "    ]"
                + "  },"
                + "  \"body\": {"
                + "    \"type\": \"box\","
                + "    \"layout\": \"vertical\","
                + "    \"contents\": ["
                + "      {\"type\": \"box\", \"layout\": \"baseline\", \"margin\": \"md\", \"contents\": ["
                + "        {\"type\": \"text\", \"text\": \"📍 ห้องที่:\", \"size\": \"sm\", \"color\": \"#64748b\", \"flex\": 2},"
                + "        {\"type\": \"text\", \"text\": \"ห้อง " + photo.getRoomNumber() + "\", \"size\": \"md\", \"weight\": \"bold\", \"color\": \"#1e293b\", \"flex\": 4}"
                + "      ]},"
                + "      {\"type\": \"box\", \"layout\": \"baseline\", \"margin\": \"md\", \"contents\": ["
                + "        {\"type\": \"text\", \"text\": \"⚡ อ่านได้ (OCR):\", \"size\": \"sm\", \"color\": \"#059669\", \"flex\": 2},"
                + "        {\"type\": \"text\", \"text\": \"" + readingDisplay + "\", \"size\": \"md\", \"weight\": \"bold\", \"color\": \"#10b981\", \"flex\": 4}"
                + "      ]},"
                + "      {\"type\": \"box\", \"layout\": \"baseline\", \"margin\": \"md\", \"contents\": ["
                + "        {\"type\": \"text\", \"text\": \"⏱️ เวลาถ่าย:\", \"size\": \"sm\", \"color\": \"#64748b\", \"flex\": 2},"
                + "        {\"type\": \"text\", \"text\": \"" + timeStr + "\", \"size\": \"sm\", \"color\": \"#334155\", \"flex\": 4}"
                + "      ]},"
                + "      {\"type\": \"box\", \"layout\": \"baseline\", \"margin\": \"md\", \"contents\": ["
                + "        {\"type\": \"text\", \"text\": \"🆔 ID รูป:\", \"size\": \"sm\", \"color\": \"#64748b\", \"flex\": 2},"
                + "        {\"type\": \"text\", \"text\": \"" + photo.getPhotoId() + "\", \"size\": \"xs\", \"color\": \"#0284c7\", \"flex\": 4}"
                + "      ]},"
                + "      {\"type\": \"separator\", \"margin\": \"lg\"},"
                + "      {\"type\": \"text\", \"text\": \"โปรดเปิดดูรูปเพื่อตรวจสอบ หากถูกต้องกด 'อนุมัติ & บันทึกค่านี้' เพื่อบันทึกลงระบบทันที\", \"wrap\": true, \"color\": \"#475569\", \"size\": \"xs\", \"margin\": \"md\"}"
                + "    ]"
                + "  },"
                + "  \"footer\": {"
                + "    \"type\": \"box\","
                + "    \"layout\": \"vertical\","
                + "    \"spacing\": \"sm\","
                + "    \"contents\": ["
                + "      {"
                + "        \"type\": \"button\","
                + "        \"style\": \"secondary\","
                + "        \"color\": \"#0284c7\","
                + "        \"action\": {"
                + "          \"type\": \"uri\","
                + "          \"label\": \"🔍 กดเปิดดูรูปถ่ายมิเตอร์บนมือถือ\","
                + "          \"uri\": \"" + imgUrl + "\""
                + "        }"
                + "      },"
                + "      {"
                + "        \"type\": \"button\","
                + "        \"style\": \"primary\","
                + "        \"color\": \"#10b981\","
                + "        \"action\": {"
                + "          \"type\": \"postback\","
                + "          \"label\": \"🟢 อนุมัติ & บันทึกค่านี้ (" + readingDisplay + ")\","
                + "          \"data\": \"action=APPROVE_OCR&photoId=" + photo.getPhotoId() + "\","
                + "          \"displayText\": \"อนุมัติค่ามิเตอร์ " + readingDisplay + " ห้อง " + photo.getRoomNumber() + "\""
                + "        }"
                + "      },"
                + "      {"
                + "        \"type\": \"button\","
                + "        \"style\": \"secondary\","
                + "        \"color\": \"#ef4444\","
                + "        \"action\": {"
                + "          \"type\": \"postback\","
                + "          \"label\": \"🔴 ปฏิเสธรูปนี้ (ให้ถ่ายใหม่)\","
                + "          \"data\": \"action=REJECT_OCR&photoId=" + photo.getPhotoId() + "\","
                + "          \"displayText\": \"ปฏิเสธรูปมิเตอร์ห้อง " + photo.getRoomNumber() + "\""
                + "        }"
                + "      }"
                + "    ]"
                + "  }"
                + "}"
                + "}";
    }

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    private static class FileMultipartFile implements MultipartFile {
        private final File file;
        private final String originalFilename;
        private final String contentType;

        public FileMultipartFile(File file, String originalFilename, String contentType) {
            this.file = file;
            this.originalFilename = originalFilename != null ? originalFilename : file.getName();
            this.contentType = contentType != null ? contentType : "image/jpeg";
        }

        @Override public String getName() { return file.getName(); }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return file.length() == 0; }
        @Override public long getSize() { return file.length(); }
        @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(file.toPath()); }
        @Override public java.io.InputStream getInputStream() throws IOException { return new FileInputStream(file); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException { Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
    }
}
