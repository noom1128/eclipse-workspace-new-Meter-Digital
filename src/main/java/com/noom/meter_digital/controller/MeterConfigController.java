package com.noom.meter_digital.controller;

import com.noom.meter_digital.entity.GatewayDevice;
import com.noom.meter_digital.repository.GatewayDeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/meter-configs", "/api/meter-configs"})
public class MeterConfigController {

    @Autowired
    private GatewayDeviceRepository deviceRepository;

    @Autowired
    private com.noom.meter_digital.service.ModbusSimulatorService simulatorService;

    @GetMapping
    public List<GatewayDevice> getAllConfigs() {
        return deviceRepository.findAll();
    }

    @GetMapping("/simulator/status")
    public Map<String, Object> getSimulatorStatus() {
        return Map.of(
                "running", simulatorService.isRunning(),
                "port", simulatorService.getPort()
        );
    }

    @PostMapping("/simulator/toggle")
    public Map<String, Object> toggleSimulator(@RequestBody(required = false) Map<String, Object> body) {
        if (simulatorService.isRunning()) {
            simulatorService.stopSimulator();
        } else {
            int port = 10502;
            if (body != null && body.containsKey("port")) {
                port = Integer.parseInt(body.get("port").toString());
            }
            simulatorService.startSimulator(port);
        }
        return Map.of(
                "running", simulatorService.isRunning(),
                "port", simulatorService.getPort(),
                "message", simulatorService.isRunning() ? "🤖 Modbus Simulator Started!" : "🛑 Modbus Simulator Stopped"
        );
    }


    @GetMapping("/{id}")
    public GatewayDevice getConfigById(@PathVariable Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter configuration not found"));
    }

    @PostMapping
    public GatewayDevice saveOrUpdateConfig(@RequestBody GatewayDevice deviceConfig) {
        if (deviceConfig.getDeviceId() == null || deviceConfig.getDeviceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceId is required");
        }

        GatewayDevice existing = deviceRepository.findByDeviceId(deviceConfig.getDeviceId()).orElse(null);
        if (existing != null) {
            if (deviceConfig.getDeviceName() != null) existing.setDeviceName(deviceConfig.getDeviceName());
            if (deviceConfig.getIpAddress() != null) existing.setIpAddress(deviceConfig.getIpAddress());
            if (deviceConfig.getReaderMode() != null) existing.setReaderMode(deviceConfig.getReaderMode());
            if (deviceConfig.getPort() != null) existing.setPort(deviceConfig.getPort());
            if (deviceConfig.getSlaveId() != null) existing.setSlaveId(deviceConfig.getSlaveId());
            if (deviceConfig.getActiveMeterModel() != null) existing.setActiveMeterModel(deviceConfig.getActiveMeterModel());
            if (deviceConfig.getPollingIntervalSeconds() != null) existing.setPollingIntervalSeconds(deviceConfig.getPollingIntervalSeconds());
            if (deviceConfig.getTargetRoomNumber() != null) existing.setTargetRoomNumber(deviceConfig.getTargetRoomNumber());
            if (deviceConfig.getProtocolMode() != null) existing.setProtocolMode(deviceConfig.getProtocolMode());
            if (deviceConfig.getWifiSsid() != null) existing.setWifiSsid(deviceConfig.getWifiSsid());
            if (deviceConfig.getWifiPassword() != null) existing.setWifiPassword(deviceConfig.getWifiPassword());
            if (deviceConfig.getServerIp() != null) existing.setServerIp(deviceConfig.getServerIp());
            if (deviceConfig.getServerPort() != null) existing.setServerPort(deviceConfig.getServerPort());

            existing.setLastHeartbeat(LocalDateTime.now());
            return deviceRepository.save(existing);
        }

        deviceConfig.setStatus("ONLINE");
        deviceConfig.setLastHeartbeat(LocalDateTime.now());
        return deviceRepository.save(deviceConfig);
    }

    @Autowired
    private com.noom.meter_digital.service.ModbusMasterService modbusMasterService;

    @PostMapping("/test-connection")
    public Map<String, Object> testConnectionDirect(@RequestBody GatewayDevice deviceConfig) {
        String ip = deviceConfig.getIpAddress();
        int port = deviceConfig.getPort() != null ? deviceConfig.getPort() : 502;
        int slaveId = deviceConfig.getSlaveId() != null ? deviceConfig.getSlaveId() : 1;
        String model = deviceConfig.getActiveMeterModel() != null ? deviceConfig.getActiveMeterModel() : "MODEL_A";
        String protocolMode = deviceConfig.getProtocolMode() != null ? deviceConfig.getProtocolMode() : "AUTO";

        return modbusMasterService.testConnection(ip, port, slaveId, model, protocolMode);
    }

    @PostMapping("/{id}/test-connection")
    public Map<String, Object> testConnectionById(@PathVariable Long id) {
        GatewayDevice device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter configuration not found"));

        return modbusMasterService.testConnection(
                device.getIpAddress(),
                device.getPort() != null ? device.getPort() : 502,
                device.getSlaveId() != null ? device.getSlaveId() : 1,
                device.getActiveMeterModel() != null ? device.getActiveMeterModel() : "MODEL_A",
                device.getProtocolMode() != null ? device.getProtocolMode() : "AUTO"
        );
    }


    @DeleteMapping("/{id}")
    public Map<String, String> deleteConfig(@PathVariable Long id) {
        deviceRepository.deleteById(id);
        return Map.of("message", "Meter configuration deleted successfully");
    }
}

