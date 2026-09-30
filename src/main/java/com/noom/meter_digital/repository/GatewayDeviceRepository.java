package com.noom.meter_digital.repository;

import com.noom.meter_digital.entity.GatewayDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GatewayDeviceRepository extends JpaRepository<GatewayDevice, Long> {
    Optional<GatewayDevice> findByDeviceId(String deviceId);
    Optional<GatewayDevice> findByIpAddress(String ipAddress);
}
