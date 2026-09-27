package com.noom.meter_digital.repository;

import com.noom.meter_digital.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllByUsernameOrderByIdAsc(String username);
}
