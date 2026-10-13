package com.noom.meter_digital.service;

import com.noom.meter_digital.dto.MeterSingleReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ModbusSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(ModbusSimulatorService.class);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private Thread serverThread;
    private int port = 10502;

    // Simulated energy kWh per slave ID
    private final Map<Integer, Double> energyMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void autoStart() {
        startSimulator(10502);
    }

    @PreDestroy
    public void autoStop() {
        stopSimulator();
    }

    public synchronized void startSimulator(int listenPort) {
        if (running.get()) {
            log.info("🤖 Modbus Simulator is already running on port {}", this.port);
            return;
        }
        this.port = listenPort > 0 ? listenPort : 10502;
        running.set(true);

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(this.port);
                log.info("🤖 Virtual Modbus TCP / RTU Simulator Server started on port {}", this.port);
                while (running.get() && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(() -> handleClient(clientSocket)).start();
                    } catch (Exception e) {
                        if (!running.get()) break;
                        log.debug("Simulator accept exception: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to start Modbus Simulator Server on port {}: {}", this.port, e.getMessage());
            } finally {
                running.set(false);
            }
        });
        serverThread.setDaemon(true);
        serverThread.setName("modbus-simulator-thread");
        serverThread.start();
    }

    public synchronized void stopSimulator() {
        running.set(false);
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
        }
        log.info("🛑 Virtual Modbus Simulator stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        return port;
    }

    public MeterSingleReading generateSimulatedReading(int slaveId, String model, String roomNumber) {
        double baseEnergy = energyMap.computeIfAbsent(slaveId, id -> 1245.0 + (id * 12.3));
        double newEnergy = baseEnergy + (Math.random() * 0.08);
        energyMap.put(slaveId, newEnergy);

        double voltage = 220.0 + (Math.random() * 4.5) - 2.0; // 218.0V - 222.5V
        double current = 1.2 + (Math.random() * 8.5);         // 1.2A - 9.7A
        double power = (voltage * current * 0.95) / 1000.0;   // kW

        MeterSingleReading reading = new MeterSingleReading(
                roomNumber != null ? roomNumber : String.valueOf(slaveId),
                "SIM-METER-" + slaveId,
                Math.round(voltage * 100.0) / 100.0,
                Math.round(current * 1000.0) / 1000.0,
                Math.round(newEnergy * 10.0) / 10.0
        );
        reading.setPower(Math.round(power * 1000.0) / 1000.0);
        return reading;
    }

    private void handleClient(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            socket.setSoTimeout(3000);

            byte[] buffer = new byte[256];
            int readBytes = in.read(buffer);
            if (readBytes < 6) return;

            // Check if MBAP Header (Modbus TCP) or Modbus RTU over TCP
            boolean isModbusTcp = (readBytes >= 12 && buffer[2] == 0 && buffer[3] == 0);

            int slaveId;
            int fc;
            if (isModbusTcp) {
                slaveId = buffer[6] & 0xFF;
                fc = buffer[7] & 0xFF;
            } else {
                slaveId = buffer[0] & 0xFF;
                fc = buffer[1] & 0xFF;
            }

            MeterSingleReading reading = generateSimulatedReading(slaveId, "MODEL_A", String.valueOf(slaveId));

            byte[] dataBytes = new byte[16];
            writeFloat(dataBytes, 0, reading.getVoltage() != null ? reading.getVoltage().floatValue() : 220.0f);
            writeFloat(dataBytes, 4, reading.getCurrent() != null ? reading.getCurrent().floatValue() : 2.5f);
            writeFloat(dataBytes, 8, reading.getPower() != null ? reading.getPower().floatValue() : 0.55f);
            writeFloat(dataBytes, 12, reading.getEnergyKwh() != null ? reading.getEnergyKwh().floatValue() : 1245.0f);


            if (isModbusTcp) {
                // Response MBAP: TxID (2), Proto (2), Length (2), UnitID (1), FC (1), ByteCount (1), Data (16)
                byte[] response = new byte[7 + 2 + 16];
                response[0] = buffer[0];
                response[1] = buffer[1];
                response[2] = 0;
                response[3] = 0;
                response[4] = 0;
                response[5] = 19; // 1 (UnitID) + 1 (FC) + 1 (ByteCount) + 16 (Data) = 19 bytes
                response[6] = (byte) slaveId;
                response[7] = (byte) fc;
                response[8] = 16; // Byte count
                System.arraycopy(dataBytes, 0, response, 9, 16);

                out.write(response);
                out.flush();
            } else {
                // Response RTU: SlaveID (1), FC (1), ByteCount (1), Data (16), CRC (2)
                byte[] frame = new byte[3 + 16 + 2];
                frame[0] = (byte) slaveId;
                frame[1] = (byte) fc;
                frame[2] = 16;
                System.arraycopy(dataBytes, 0, frame, 3, 16);

                int crc = ModbusMasterService.calculateCRC16(frame, 19);
                frame[19] = (byte) (crc & 0xFF);
                frame[20] = (byte) ((crc >> 8) & 0xFF);

                out.write(frame);
                out.flush();
            }
        } catch (Exception e) {
            log.debug("Client handler exception in Modbus Simulator: {}", e.getMessage());
        }
    }

    private void writeFloat(byte[] bytes, int offset, float value) {
        ByteBuffer bb = ByteBuffer.wrap(bytes, offset, 4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putFloat(value);
    }
}
