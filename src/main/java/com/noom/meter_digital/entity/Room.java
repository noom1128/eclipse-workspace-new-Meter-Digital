package com.noom.meter_digital.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomNumber;

    private String tenantName;

    private Double monthlyRent;

    private Double waterRatePerUnit;

    private Double ratePerUnit;

    private String elecRateType; // PER_UNIT, MINIMUM, FLAT_RATE
    private Double elecMinCharge;

    private String waterRateType; // PER_UNIT, MINIMUM, FLAT_RATE
    private Double waterMinCharge;

    private Integer meterDigits = 4;

    // ===== Getter & Setter =====

    public Long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Double getRatePerUnit() {
        return ratePerUnit;
    }

    public void setRatePerUnit(Double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }

    public Double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(Double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public Double getWaterRatePerUnit() {
        return waterRatePerUnit;
    }

    public void setWaterRatePerUnit(Double waterRatePerUnit) {
        this.waterRatePerUnit = waterRatePerUnit;
    }

    public String getElecRateType() {
        return elecRateType != null ? elecRateType : "PER_UNIT";
    }

    public void setElecRateType(String elecRateType) {
        this.elecRateType = elecRateType;
    }

    public Double getElecMinCharge() {
        return elecMinCharge != null ? elecMinCharge : 0.0;
    }

    public void setElecMinCharge(Double elecMinCharge) {
        this.elecMinCharge = elecMinCharge;
    }

    public String getWaterRateType() {
        return waterRateType != null ? waterRateType : "PER_UNIT";
    }

    public void setWaterRateType(String waterRateType) {
        this.waterRateType = waterRateType;
    }

    public Double getWaterMinCharge() {
        return waterMinCharge != null ? waterMinCharge : 0.0;
    }

    public void setWaterMinCharge(Double waterMinCharge) {
        this.waterMinCharge = waterMinCharge;
    }

    public Integer getMeterDigits() {
        return meterDigits != null ? meterDigits : 4;
    }

    public void setMeterDigits(Integer meterDigits) {
        this.meterDigits = meterDigits;
    }
}
