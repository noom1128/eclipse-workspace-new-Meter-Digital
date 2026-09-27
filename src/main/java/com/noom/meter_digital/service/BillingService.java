package com.noom.meter_digital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.noom.meter_digital.dto.InvoiceRequest;
import com.noom.meter_digital.dto.AdditionalChargeRequest;
import com.noom.meter_digital.entity.Invoice;
import com.noom.meter_digital.entity.InvoiceCharge;
import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.InvoiceRepository;
import com.noom.meter_digital.repository.MeterReadingRepository;
import com.noom.meter_digital.repository.RoomRepository;

@Service
public class BillingService {
    private final MeterReadingRepository meterReadings;
    private final RoomRepository rooms;
    private final InvoiceRepository invoices;

    public BillingService(MeterReadingRepository meterReadings, RoomRepository rooms, InvoiceRepository invoices) {
        this.meterReadings = meterReadings;
        this.rooms = rooms;
        this.invoices = invoices;
    }

    public Map<String, Object> preview(Long roomId, String billingMonth, Double openingOverride,
            Double closingOverride, Double waterUnits) {
        return preview(roomId, billingMonth, openingOverride, closingOverride, waterUnits, null, null, List.of());
    }

    public Map<String, Object> preview(Long roomId, String billingMonth, Double openingOverride,
            Double closingOverride, Double waterUnits, Double rentOverride, Double waterRateOverride,
            List<AdditionalChargeRequest> requestedCharges) {
        Room room = getRoom(roomId);
        YearMonth month = parseMonth(billingMonth);
        Double opening = openingOverride != null ? openingOverride : readingBefore(roomId, month.atDay(1).atStartOfDay());
        Double closing = closingOverride != null ? closingOverride
                : readingAtOrBefore(roomId, month.atEndOfMonth().atTime(23, 59, 59));
        if (opening == null) opening = 0.0;
        if (closing == null) closing = opening;
        if (closing < opening) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closing meter reading must not be lower than opening reading");
        }

        double electricityUnits = round(closing - opening);
        double electricityRate = valueOrZero(room.getRatePerUnit());
        double water = valueOrZero(waterUnits);
        double waterRate = waterRateOverride == null ? valueOrZero(room.getWaterRatePerUnit()) : waterRateOverride;
        double rent = rentOverride == null ? valueOrZero(room.getMonthlyRent()) : rentOverride;
        if (waterRate < 0 || rent < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rent and water rate must not be negative");
        }
        double electricityAmount = round(electricityUnits * electricityRate);
        double waterAmount = round(water * waterRate);
        List<Map<String, Object>> additionalCharges = normalizedCharges(requestedCharges);
        double additionalAmount = additionalCharges.stream()
                .mapToDouble(charge -> ((Number) charge.get("amount")).doubleValue()).sum();
        double subtotal = round(rent + electricityAmount + waterAmount + additionalAmount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", room.getId());
        result.put("roomNumber", room.getRoomNumber());
        result.put("tenantName", room.getTenantName());
        result.put("billingMonth", month.toString());
        result.put("openingReading", opening);
        result.put("closingReading", closing);
        result.put("electricityUnits", electricityUnits);
        result.put("electricityRate", electricityRate);
        result.put("electricityAmount", electricityAmount);
        result.put("waterUnits", water);
        result.put("waterRate", waterRate);
        result.put("waterAmount", waterAmount);
        result.put("rentAmount", rent);
        result.put("additionalCharges", additionalCharges);
        result.put("additionalAmount", round(additionalAmount));
        result.put("subtotal", subtotal);
        result.put("vatAmount", 0.0);
        result.put("totalAmount", subtotal);
        return result;
    }

