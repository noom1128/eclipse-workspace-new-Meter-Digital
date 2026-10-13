package com.noom.meter_digital.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.noom.meter_digital.entity.MeterReplacement;

public interface MeterReplacementRepository extends JpaRepository<MeterReplacement, Long> {
    List<MeterReplacement> findByRoomIdOrderByReplacedDateDesc(Long roomId);
    List<MeterReplacement> findByRoomIdAndReplacedDateBetween(Long roomId, LocalDate start, LocalDate end);
}
