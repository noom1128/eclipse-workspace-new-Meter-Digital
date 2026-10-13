package com.noom.meter_digital.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gateway_devices")
public class GatewayDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String deviceId;

    private String deviceName;
    private String ipAddress;
    private String macAddress;
    private String status; // ONLINE, OFFLINE, WARNING
    private String serverIp;
    private Integer serverPort;
    private String wifiSsid;
    private String wifiPassword;
    private Long readIntervalMs;
    private String activeMeterModel; // CIRCUTOR_CVMC5, SCHNEIDER_PM2200

    private String readerMode = "MODE_ESP32_WIFI"; // MODE_ESP32_WIFI, MODE_ESP32_LAN, MODE_MODBUS_WIFI, MODE_MODBUS_LAN, MODE_MOXA_SERIAL
    private Integer port = 502;
    private Integer slaveId = 1;
    private Integer pollingIntervalSeconds = 60;
    private String targetRoomNumber; // e.g. "101"
    private String protocolMode = "AUTO"; // AUTO, MODBUS_TCP, MODBUS_RTU_OVER_TCP

    @Column(columnDefinition = "TEXT")
    private String meterMappingJson;

    private LocalDateTime lastHeartbeat;

    public GatewayDevice() {}

    public GatewayDevice(String deviceId, String deviceName, String ipAddress, String status) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.status = status;
        this.lastHeartbeat = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public Integer getServerPort() { return serverPort; }
    public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }

    public String getWifiSsid() { return wifiSsid; }
    public void setWifiSsid(String wifiSsid) { this.wifiSsid = wifiSsid; }

    public String getWifiPassword() { return wifiPassword; }
    public void setWifiPassword(String wifiPassword) { this.wifiPassword = wifiPassword; }

    public Long getReadIntervalMs() { return readIntervalMs; }
    public void setReadIntervalMs(Long readIntervalMs) { this.readIntervalMs = readIntervalMs; }

    public String getActiveMeterModel() { return activeMeterModel; }
    public void setActiveMeterModel(String activeMeterModel) { this.activeMeterModel = activeMeterModel; }

    public String getReaderMode() { return readerMode != null ? readerMode : "MODE_ESP32_WIFI"; }
    public void setReaderMode(String readerMode) { this.readerMode = readerMode; }

    public Integer getPort() { return port != null ? port : 502; }
    public void setPort(Integer port) { this.port = port; }

    public Integer getSlaveId() { return slaveId != null ? slaveId : 1; }
    public void setSlaveId(Integer slaveId) { this.slaveId = slaveId; }

    public Integer getPollingIntervalSeconds() { return pollingIntervalSeconds != null ? pollingIntervalSeconds : 60; }
    public void setPollingIntervalSeconds(Integer pollingIntervalSeconds) { this.pollingIntervalSeconds = pollingIntervalSeconds; }

    public String getTargetRoomNumber() { return targetRoomNumber; }
    public void setTargetRoomNumber(String targetRoomNumber) { this.targetRoomNumber = targetRoomNumber; }

    public String getProtocolMode() { return protocolMode != null ? protocolMode : "AUTO"; }
    public void setProtocolMode(String protocolMode) { this.protocolMode = protocolMode; }

    public String getMeterMappingJson() { return meterMappingJson; }
    public void setMeterMappingJson(String meterMappingJson) { this.meterMappingJson = meterMappingJson; }

    public LocalDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(LocalDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
}

