package com.noom.meter_digital.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.RoomRepository;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @PostMapping
    public Room addRoom(@RequestBody Room room) {
        return roomRepository.save(room);
    }

    @GetMapping
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }

    @GetMapping("/{id}")
    public Room getRoom(@PathVariable Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Room not found"));
    }

    @PutMapping("/{id}")
    public Room updateRoom(@PathVariable Long id, @RequestBody Room input) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Room not found"));
        room.setRoomNumber(input.getRoomNumber());
        room.setTenantName(input.getTenantName());
        room.setMonthlyRent(input.getMonthlyRent());
        room.setRatePerUnit(input.getRatePerUnit());
        room.setWaterRatePerUnit(input.getWaterRatePerUnit());
        return roomRepository.save(room);
    }

    @DeleteMapping("/{id}")
    public java.util.Map<String, Object> deleteRoom(@PathVariable Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Room not found"));
        roomRepository.delete(room);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("deleted", true);
        response.put("id", id);
        response.put("roomNumber", room.getRoomNumber());
        return response;
    }
}
