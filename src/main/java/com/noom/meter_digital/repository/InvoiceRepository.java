package com.noom.meter_digital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.noom.meter_digital.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByIssuedDateDescIdDesc();
    Optional<Invoice> findByRoomIdAndBillingMonth(Long roomId, String billingMonth);
}
