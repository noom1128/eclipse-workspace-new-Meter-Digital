package com.noom.meter_digital.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.noom.meter_digital.service.LicenseService;

@RestController
@RequestMapping("/api/license")
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return licenseService.getLicenseStatus();
    }

    @PostMapping("/activate")
    public Map<String, Object> activate(@RequestBody Map<String, String> payload) {
        String key = payload != null ? payload.get("licenseKey") : null;
        return licenseService.activateLicense(key);
    }

    /**
     * Owner Key Generator Tool Endpoint (Admin / Owner Use)
     */
    @PostMapping("/generate-owner-key")
    public Map<String, Object> generateOwnerKey(
            @RequestParam(value = "machineId", required = false) String machineId,
            @RequestParam(value = "maxRooms", defaultValue = "100") int maxRooms,
            @RequestParam(value = "validDays", defaultValue = "365") int validDays) {
        
        String generatedKey = licenseService.generateLicenseKey(machineId, maxRooms, validDays);
        return Map.of(
                "success", true,
                "machineId", machineId != null ? machineId : licenseService.getMachineId(),
                "maxRooms", maxRooms,
                "validDays", validDays,
                "generatedLicenseKey", generatedKey,
                "message", "✨ สร้างรหัสเปิดใช้งาน License Key สำหรับลูกค้าสำเร็จเรียบร้อยแล้ว!"
        );
    }
}
