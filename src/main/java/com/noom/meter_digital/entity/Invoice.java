package com.noom.meter_digital.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;
    private String billingMonth;
    private String status;
    private LocalDate issuedDate;
    private LocalDate dueDate;
    private LocalDateTime paidAt;

    @ManyToOne
    private Room room;

    private Double openingReading;
    private Double closingReading;
    private Double electricityUnits;
    private Double electricityRate;
    private Double electricityAmount;
    private Double waterUnits;
    private Double waterRate;
    private Double waterAmount;
    private Double rentAmount;
    private Double subtotal;
    private Double vatAmount;
    private Double totalAmount;

    @OneToMany(mappedBy = "invoice", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceCharge> additionalCharges = new ArrayList<>();

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getBillingMonth() { return billingMonth; }
    public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getIssuedDate() { return issuedDate; }
    public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Double getOpeningReading() { return openingReading; }
    public void setOpeningReading(Double openingReading) { this.openingReading = openingReading; }
    public Double getClosingReading() { return closingReading; }
    public void setClosingReading(Double closingReading) { this.closingReading = closingReading; }
    public Double getElectricityUnits() { return electricityUnits; }
    public void setElectricityUnits(Double electricityUnits) { this.electricityUnits = electricityUnits; }
    public Double getElectricityRate() { return electricityRate; }
    public void setElectricityRate(Double electricityRate) { this.electricityRate = electricityRate; }
    public Double getElectricityAmount() { return electricityAmount; }
    public void setElectricityAmount(Double electricityAmount) { this.electricityAmount = electricityAmount; }
    public Double getWaterUnits() { return waterUnits; }
    public void setWaterUnits(Double waterUnits) { this.waterUnits = waterUnits; }
    public Double getWaterRate() { return waterRate; }
    public void setWaterRate(Double waterRate) { this.waterRate = waterRate; }
    public Double getWaterAmount() { return waterAmount; }
    public void setWaterAmount(Double waterAmount) { this.waterAmount = waterAmount; }
    public Double getRentAmount() { return rentAmount; }
    public void setRentAmount(Double rentAmount) { this.rentAmount = rentAmount; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public Double getVatAmount() { return vatAmount; }
    public void setVatAmount(Double vatAmount) { this.vatAmount = vatAmount; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public List<InvoiceCharge> getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(List<InvoiceCharge> additionalCharges) { this.additionalCharges = additionalCharges; }
}
