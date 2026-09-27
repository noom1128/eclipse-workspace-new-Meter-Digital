package com.noom.meter_digital.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.PostMapping;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

import org.springframework.data.jpa.repository.JpaRepository;
import com.noom.meter_digital.entity.MeterReading;
@Entity
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double unit; // หน่วยไฟที่อ่านได้

    //private String meterMonth; // เช่น 2026-03

    private LocalDate recordDate;

    private String meterId;

    private Double metervalue;

    private LocalDateTime timestamp;
    
    private Double voltage;
    private Double energy;
    private Double current;
    private Double power;
    
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Transient
    private String roomNumber;
    
    @Column(name="meter_month")
    private String meterMonth;

    // getter setter
    public Long getId() { return id; }

    public Double getUnit() { return unit; }
    public void setUnit(Double unit) { this.unit = unit; }

    public String getMonth() { return meterMonth; }
    public void setMonth(String meterMonth) { this.meterMonth = meterMonth; }

    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    
	public String getMeterId() {
		return meterId;
	}

	public void setMeterId(String meterId) {
		this.meterId = meterId;
	}

	public Double getValue() {
		return metervalue;
	}

	public void setValue(Double value) {
		this.metervalue = value;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public Double getMetervalue() {
		return metervalue;
	}

	public void setMetervalue(Double metervalue) {
		this.metervalue = metervalue;
	}

	public Double getVoltage() {
		return voltage;
	}

	public void setVoltage(Double voltage) {
		this.voltage = voltage;
	}

	public Double getEnergy() {
		return energy;
	}

	public void setEnergy(Double energy) {
		this.energy = energy;
	}

	public Double getCurrent() {
		return current;
	}

	public void setCurrent(Double current) {
		this.current = current;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double power) {
		this.power = power;
	}
}
