package com.noom.meter_digital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.noom.meter_digital.entity.Room;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByRoomNumberOrderByIdAsc(String roomNumber);
}
