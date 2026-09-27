package com.noom.meter_digital.dto;

public class AdditionalChargeRequest {
    private String name;
    private Double amount;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
