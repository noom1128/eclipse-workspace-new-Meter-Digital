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
        LocalDateTime timestamp) {
}
