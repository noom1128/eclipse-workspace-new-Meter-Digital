package com.noom.meter_digital.service;

import com.noom.meter_digital.dto.MeterSingleReading;
import com.noom.meter_digital.entity.GatewayDevice;
import com.noom.meter_digital.entity.MeterReading;
import com.noom.meter_digital.entity.Room;
import com.noom.meter_digital.repository.GatewayDeviceRepository;
import com.noom.meter_digital.repository.MeterReadingRepository;
import com.noom.meter_digital.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ModbusPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ModbusPollingScheduler.class);

    @Autowired
    private GatewayDeviceRepository deviceRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private ModbusMasterService modbusMasterService;

    @Autowired
    private MeterNotificationService notificationService;

    private final Map<Long, Long> lastPollTimeMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 10000) // Checks every 10 seconds
    public void pollModbusDevices() {
        List<GatewayDevice> devices = deviceRepository.findAll();
        long currentTime = System.currentTimeMillis();

        for (GatewayDevice device : devices) {
            String mode = device.getReaderMode();
            // Mode 3: RS485 to Wi-Fi Converter (Modbus TCP)
            // Mode 4: RS485 to LAN Converter (Modbus TCP)
            if ("MODE_MODBUS_WIFI".equalsIgnoreCase(mode) || "MODE_MODBUS_LAN".equalsIgnoreCase(mode) || "MODE_MOXA_SERIAL".equalsIgnoreCase(mode) || (mode != null && mode.startsWith("MODE_MOXA"))) {
                int intervalSec = device.getPollingIntervalSeconds() != null && device.getPollingIntervalSeconds() > 0
                        ? device.getPollingIntervalSeconds() : 60;
                long intervalMs = intervalSec * 1000L;

                Long lastPoll = lastPollTimeMap.get(device.getId());
                if (lastPoll != null && (currentTime - lastPoll) < intervalMs) {
                    continue; // Skip if interval has not elapsed
                }

                lastPollTimeMap.put(device.getId(), currentTime);

                String targetRoom = device.getTargetRoomNumber();
                if (targetRoom == null || targetRoom.isBlank()) {
                    targetRoom = "101"; // Default target room fallback
                }

                log.info("Polling Modbus Converter/MOXA [{}] IP: {}:{}, Slave: {}, Model: {}, Protocol: {}",
                        device.getDeviceId(), device.getIpAddress(), device.getPort(), device.getSlaveId(), device.getActiveMeterModel(), device.getProtocolMode());

                MeterSingleReading readingDto = modbusMasterService.readMeterViaModbus(
                        device.getIpAddress(),
                        device.getPort(),
                        device.getSlaveId(),
                        device.getActiveMeterModel(),
                        targetRoom,
                        device.getProtocolMode()
                );

                if (readingDto != null) {
                    saveAndBroadcastReading(device, readingDto);
                } else {
                    device.setStatus("OFFLINE");
                    deviceRepository.save(device);
                }
            }
        }
    }

    private void saveAndBroadcastReading(GatewayDevice device, MeterSingleReading readingDto) {
        String roomNumber = readingDto.getRoomNumber();
        Room room = roomRepository.findAllByRoomNumberOrderByIdAsc(roomNumber).stream()
                .findFirst()
                .orElse(null);

        if (room == null) {
            log.warn("Room {} not found for Modbus device {}, skipping DB save", roomNumber, device.getDeviceId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        MeterReading reading = new MeterReading();
        reading.setRoom(room);
        reading.setRoomNumber(roomNumber);
        reading.setMeterId(readingDto.getMeterId() != null ? readingDto.getMeterId() : "METER-" + roomNumber);
        reading.setUnit(readingDto.getEnergyKwh());
        reading.setEnergy(readingDto.getEnergyKwh());
        reading.setValue(readingDto.getEnergyKwh());
        reading.setVoltage(readingDto.getVoltage());
        reading.setCurrent(readingDto.getCurrent());
        reading.setPower(readingDto.getPower());
        reading.setTimestamp(now);
        reading.setRecordDate(LocalDate.now());
        reading.setMonth(now.format(DateTimeFormatter.ofPattern("yyyy-MM")));

        meterReadingRepository.save(reading);

        // Update device status
        device.setStatus("ONLINE");
        device.setLastHeartbeat(now);
        deviceRepository.save(device);

        // Broadcast to Dashboard via SSE & WebSocket
        notificationService.broadcastTelemetryUpdate(reading);
        log.info("Saved and broadcast Modbus reading for Room {}: {} kWh", roomNumber, readingDto.getEnergyKwh());
    }
}
