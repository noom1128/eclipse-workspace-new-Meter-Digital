package com.noom.meter_digital.controller;

import com.noom.meter_digital.entity.User;
import com.noom.meter_digital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. ดึงรายชื่อผู้ใช้งานทั้งหมด
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 2. เพิ่มผู้ใช้งานใหม่ (เช่น เพิ่มผู้เช่าห้องใหม่ room102)
    @PostMapping
    public User createUser(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String role = body.getOrDefault("role", "ROLE_TENANT");
        String roomNumber = body.get("roomNumber");

        if (username == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required");
        }

        if (!userRepository.findAllByUsernameOrderByIdAsc(username).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User newUser = new User(
                username,
                passwordEncoder.encode(password),
                role,
                roomNumber
        );

        return userRepository.save(newUser);
    }

    // 3. แก้ไขข้อมูลผู้ใช้ (เปลี่ยน Username, Password, หรือ RoomNumber)
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (body.containsKey("username") && !body.get("username").isBlank()) {
            String newUsername = body.get("username").trim();
            // เช็กว่า Username ใหม่ไปซ้ำกับคนอื่นหรือไม่
            boolean existsForAnotherUser = userRepository.findAllByUsernameOrderByIdAsc(newUsername).stream()
                    .anyMatch(existing -> !existing.getId().equals(id));
            if (existsForAnotherUser) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
            user.setUsername(newUsername);
        }

        if (body.containsKey("password") && !body.get("password").isBlank()) {
            user.setPassword(passwordEncoder.encode(body.get("password").trim()));
        }

        if (body.containsKey("roomNumber")) {
            user.setRoomNumber(body.get("roomNumber"));
        }

        if (body.containsKey("role")) {
            user.setRole(body.get("role"));
        }

        return userRepository.save(user);
    }

    // 4. ลบผู้ใช้งาน
    @DeleteMapping("/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
        return Map.of("message", "User deleted successfully");
    }
}
