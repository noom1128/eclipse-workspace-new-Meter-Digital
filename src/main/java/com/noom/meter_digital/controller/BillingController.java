package com.noom.meter_digital.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.noom.meter_digital.dto.InvoiceRequest;
import com.noom.meter_digital.entity.Invoice;
import com.noom.meter_digital.service.BillingService;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final BillingService billing;
    public BillingController(BillingService billing) { this.billing = billing; }

    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam Long roomId, @RequestParam String billingMonth,
            @RequestParam(required = false) Double openingReading, @RequestParam(required = false) Double closingReading,
            @RequestParam(required = false, defaultValue = "0") Double waterUnits) {
        return billing.preview(roomId, billingMonth, openingReading, closingReading, waterUnits);
    }

    @PostMapping("/preview")
    public Map<String, Object> previewWithAdditionalCharges(@RequestBody InvoiceRequest request) {
        return billing.preview(request.getRoomId(), request.getBillingMonth(), request.getOpeningReading(),
                request.getClosingReading(), request.getWaterUnits(), request.getRentAmount(), request.getWaterRate(),
                request.getAdditionalCharges());
    }

    @GetMapping("/invoices")
    public List<Invoice> list() { return billing.list(); }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice create(@RequestBody InvoiceRequest request) { return billing.create(request); }

    @PostMapping("/invoices/{id}/mark-paid")
    public Invoice markPaid(@PathVariable Long id) { return billing.markPaid(id); }
}
