package com.noom.meter_digital.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.MeterReadingRepository;
import com.noom.meter_digital.repository.RoomRepository;

/**
 * AI Anomaly & Human Error Prevention Engine for Meter Readings.
 * Protects against:
 * 1. Wrong room meter photos (e.g. photo of Room 102 submitted as Room 101)
 * 2. Abnormal consumption spikes (e.g. 100 kWh -> 2,000 kWh = +1,900 units)
 * 3. Decimal point misreadings (e.g. 200.0 read as 2000, 10x slip)
 * 4. Unexpected drops in meter reading without rollover
 */
@Service
public class MeterAnomalyService {

    private final MeterReadingRepository meterReadingRepository;
    private final RoomRepository roomRepository;

    public MeterAnomalyService(MeterReadingRepository meterReadingRepository, RoomRepository roomRepository) {
        this.meterReadingRepository = meterReadingRepository;
        this.roomRepository = roomRepository;
    }

    public static class AnomalyResult {
        private boolean anomalyDetected;
        private String anomalyType; // NORMAL, ROOM_MISMATCH, EXTREME_SPIKE, DECIMAL_SLIP, ABNORMAL_DROP
        private String severity;    // NONE, WARNING, CRITICAL
        private String warningMessage;
        private Double previousReading;
        private Double currentReading;
        private Double deltaUnits;
        private Double suggestedReading;
        private boolean requiresConfirmation;

