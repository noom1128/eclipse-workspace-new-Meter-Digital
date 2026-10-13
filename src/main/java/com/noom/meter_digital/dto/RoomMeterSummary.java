package com.noom.meter_digital.dto;

import java.time.LocalDateTime;

public record RoomMeterSummary(
        Long roomId,
        String roomNumber,
        String meterId,
        Double unit,
        Double voltage,
        Double current,
        Double power,
        LocalDateTime timestamp,
        String readerMode,
        String connectionLabel) {

    public RoomMeterSummary(Long roomId, String roomNumber, String meterId, Double unit, Double voltage, Double current, Double power, LocalDateTime timestamp) {
        this(roomId, roomNumber, meterId, unit, voltage, current, power, timestamp, "MODE_ESP32_WIFI", "ESP32 (Wi-Fi)");
    }
}
