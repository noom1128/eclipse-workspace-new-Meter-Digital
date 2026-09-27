package com.noom.meter_digital.dto;

import java.util.ArrayList;
import java.util.List;

public class InvoiceRequest {
    private Long roomId;
    private String billingMonth;
    private Double openingReading;
    private Double closingReading;
    private Double waterUnits;
    private Double rentAmount;
    private Double waterRate;
    private String dueDate;
    private List<AdditionalChargeRequest> additionalCharges = new ArrayList<>();

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }
    public Double getOpeningReading() { return openingReading; }
    public void setOpeningReading(Double openingReading) { this.openingReading = openingReading; }
    public Double getClosingReading() { return closingReading; }
    public void setClosingReading(Double closingReading) { this.closingReading = closingReading; }
    public Double getWaterUnits() { return waterUnits; }
    public void setWaterUnits(Double waterUnits) { this.waterUnits = waterUnits; }
    public Double getRentAmount() { return rentAmount; }
    public void setRentAmount(Double rentAmount) { this.rentAmount = rentAmount; }
    public Double getWaterRate() { return waterRate; }
    public void setWaterRate(Double waterRate) { this.waterRate = waterRate; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public List<AdditionalChargeRequest> getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(List<AdditionalChargeRequest> additionalCharges) {
        this.additionalCharges = additionalCharges == null ? new ArrayList<>() : additionalCharges;
    }
}
