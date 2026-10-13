package com.noom.meter_digital.controller;

import com.noom.meter_digital.model.modbus.ModbusProfile;
import com.noom.meter_digital.service.ModbusProfileManagerService;
import com.noom.meter_digital.service.ModbusScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/modbus-debug", "/api/modbus-debug"})
public class RemoteMeterDebugController {

    @Autowired
    private ModbusScannerService scannerService;

    @Autowired
    private ModbusProfileManagerService profileManagerService;

    @PostMapping("/read-registers")
    public Map<String, Object> readRawRegisters(@RequestBody Map<String, Object> request) {
        String ipAddress = (String) request.getOrDefault("ipAddress", "127.0.0.1");
        int port = parseInteger(request.get("port"), 502);
        int slaveId = parseInteger(request.get("slaveId"), 1);
        int functionCode = parseInteger(request.get("functionCode"), 3);
        int startRegister = parseInteger(request.get("startRegister"), 0);
        int registerCount = parseInteger(request.get("registerCount"), 8);
        String protocolMode = (String) request.getOrDefault("protocolMode", "AUTO");

        return scannerService.readRawRegisters(ipAddress, port, slaveId, functionCode, startRegister, registerCount, protocolMode);
    }

    @PostMapping("/scan-slaves")
    public Map<String, Object> scanSlaveIds(@RequestBody Map<String, Object> request) {
        String ipAddress = (String) request.getOrDefault("ipAddress", "127.0.0.1");
        int port = parseInteger(request.get("port"), 502);
        int startSlaveId = parseInteger(request.get("startSlaveId"), 1);
        int endSlaveId = parseInteger(request.get("endSlaveId"), 32);
        String protocolMode = (String) request.getOrDefault("protocolMode", "AUTO");

        return scannerService.scanSlaveIds(ipAddress, port, startSlaveId, endSlaveId, protocolMode);
    }

    @PostMapping("/scan-registers")
    public Map<String, Object> scanRegisterRange(@RequestBody Map<String, Object> request) {
        String ipAddress = (String) request.getOrDefault("ipAddress", "127.0.0.1");
        int port = parseInteger(request.get("port"), 502);
        int slaveId = parseInteger(request.get("slaveId"), 1);
        int startRegister = parseInteger(request.get("startRegister"), 0);
        int endRegister = parseInteger(request.get("endRegister"), 100);
        int functionCode = parseInteger(request.get("functionCode"), 3);
        String protocolMode = (String) request.getOrDefault("protocolMode", "AUTO");

        return scannerService.scanRegisterRange(ipAddress, port, slaveId, startRegister, endRegister, functionCode, protocolMode);
    }

    @PostMapping("/save-profile")
    public Map<String, Object> saveDiscoveredProfile(@RequestBody ModbusProfile profile) {
        try {
            ModbusProfile saved = profileManagerService.saveProfile(profile);
            return Map.of(
                    "success", true,
                    "message", "Dynamic Modbus profile " + saved.getModelKey() + " saved successfully!",
                    "profile", saved
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "Failed to save profile: " + e.getMessage()
            );
        }
    }

    private int parseInteger(Object val, int defaultValue) {
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
