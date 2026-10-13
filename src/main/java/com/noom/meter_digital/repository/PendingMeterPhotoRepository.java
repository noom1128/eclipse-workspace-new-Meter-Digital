package com.noom.meter_digital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.noom.meter_digital.entity.PendingMeterPhoto;

@Repository
public interface PendingMeterPhotoRepository extends JpaRepository<PendingMeterPhoto, Long> {
    Optional<PendingMeterPhoto> findByPhotoId(String photoId);
    List<PendingMeterPhoto> findByStatusOrderByCreatedAtDesc(String status);
    List<PendingMeterPhoto> findByRoomNumberOrderByCreatedAtDesc(String roomNumber);
}
