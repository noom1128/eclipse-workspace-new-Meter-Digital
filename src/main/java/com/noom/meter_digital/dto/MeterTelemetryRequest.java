package com.noom.meter_digital.dto;

import java.util.List;

public class MeterTelemetryRequest {
    private String deviceId;
    private String connectionType; // WIFI, LAN
    private List<MeterSingleReading> readings;
    private String timestamp;

    public MeterTelemetryRequest() {}

    public MeterTelemetryRequest(String deviceId, String connectionType, List<MeterSingleReading> readings, String timestamp) {
        this.deviceId = deviceId;
        this.connectionType = connectionType;
        this.readings = readings;
        this.timestamp = timestamp;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }

    public List<MeterSingleReading> getReadings() { return readings; }
    public void setReadings(List<MeterSingleReading> readings) { this.readings = readings; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
