package com.noom.meter_digital.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Commercial Licensing Service for Meter Digital Application.
 * Handles Machine Hardware Fingerprinting, License Key Verification, Registration, and Expiry Tracking.
 */
@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);
    private static final String SECRET_SALT = "METER_DIGITAL_PRO_SECRET_SALT_2026_KEY_GENERATOR";
    private static final String PREF_KEY_LICENSE = "METER_DIGITAL_LICENSE_KEY";

    private final Preferences prefs = Preferences.userNodeForPackage(LicenseService.class);

    /**
     * Generate Machine Hardware Fingerprint (Machine ID)
     */
    public String getMachineId() {
        try {
            String osName = System.getProperty("os.name", "");
            String osArch = System.getProperty("os.arch", "");
            String userName = System.getProperty("user.name", "");
            String rawHardwareInfo = osName + "-" + osArch + "-" + userName;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawHardwareInfo.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder("MD-");
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02X", hash[i]));
            }
            sb.append("-");
            for (int i = 4; i < 8; i++) {
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to generate Machine ID", e);
            return "MD-8F92-A3B1-9921";
        }
    }

    /**
     * Owner Tool: Generate Commercial License Key for Customer
     * Key Format: Base64(MachineID | MaxRooms | ExpiryDateyyyyMMdd | HashSignature)
     */
    public String generateLicenseKey(String targetMachineId, int maxRooms, int validDays) {
        if (targetMachineId == null) targetMachineId = getMachineId();
        
        LocalDate expiryDate = LocalDate.now().plusDays(validDays);
        String dateStr = expiryDate.format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        
        String payload = targetMachineId.trim().toUpperCase() + "|" + maxRooms + "|" + dateStr;
        String signature = computeHmacSignature(payload);
        
        String rawKey = payload + "|" + signature;
        return "MDLIC-" + Base64.getUrlEncoder().withoutPadding().encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Activate License Key
     */
    public Map<String, Object> activateLicense(String licenseKey) {
        Map<String, Object> result = new HashMap<>();
        
        if (licenseKey == null || licenseKey.isBlank()) {
            result.put("success", false);
            result.put("message", "กรุณากรอกรหัสเปิดใช้งาน License Key");
            return result;
        }

        Map<String, Object> status = validateLicenseKey(licenseKey.trim());
        if (Boolean.TRUE.equals(status.get("valid"))) {
            prefs.put(PREF_KEY_LICENSE, licenseKey.trim());
            result.put("success", true);
            result.put("message", "🎉 เปิดใช้งานโปรแกรม Meter Digital Pro สำเร็จเรียบร้อยแล้ว!");
            result.put("licenseInfo", status);
        } else {
            result.put("success", false);
            result.put("message", "❌ รหัส License Key ไม่ถูกต้อง หรือ ไม่ตรงกับ Machine ID เครื่องนี้: " + status.get("reason"));
        }

        return result;
    }

    /**
     * Check Current License Status
     */
    public Map<String, Object> getLicenseStatus() {
        String savedKey = prefs.get(PREF_KEY_LICENSE, "");
        if (savedKey.isBlank()) {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "TRIAL");
            status.put("statusDisplay", "🟢 โหมดทดลองใช้งาน (Trial Version)");
            status.put("machineId", getMachineId());
            status.put("maxRooms", 10);
            status.put("isActivated", false);
            return status;
        }

        Map<String, Object> valResult = validateLicenseKey(savedKey);
        valResult.put("machineId", getMachineId());
        return valResult;
    }

    /**
     * Validate License Key against current Machine ID and Expiry Date
     */
    public Map<String, Object> validateLicenseKey(String licenseKey) {
        Map<String, Object> res = new HashMap<>();
        res.put("valid", false);

        try {
            if (!licenseKey.startsWith("MDLIC-")) {
                res.put("reason", "รูปแบบ License Key ไม่ถูกต้อง (ต้องขึ้นต้นด้วย MDLIC-)");
                return res;
            }

            String encodedPart = licenseKey.substring(6);
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPart);
            String rawPayload = new String(decodedBytes, StandardCharsets.UTF_8);

            String[] parts = rawPayload.split("\\|");
            if (parts.length < 4) {
                res.put("reason", "องค์ประกอบ License Key ไม่สมบูรณ์");
                return res;
            }

            String keyMachineId = parts[0];
            int maxRooms = Integer.parseInt(parts[1]);
            String expiryDateStr = parts[2];
            String expectedSignature = parts[3];

            // Verify Machine ID
            String currentMachineId = getMachineId();
            if (!currentMachineId.equalsIgnoreCase(keyMachineId)) {
                res.put("reason", "License Key นี้ลงทะเบียนไว้กับเครื่องอื่น (Machine ID: " + keyMachineId + ")");
                return res;
            }

            // Verify Signature
            String rebuildPayload = keyMachineId + "|" + maxRooms + "|" + expiryDateStr;
            String computedSig = computeHmacSignature(rebuildPayload);
            if (!computedSig.equals(expectedSignature)) {
                res.put("reason", "ลายเซ็นดิจิทัล License Key ถือเป็นเท็จ (ถูกแก้ไขดัดแปลง)");
                return res;
            }

            // Verify Expiry Date
            LocalDate expiryDate = LocalDate.parse(expiryDateStr, DateTimeFormatter.BASIC_ISO_DATE);
            LocalDate now = LocalDate.now();

            if (now.isAfter(expiryDate)) {
                res.put("status", "EXPIRED");
                res.put("statusDisplay", "🔴 License หมดอายุการใช้งานเมื่อ " + expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                res.put("reason", "License หมดอายุเมื่อ " + expiryDate);
                res.put("isActivated", false);
                return res;
            }

            res.put("valid", true);
            res.put("status", "ACTIVATED");
            res.put("statusDisplay", "✨ License Pro เปิดใช้งานแล้ว (หมดอายุ: " + expiryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
            res.put("maxRooms", maxRooms);
            res.put("expiryDate", expiryDate.toString());
            res.put("isActivated", true);

        } catch (Exception e) {
            log.error("Failed to parse license key", e);
            res.put("reason", "เกิดข้อผิดพลาดในการตรวจสอบรหัส: " + e.getMessage());
        }

        return res;
    }

    private String computeHmacSignature(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((payload + SECRET_SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "SIGN_ERR";
        }
    }
}
