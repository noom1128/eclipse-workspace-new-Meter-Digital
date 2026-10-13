package com.noom.meter_digital.dto;

public class MeterSingleReading {
    private String roomNumber;
    private String meterId;
    private Double voltage;
    private Double current;
    private Double power;
    private Double energyKwh;

    public MeterSingleReading() {}

    public MeterSingleReading(String roomNumber, String meterId, Double voltage, Double current, Double energyKwh) {
        this.roomNumber = roomNumber;
        this.meterId = meterId;
        this.voltage = voltage;
        this.current = current;
        this.energyKwh = energyKwh;
        if (voltage != null && current != null) {
            this.power = (voltage * current) / 1000.0;
        }
    }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }

    public Double getVoltage() { return voltage; }
    public void setVoltage(Double voltage) { this.voltage = voltage; }

    public Double getCurrent() { return current; }
    public void setCurrent(Double current) { this.current = current; }

    public Double getPower() { return power; }
    public void setPower(Double power) { this.power = power; }

    public Double getEnergyKwh() { return energyKwh; }
    public void setEnergyKwh(Double energyKwh) { this.energyKwh = energyKwh; }
}
