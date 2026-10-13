package com.noom.meter_digital.service;

import com.noom.meter_digital.dto.MeterSingleReading;
import com.noom.meter_digital.model.modbus.ModbusDataDecoder;
import com.noom.meter_digital.model.modbus.ModbusProfile;
import com.noom.meter_digital.model.modbus.RegisterMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModbusMasterService {

    private static final Logger log = LoggerFactory.getLogger(ModbusMasterService.class);

    @Autowired
    private ModbusSimulatorService simulatorService;

    @Autowired
    private ModbusProfileManagerService profileManagerService;

    private int transactionIdCounter = 1;

    public MeterSingleReading readMeterViaModbus(String ipAddress, int port, int slaveId, String model, String roomNumber, String protocolMode) {
        if ("127.0.0.1".equals(ipAddress) || "localhost".equalsIgnoreCase(ipAddress) || "SIMULATOR".equalsIgnoreCase(ipAddress)) {
            if (simulatorService != null) {
                return simulatorService.generateSimulatedReading(slaveId, model, roomNumber);
            }
        }

        MeterSingleReading reading = null;
        if ("MODBUS_RTU_OVER_TCP".equalsIgnoreCase(protocolMode)) {
            reading = readMeterViaModbusRtuOverTcp(ipAddress, port, slaveId, model, roomNumber);
            if (reading == null) reading = readMeterViaModbusTcp(ipAddress, port, slaveId, model, roomNumber);
        } else if ("MODBUS_TCP".equalsIgnoreCase(protocolMode)) {
            reading = readMeterViaModbusTcp(ipAddress, port, slaveId, model, roomNumber);
            if (reading == null) reading = readMeterViaModbusRtuOverTcp(ipAddress, port, slaveId, model, roomNumber);
        } else {
            // AUTO: try Modbus RTU over TCP if port != 502, else Modbus TCP first
            if (port != 502 && port > 0) {
                reading = readMeterViaModbusRtuOverTcp(ipAddress, port, slaveId, model, roomNumber);
                if (reading == null) reading = readMeterViaModbusTcp(ipAddress, port, slaveId, model, roomNumber);
            } else {
                reading = readMeterViaModbusTcp(ipAddress, port, slaveId, model, roomNumber);
                if (reading == null) reading = readMeterViaModbusRtuOverTcp(ipAddress, port, slaveId, model, roomNumber);
            }
        }

        // If physical device not reachable, return null (do not fallback to simulator unless explicitly requested)
        if (reading == null && ("SIMULATOR".equalsIgnoreCase(ipAddress) || "127.0.0.1".equals(ipAddress) || "localhost".equalsIgnoreCase(ipAddress))) {
            if (simulatorService != null) {
                log.info("ℹ️ Using Virtual Simulator Reading for Slave {}", slaveId);
                return simulatorService.generateSimulatedReading(slaveId, model, roomNumber);
            }
        }

        return reading;
    }

    public MeterSingleReading readMeterViaModbusTcp(String ipAddress, int port, int slaveId, String model, String roomNumber) {
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("Cannot poll Modbus TCP: IP address is missing");
            return null;
        }

        ModbusProfile profile = getProfileForModel(model);
        int functionCode = profile.getDefaultFunctionCode();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port > 0 ? port : 502), 3000);
            socket.setSoTimeout(3000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            MeterSingleReading reading = new MeterSingleReading();
            reading.setMeterId("MODBUS-METER-" + slaveId + "-" + System.currentTimeMillis());
            reading.setRoomNumber(roomNumber);

            boolean anySuccess = false;

            for (RegisterMapping mapping : profile.getRegisterMappings()) {
                int startReg = mapping.getRegisterAddress();
                int regCount = mapping.getRegisterCount() > 0 ? mapping.getRegisterCount() : 2;

                int txId = ++transactionIdCounter & 0xFFFF;
                byte[] request = new byte[12];
                request[0] = (byte) (txId >> 8);
                request[1] = (byte) (txId & 0xFF);
                request[2] = 0x00;
                request[3] = 0x00;
                request[4] = 0x00;
                request[5] = 0x06;
                request[6] = (byte) (slaveId & 0xFF);
                request[7] = (byte) functionCode;
                request[8] = (byte) (startReg >> 8);
                request[9] = (byte) (startReg & 0xFF);
                request[10] = (byte) (regCount >> 8);
                request[11] = (byte) (regCount & 0xFF);

                out.write(request);
                out.flush();

                byte[] mbap = new byte[7];
                in.readFully(mbap);

                int respLen = ((mbap[4] & 0xFF) << 8) | (mbap[5] & 0xFF);
                byte[] pdu = new byte[respLen - 1];
                in.readFully(pdu);

                int respFc = pdu[0] & 0xFF;
                if (respFc >= 0x80) {
                    log.warn("Modbus TCP Exception 0x{} reading Reg {} for IP {}", Integer.toHexString(respFc), startReg, ipAddress);
                    continue;
                }

                int byteCount = pdu[1] & 0xFF;
                byte[] registerData = new byte[byteCount];
                System.arraycopy(pdu, 2, registerData, 0, Math.min(byteCount, pdu.length - 2));

                double val = ModbusDataDecoder.decodeValue(registerData, 0, mapping.getDataType(), mapping.getMultiplier());
                String metric = mapping.getMetric() != null ? mapping.getMetric().toLowerCase() : "";

                if (metric.contains("volt")) { reading.setVoltage(val); anySuccess = true; }
                else if (metric.contains("curr")) { reading.setCurrent(val); anySuccess = true; }
                else if (metric.contains("pow")) { reading.setPower(val); anySuccess = true; }
                else if (metric.contains("energ")) { reading.setEnergyKwh(val); anySuccess = true; }
            }

            return anySuccess ? reading : null;

        } catch (Exception e) {
            log.warn("Modbus TCP polling failed for IP {}:{} (Slave ID: {}): {}", ipAddress, port, slaveId, e.getMessage());
            return null;
        }
    }

    public MeterSingleReading readMeterViaModbusRtuOverTcp(String ipAddress, int port, int slaveId, String model, String roomNumber) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }

        ModbusProfile profile = getProfileForModel(model);
        int functionCode = profile.getDefaultFunctionCode();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port > 0 ? port : 4001), 3000);
            socket.setSoTimeout(3000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            MeterSingleReading reading = new MeterSingleReading();
            reading.setMeterId("MOXA-METER-" + slaveId + "-" + System.currentTimeMillis());
            reading.setRoomNumber(roomNumber);

            boolean anySuccess = false;

            for (RegisterMapping mapping : profile.getRegisterMappings()) {
                int startReg = mapping.getRegisterAddress();
                int regCount = mapping.getRegisterCount() > 0 ? mapping.getRegisterCount() : 2;

                byte[] request = new byte[8];
                request[0] = (byte) (slaveId & 0xFF);
                request[1] = (byte) (functionCode & 0xFF);
                request[2] = (byte) (startReg >> 8);
                request[3] = (byte) (startReg & 0xFF);
                request[4] = (byte) (regCount >> 8);
                request[5] = (byte) (regCount & 0xFF);

                int crc = calculateCRC16(request, 6);
                request[6] = (byte) (crc & 0xFF);
                request[7] = (byte) ((crc >> 8) & 0xFF);

                out.write(request);
                out.flush();

                byte[] header = new byte[3];
                in.readFully(header);

                int respFc = header[1] & 0xFF;
                int byteCount = header[2] & 0xFF;
                if (respFc >= 0x80) {
                    log.warn("Modbus RTU Exception 0x{} reading Reg {} for IP {}", Integer.toHexString(respFc), startReg, ipAddress);
                    continue;
                }

                byte[] dataAndCrc = new byte[byteCount + 2];
                in.readFully(dataAndCrc);

                byte[] registerData = new byte[byteCount];
                System.arraycopy(dataAndCrc, 0, registerData, 0, Math.min(byteCount, dataAndCrc.length - 2));

                double val = ModbusDataDecoder.decodeValue(registerData, 0, mapping.getDataType(), mapping.getMultiplier());
                String metric = mapping.getMetric() != null ? mapping.getMetric().toLowerCase() : "";

                if (metric.contains("volt")) { reading.setVoltage(val); anySuccess = true; }
                else if (metric.contains("curr")) { reading.setCurrent(val); anySuccess = true; }
                else if (metric.contains("pow")) { reading.setPower(val); anySuccess = true; }
                else if (metric.contains("energ")) { reading.setEnergyKwh(val); anySuccess = true; }
            }

            return anySuccess ? reading : null;

        } catch (Exception e) {
            log.warn("Modbus RTU over TCP failed for IP {}:{} (Slave ID: {}): {}", ipAddress, port, slaveId, e.getMessage());
            return null;
        }
    }

    private ModbusProfile getProfileForModel(String model) {
        if (profileManagerService != null) {
            return profileManagerService.getProfile(model);
        }
        // Fallback default
        ModbusProfile fallback = new ModbusProfile("MODEL_A", "Generic", "Standard Float Meter", "Fallback", 4, "AUTO");
        fallback.addMapping(new RegisterMapping("voltage", 0, 2, "FLOAT32_AB_CD", 1.0, "V"));
        fallback.addMapping(new RegisterMapping("current", 2, 2, "FLOAT32_AB_CD", 1.0, "A"));
        fallback.addMapping(new RegisterMapping("power", 4, 2, "FLOAT32_AB_CD", 1.0, "kW"));
        fallback.addMapping(new RegisterMapping("energyKwh", 6, 2, "FLOAT32_AB_CD", 1.0, "kWh"));
        return fallback;
    }

    private RegisterRange calculateRegisterRange(ModbusProfile profile) {
        int minAddr = 65535;
        int maxAddr = 0;

        List<RegisterMapping> mappings = profile.getRegisterMappings();
        if (mappings == null || mappings.isEmpty()) {
            return new RegisterRange(0, 8);
        }

        for (RegisterMapping m : mappings) {
            int addr = m.getRegisterAddress();
            int cnt = m.getRegisterCount() > 0 ? m.getRegisterCount() : 2;
            if (addr < minAddr) minAddr = addr;
            if ((addr + cnt) > maxAddr) maxAddr = addr + cnt;
        }

        if (minAddr > maxAddr) {
            minAddr = 0;
            maxAddr = 8;
        }

        int totalCount = maxAddr - minAddr;
        if (totalCount > 16) {
            // Safety cap: physical meters like Circutor CVM-C5 reject large gap reads
            totalCount = 8;
        }

        return new RegisterRange(minAddr, totalCount);
    }

    private MeterSingleReading parseReadingFromProfile(byte[] registerData, int baseRegisterAddress, ModbusProfile profile, String roomNumber, int slaveId, String idPrefix) {
        double voltage = 0.0;
        double current = 0.0;
        double power = 0.0;
        double energyKwh = 0.0;

        for (RegisterMapping mapping : profile.getRegisterMappings()) {
            int regOffset = mapping.getRegisterAddress() - baseRegisterAddress;
            int byteOffset = regOffset * 2; // Each 16-bit register is 2 bytes

            if (byteOffset >= 0 && byteOffset < registerData.length) {
                double val = ModbusDataDecoder.decodeValue(registerData, byteOffset, mapping.getDataType(), mapping.getMultiplier());
                String metric = mapping.getMetric() != null ? mapping.getMetric().toLowerCase() : "";

                if ("voltage".equals(metric) || "v".equals(metric)) {
                    voltage = val;
                } else if ("current".equals(metric) || "a".equals(metric) || "amp".equals(metric)) {
                    current = val;
                } else if ("power".equals(metric) || "kw".equals(metric) || "watt".equals(metric)) {
                    power = val;
                } else if ("energykwh".equals(metric) || "kwh".equals(metric) || "energy".equals(metric)) {
                    energyKwh = val;
                }
            }
        }

        String meterId = idPrefix + (roomNumber != null ? roomNumber : slaveId);
        MeterSingleReading reading = new MeterSingleReading(
                roomNumber != null ? roomNumber : String.valueOf(slaveId),
                meterId,
                Math.round(voltage * 100.0) / 100.0,
                Math.round(current * 1000.0) / 1000.0,
                Math.round(energyKwh * 10.0) / 10.0
        );
        reading.setPower(Math.round(power * 1000.0) / 1000.0);
        return reading;
    }

    public Map<String, Object> testConnection(String ipAddress, int port, int slaveId, String model) {
        return testConnection(ipAddress, port, slaveId, model, "AUTO");
    }

    public Map<String, Object> testConnection(String ipAddress, int port, int slaveId, String model, String protocolMode) {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();

        if (ipAddress == null || ipAddress.isBlank()) {
            result.put("success", false);
            result.put("message", "IP Address is missing");
            return result;
        }

        try {
            MeterSingleReading reading = readMeterViaModbus(ipAddress, port, slaveId, model, "TEST", protocolMode);
            long elapsed = System.currentTimeMillis() - start;

            if (reading != null) {
                result.put("success", true);
                result.put("message", "Modbus connection successful (MOXA / Gateway)!");
                result.put("responseTimeMs", elapsed);
                result.put("ipAddress", ipAddress);
                result.put("port", port > 0 ? port : 502);
                result.put("slaveId", slaveId);
                result.put("voltage", reading.getVoltage());
                result.put("current", reading.getCurrent());
                result.put("power", reading.getPower());
                result.put("energyKwh", reading.getEnergyKwh());
                result.put("protocolUsed", "MODBUS_RTU_OVER_TCP".equalsIgnoreCase(protocolMode) ? "Modbus RTU over TCP (MOXA)" : "Modbus TCP");
            } else {
                result.put("success", false);
                result.put("message", "Could not read data from MOXA / Converter at " + ipAddress + ":" + (port > 0 ? port : 502) + " (Slave ID: " + slaveId + ")");
                result.put("responseTimeMs", elapsed);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            result.put("success", false);
            result.put("message", "Connection failed: " + e.getMessage());
            result.put("responseTimeMs", elapsed);
        }

        return result;
    }

    public static int calculateCRC16(byte[] data, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    private static class RegisterRange {
        int startRegister;
        int registerCount;

        RegisterRange(int startRegister, int registerCount) {
            this.startRegister = startRegister;
            this.registerCount = registerCount;
        }
    }
}
