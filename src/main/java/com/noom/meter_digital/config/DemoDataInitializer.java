package com.noom.meter_digital.config;

import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.entity.User;
import com.noom.meter_digital.repository.RoomRepository;
import com.noom.meter_digital.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedFiveRooms(RoomRepository rooms, UserRepository users, PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed Admin User
            if (users.findAllByUsernameOrderByIdAsc("admin").isEmpty()) {
                users.save(new User("admin", passwordEncoder.encode("admin123"), "ROLE_ADMIN", null));
            }

            // Seed Rooms 101 to 105 & Tenant Users
            for (int number = 101; number <= 105; number++) {
                String roomNumber = String.valueOf(number);
                if (rooms.findAllByRoomNumberOrderByIdAsc(roomNumber).isEmpty()) {
                    Room room = new Room();
                    room.setRoomNumber(roomNumber);
                    room.setRatePerUnit(8.0);
                    rooms.save(room);
                }
                String username = "room" + roomNumber;
                if (users.findAllByUsernameOrderByIdAsc(username).isEmpty()) {
                    users.save(new User(username, passwordEncoder.encode("1234"), "ROLE_TENANT", roomNumber));
                }
            }
        };
    }
}
