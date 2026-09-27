package com.noom.meter_digital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.noom.meter_digital.entity.MeterReading;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

@SuppressWarnings("unused")
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
	@Query("SELECT m FROM MeterReading m WHERE m.room.id = :roomId AND m.meterMonth = :meterMonth")
	Optional<MeterReading> findByRoomIdAndMeterMonth(Long roomId, String meterMonth);
	
	MeterReading findTopByOrderByTimestampDesc();

    MeterReading findTopByRoomIdOrderByTimestampDesc(Long roomId);

    MeterReading findTopByRoomIdAndTimestampBeforeOrderByTimestampDesc(Long roomId, java.time.LocalDateTime timestamp);

    MeterReading findTopByRoomIdAndTimestampLessThanEqualOrderByTimestampDesc(Long roomId, java.time.LocalDateTime timestamp);

    List<MeterReading> findTop50ByOrderByTimestampDesc();
}
