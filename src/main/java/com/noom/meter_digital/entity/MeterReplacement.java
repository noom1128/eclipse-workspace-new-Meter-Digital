package com.noom.meter_digital.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "meter_replacements")
public class MeterReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;
    private String roomNumber;
    private LocalDate replacedDate;
    private Double oldMeterFinalReading;
    private Double newMeterInitialReading;
    private String oldMeterId;
    private String newMeterId;
    private String reason;
    private LocalDateTime createdAt = LocalDateTime.now();

    public MeterReplacement() {}

    public MeterReplacement(Long roomId, String roomNumber, LocalDate replacedDate,
                            Double oldMeterFinalReading, Double newMeterInitialReading,
                            String oldMeterId, String newMeterId, String reason) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.replacedDate = replacedDate;
        this.oldMeterFinalReading = oldMeterFinalReading;
        this.newMeterInitialReading = newMeterInitialReading;
        this.oldMeterId = oldMeterId;
        this.newMeterId = newMeterId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public LocalDate getReplacedDate() { return replacedDate; }
    public void setReplacedDate(LocalDate replacedDate) { this.replacedDate = replacedDate; }
    public Double getOldMeterFinalReading() { return oldMeterFinalReading; }
    public void setOldMeterFinalReading(Double oldMeterFinalReading) { this.oldMeterFinalReading = oldMeterFinalReading; }
    public Double getNewMeterInitialReading() { return newMeterInitialReading; }
    public void setNewMeterInitialReading(Double newMeterInitialReading) { this.newMeterInitialReading = newMeterInitialReading; }
    public String getOldMeterId() { return oldMeterId; }
    public void setOldMeterId(String oldMeterId) { this.oldMeterId = oldMeterId; }
    public String getNewMeterId() { return newMeterId; }
    public void setNewMeterId(String newMeterId) { this.newMeterId = newMeterId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
