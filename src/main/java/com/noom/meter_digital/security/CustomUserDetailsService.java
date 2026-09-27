package com.noom.meter_digital.security;

import com.noom.meter_digital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void initDefaultUsers() {
        try {
            if (userRepository.count() == 0) {
                userRepository.save(new com.noom.meter_digital.entity.User(
                        "admin",
                        passwordEncoder.encode("1234"),
                        "ROLE_ADMIN",
                        null
                ));
                userRepository.save(new com.noom.meter_digital.entity.User(
                        "room101",
                        passwordEncoder.encode("1234"),
                        "ROLE_TENANT",
                        "101"
                ));
                System.out.println(">>> Initialized default users in Database (users table)");
            }
        } catch (Exception e) {
            System.err.println("User Data Seeder Note: " + e.getMessage());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.noom.meter_digital.entity.User dbUser = userRepository.findAllByUsernameOrderByIdAsc(username).stream()
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found in DB: " + username));

        return new org.springframework.security.core.userdetails.User(
                dbUser.getUsername(),
                dbUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(dbUser.getRole()))
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }
}