        public boolean isAnomalyDetected() { return anomalyDetected; }
        public void setAnomalyDetected(boolean anomalyDetected) { this.anomalyDetected = anomalyDetected; }
        public String getAnomalyType() { return anomalyType; }
        public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getWarningMessage() { return warningMessage; }
        public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
        public Double getPreviousReading() { return previousReading; }
        public void setPreviousReading(Double previousReading) { this.previousReading = previousReading; }
        public Double getCurrentReading() { return currentReading; }
        public void setCurrentReading(Double currentReading) { this.currentReading = currentReading; }
        public Double getDeltaUnits() { return deltaUnits; }
        public void setDeltaUnits(Double deltaUnits) { this.deltaUnits = deltaUnits; }
        public Double getSuggestedReading() { return suggestedReading; }
        public void setSuggestedReading(Double suggestedReading) { this.suggestedReading = suggestedReading; }
        public boolean isRequiresConfirmation() { return requiresConfirmation; }
        public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("anomalyDetected", anomalyDetected);
            m.put("anomalyType", anomalyType);
            m.put("severity", severity);
            m.put("warningMessage", warningMessage);
            m.put("previousReading", previousReading);
            m.put("currentReading", currentReading);
            m.put("deltaUnits", deltaUnits);
            m.put("suggestedReading", suggestedReading);
            m.put("requiresConfirmation", requiresConfirmation);
            return m;
        }
    }

    public AnomalyResult analyzeReading(String roomNumber, Double currentReading, String detectedRoomTagInPhoto) {
        AnomalyResult result = new AnomalyResult();
        result.setCurrentReading(currentReading);
        result.setAnomalyDetected(false);
        result.setAnomalyType("NORMAL");
        result.setSeverity("NONE");
        result.setRequiresConfirmation(false);

        if (roomNumber == null || roomNumber.isBlank() || currentReading == null) {
            return result;
        }

        String cleanedRoom = roomNumber.trim();

        // 1. Check Room Tag Mismatch from Photo OCR / AI Vision
        if (detectedRoomTagInPhoto != null && !detectedRoomTagInPhoto.isBlank()) {
            String detectedCleaned = detectedRoomTagInPhoto.replaceAll("[^0-9a-zA-Z]", "").trim();
            String targetCleaned = cleanedRoom.replaceAll("[^0-9a-zA-Z]", "").trim();
            if (!detectedCleaned.isEmpty() && !targetCleaned.equalsIgnoreCase(detectedCleaned)) {
                result.setAnomalyDetected(true);
                result.setAnomalyType("ROOM_MISMATCH");
                result.setSeverity("CRITICAL");
                result.setRequiresConfirmation(true);
                result.setWarningMessage(String.format(
                        "🚨 ป้ายห้องในภาพไม่ตรงกัน! AI ตรวจพบป้ายระบุ '%s' ในภาพ แต่วันนี้กำลังส่งข้อมูลให้ห้อง '%s' น่าจะถ่ายผิดห้อง",
                        detectedRoomTagInPhoto, cleanedRoom));
                return result;
            }
        }

        // 2. Lookup Previous Reading for this Room
        Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(cleanedRoom).stream().findFirst().orElse(null);
        MeterReading lastRecord = (room != null) ? meterReadingRepository.findTopByRoomIdOrderByTimestampDesc(room.getId()) : null;

        if (lastRecord == null || lastRecord.getUnit() == null) {
            // First time reading for this room
            result.setWarningMessage(String.format("บันทึกค่ามิเตอร์เริ่มต้นสำหรับห้อง %s (%.1f kWh)", cleanedRoom, currentReading));
            return result;
        }

        double prev = lastRecord.getUnit();
        result.setPreviousReading(prev);
        double delta = Math.round((currentReading - prev) * 100.0) / 100.0;
        result.setDeltaUnits(delta);

        // 3. Check for Abnormal Drop (Reading decreased without rollover)
        if (delta < -5.0) {
            // Check if plausible rollover (e.g. from 9990 to 10 on a 4 or 5 digit meter)
            boolean isRollover = (prev > 900.0 && currentReading < 200.0);
            if (!isRollover) {
                result.setAnomalyDetected(true);
                result.setAnomalyType("ABNORMAL_DROP");
                result.setSeverity("HIGH");
                result.setRequiresConfirmation(true);
                result.setWarningMessage(String.format(
                        "⚠️ ค่ามิเตอร์ลดลงผิดปกติ: ครั้งก่อน %.1f kWh ➔ ครั้งนี้ %.1f kWh (ลดลง %.1f หน่วย) น่าจะถ่ายผิดห้อง หรือมีประวัติเปลี่ยนมิเตอร์ใหม่",
                        prev, currentReading, Math.abs(delta)));
                return result;
            }
        }

        // 4. Check for Extreme Spike / Jump (e.g. 100 kWh -> 2000 kWh = +1,900 units)
        // Typical dorm room uses 20 - 400 kWh/month. Anything above 500 kWh delta is suspicious.
        if (delta > 500.0) {
            result.setAnomalyDetected(true);
            result.setRequiresConfirmation(true);

            // Check if decimal point slip (e.g. 115.0 was read as 1150, 10x slip)
            double dividedByTen = Math.round((currentReading / 10.0) * 10.0) / 10.0;
            double deltaIfDividedByTen = dividedByTen - prev;
            if (deltaIfDividedByTen >= 0.0 && deltaIfDividedByTen <= 150.0) {
                result.setAnomalyType("DECIMAL_SLIP");
                result.setSeverity("HIGH");
                result.setSuggestedReading(dividedByTen);
                result.setWarningMessage(String.format(
                        "⚠️ ค่ามิเตอร์กระโดดผิดปกติ (+%.1f หน่วย)! AI วิเคราะห์ว่าอาจลืมจุดทศนิยมหลักสุดท้าย (ค่าจริงน่าจะเป็น %.1f kWh แทนที่จะเป็น %.1f kWh)",
                        delta, dividedByTen, currentReading));
                return result;
            }

            result.setAnomalyType("EXTREME_SPIKE");
            result.setSeverity("CRITICAL");
            result.setWarningMessage(String.format(
                    "🚨 ค่ามิเตอร์กระโดดผิดปกติมาก! ครั้งก่อน %.1f kWh ➔ ครั้งนี้ %.1f kWh (เพิ่มขึ้นถึง +%.1f หน่วย) สูงเกินการใช้ไฟหอพักปกติ น่าจะถ่ายผิดห้อง หรือเป็นมิเตอร์รวมชั้น",
                    prev, currentReading, delta));
            return result;
        }

        // Normal reading difference
        result.setWarningMessage(String.format("ค่ามิเตอร์ปกติ: ครั้งก่อน %.1f kWh ➔ ครั้งนี้ %.1f kWh (ใช้ไป +%.1f หน่วย)",
                prev, currentReading, delta));
        return result;
    }
}
