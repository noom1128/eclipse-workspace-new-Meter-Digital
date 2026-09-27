package com.noom.meter_digital.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.repository.MeterReadingRepository;

import com.noom.meter_digital.dto.RoomMeterSummary;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.RoomRepository;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/api/meter")
public class MeterReadingController {

    @Autowired
    MeterReadingRepository repository;

    @Autowired
    RoomRepository roomRepository;

    @PostMapping
    public MeterReading save(@org.springframework.web.bind.annotation.RequestBody MeterReading meter) {
        String requestedRoom = meter.getRoomNumber();
        if ((requestedRoom == null || requestedRoom.isBlank()) && meter.getRoom() != null) {
            requestedRoom = meter.getRoom().getRoomNumber();
        }
        if (requestedRoom == null || requestedRoom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomNumber is required (example: 101)");
        }
        String roomNumber = requestedRoom.trim();
        Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(roomNumber).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room " + roomNumber + " not found"));

        meter.setRoom(room);
        meter.setRoomNumber(roomNumber);
        meter.setTimestamp(LocalDateTime.now());

        if (meter.getMeterId() == null || meter.getMeterId().isBlank()) {
            meter.setMeterId("METER-" + roomNumber);
        }
        if (meter.getUnit() == null && meter.getEnergy() != null) {
            meter.setUnit(meter.getEnergy());
        }

        return repository.save(meter);
    }

    @GetMapping
    public List<MeterReading> getAll() {
        return repository.findTop50ByOrderByTimestampDesc();
    }
    
    @GetMapping("/latest")
    public MeterReading latest(@RequestParam(required = false) String roomNumber) {
        if (roomNumber != null && !roomNumber.isBlank()) {
            Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(roomNumber.trim()).stream()
                    .findFirst()
                    .orElse(null);
            if (room != null) {
                MeterReading roomReading = repository.findTopByRoomIdOrderByTimestampDesc(room.getId());
                if (roomReading != null) {
                    return roomReading;
                }
            }
        }
        return repository.findTopByOrderByTimestampDesc();
    }

    @GetMapping("/latest-by-room")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoomMeterSummary> latestByRoom() {
        return roomRepository.findAll().stream()
                .map(room -> toSummary(room, repository.findTopByRoomIdOrderByTimestampDesc(room.getId())))
                .collect(Collectors.toList());
    }

    @GetMapping("/network-mode")
    public java.util.Map<String, String> getNetworkMode(@RequestParam(defaultValue = "METER001") String meterId) {
        // คืนค่า mode เช่น "WIFI" หรือ "LAN"
        return java.util.Collections.singletonMap("mode", "WIFI");
    }

    private RoomMeterSummary toSummary(Room room, MeterReading reading) {
        if (reading == null) {
            return new RoomMeterSummary(room.getId(), room.getRoomNumber(), "METER-" + room.getRoomNumber(),
                    null, null, null, null, null);
        }
        return new RoomMeterSummary(room.getId(), room.getRoomNumber(), reading.getMeterId(), reading.getUnit(),
                reading.getVoltage(), reading.getCurrent(), reading.getPower(), reading.getTimestamp());
    }
}