    public Invoice create(InvoiceRequest request) {
        if (request.getRoomId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId is required");
        Map<String, Object> preview = preview(request.getRoomId(), request.getBillingMonth(), request.getOpeningReading(),
                request.getClosingReading(), request.getWaterUnits(), request.getRentAmount(), request.getWaterRate(),
                request.getAdditionalCharges());
        if (invoices.findByRoomIdAndBillingMonth(request.getRoomId(), request.getBillingMonth()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invoice already exists for this room and billing month");
        }
        Room room = getRoom(request.getRoomId());
        Invoice invoice = new Invoice();
        invoice.setRoom(room);
        invoice.setBillingMonth((String) preview.get("billingMonth"));
        invoice.setInvoiceNumber("INV-" + request.getBillingMonth().replace("-", "") + "-" + room.getRoomNumber());
        invoice.setStatus("PENDING");
        invoice.setIssuedDate(LocalDate.now());
        invoice.setDueDate(request.getDueDate() == null || request.getDueDate().isBlank()
                ? LocalDate.now().plusDays(10) : LocalDate.parse(request.getDueDate()));
        invoice.setOpeningReading(number(preview, "openingReading"));
        invoice.setClosingReading(number(preview, "closingReading"));
        invoice.setElectricityUnits(number(preview, "electricityUnits"));
        invoice.setElectricityRate(number(preview, "electricityRate"));
        invoice.setElectricityAmount(number(preview, "electricityAmount"));
        invoice.setWaterUnits(number(preview, "waterUnits"));
        invoice.setWaterRate(number(preview, "waterRate"));
        invoice.setWaterAmount(number(preview, "waterAmount"));
        invoice.setRentAmount(number(preview, "rentAmount"));
        invoice.setSubtotal(number(preview, "subtotal"));
        invoice.setVatAmount(number(preview, "vatAmount"));
        invoice.setTotalAmount(number(preview, "totalAmount"));
        List<InvoiceCharge> charges = new ArrayList<>();
        for (Map<String, Object> charge : (List<Map<String, Object>>) preview.get("additionalCharges")) {
            InvoiceCharge item = new InvoiceCharge();
            item.setInvoice(invoice);
            item.setName((String) charge.get("name"));
            item.setAmount(((Number) charge.get("amount")).doubleValue());
            charges.add(item);
        }
        invoice.setAdditionalCharges(charges);
        return invoices.save(invoice);
    }

    public List<Invoice> list() { return invoices.findAllByOrderByIssuedDateDescIdDesc(); }

    public Invoice markPaid(Long id) {
        Invoice invoice = invoices.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        invoice.setStatus("PAID");
        invoice.setPaidAt(LocalDateTime.now());
        return invoices.save(invoice);
    }

    private Room getRoom(Long roomId) {
        return rooms.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private YearMonth parseMonth(String value) {
        try { return YearMonth.parse(value); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "billingMonth must use YYYY-MM"); }
    }

    private Double readingBefore(Long roomId, LocalDateTime time) {
        MeterReading reading = meterReadings.findTopByRoomIdAndTimestampBeforeOrderByTimestampDesc(roomId, time);
        return reading == null ? null : reading.getUnit();
    }

    private Double readingAtOrBefore(Long roomId, LocalDateTime time) {
        MeterReading reading = meterReadings.findTopByRoomIdAndTimestampLessThanEqualOrderByTimestampDesc(roomId, time);
        return reading == null ? null : reading.getUnit();
    }

    private double valueOrZero(Double value) { return value == null ? 0.0 : value; }
    private List<Map<String, Object>> normalizedCharges(List<AdditionalChargeRequest> requestedCharges) {
        List<Map<String, Object>> charges = new ArrayList<>();
        if (requestedCharges == null) return charges;
        for (AdditionalChargeRequest item : requestedCharges) {
            String name = item.getName() == null ? "" : item.getName().trim();
            double amount = valueOrZero(item.getAmount());
            if (name.isEmpty() && amount == 0) continue;
            if (name.isEmpty() || amount < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Additional charges must have a name and a non-negative amount");
            }
            Map<String, Object> charge = new LinkedHashMap<>();
            charge.put("name", name);
            charge.put("amount", round(amount));
            charges.add(charge);
        }
        return charges;
    }
    private double number(Map<String, Object> values, String key) { return ((Number) values.get(key)).doubleValue(); }
    private double round(double value) { return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue(); }
}
