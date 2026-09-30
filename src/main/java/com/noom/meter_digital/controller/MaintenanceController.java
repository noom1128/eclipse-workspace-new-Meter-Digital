package com.noom.meter_digital.controller;

import com.noom.meter_digital.entity.GatewayDevice;
import com.noom.meter_digital.repository.GatewayDeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    @Autowired
    private GatewayDeviceRepository deviceRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/devices")
    public List<GatewayDevice> getAllDevices() {
        return deviceRepository.findAll();
    }

    @PostMapping("/devices")
    public GatewayDevice saveOrUpdateDevice(@RequestBody GatewayDevice device) {
        if (device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required (e.g. OPTA-DORM-01)");
        }

        GatewayDevice existing = deviceRepository.findByDeviceId(device.getDeviceId()).orElse(null);
        if (existing != null) {
            if (device.getDeviceName() != null) existing.setDeviceName(device.getDeviceName());
            if (device.getIpAddress() != null) existing.setIpAddress(device.getIpAddress());
            if (device.getMacAddress() != null) existing.setMacAddress(device.getMacAddress());
            if (device.getServerIp() != null) existing.setServerIp(device.getServerIp());
            if (device.getServerPort() != null) existing.setServerPort(device.getServerPort());
            if (device.getWifiSsid() != null) existing.setWifiSsid(device.getWifiSsid());
            if (device.getWifiPassword() != null) existing.setWifiPassword(device.getWifiPassword());
            if (device.getReadIntervalMs() != null) existing.setReadIntervalMs(device.getReadIntervalMs());
            if (device.getActiveMeterModel() != null) existing.setActiveMeterModel(device.getActiveMeterModel());
            if (device.getMeterMappingJson() != null) existing.setMeterMappingJson(device.getMeterMappingJson());
            existing.setStatus("ONLINE");
            existing.setLastHeartbeat(LocalDateTime.now());
            return deviceRepository.save(existing);
        }

        device.setStatus("ONLINE");
        device.setLastHeartbeat(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(@RequestBody Map<String, Object> payload) {
        String deviceId = (String) payload.get("deviceId");
        String ipAddress = (String) payload.get("ipAddress");
        String macAddress = (String) payload.get("macAddress");
        String meterModel = (String) payload.get("activeMeterModel");

        if (deviceId == null || deviceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required");
        }

        GatewayDevice device = deviceRepository.findByDeviceId(deviceId).orElseGet(() -> {
            GatewayDevice newDev = new GatewayDevice();
            newDev.setDeviceId(deviceId);
            newDev.setDeviceName("Gateway " + deviceId);
            return newDev;
        });

        if (ipAddress != null) device.setIpAddress(ipAddress);
        if (macAddress != null) device.setMacAddress(macAddress);
        if (meterModel != null) device.setActiveMeterModel(meterModel);
        device.setStatus("ONLINE");
        device.setLastHeartbeat(LocalDateTime.now());
        deviceRepository.save(device);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("serverTime", LocalDateTime.now().toString());
        response.put("deviceConfig", device);
        return response;
    }

    @GetMapping("/device-config")
    public GatewayDevice getDeviceConfig(@RequestParam String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device " + deviceId + " not found"));
    }

    @PostMapping("/devices/{id}/push-config")
    public ResponseEntity<Map<String, Object>> pushConfigToDevice(@PathVariable Long id) {
        GatewayDevice device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        if (device.getIpAddress() == null || device.getIpAddress().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device IP address is missing");
        }

        String targetUrl = "http://" + device.getIpAddress() + "/api/config";
        Map<String, Object> configPayload = new HashMap<>();
        configPayload.put("deviceId", device.getDeviceId());
        configPayload.put("serverIp", device.getServerIp());
        configPayload.put("serverPort", device.getServerPort());
        configPayload.put("wifiSsid", device.getWifiSsid());
        configPayload.put("wifiPassword", device.getWifiPassword());
        configPayload.put("activeMeterModel", device.getActiveMeterModel());
        configPayload.put("readIntervalMs", device.getReadIntervalMs());
        configPayload.put("meterMappingJson", device.getMeterMappingJson());

        Map<String, Object> result = new HashMap<>();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(targetUrl, configPayload, String.class);
            result.put("success", true);
            result.put("message", "Config pushed successfully to " + device.getIpAddress());
            result.put("deviceResponse", response.getBody());
            device.setStatus("ONLINE");
            device.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to push config to " + device.getIpAddress() + ": " + e.getMessage());
            device.setStatus("WARNING");
            deviceRepository.save(device);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
    }

    @DeleteMapping("/devices/{id}")
    public Map<String, String> deleteDevice(@PathVariable Long id) {
        deviceRepository.deleteById(id);
        Map<String, String> res = new HashMap<>();
        res.put("message", "Device deleted successfully");
        return res;
    }
}
