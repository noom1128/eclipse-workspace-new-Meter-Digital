package com.noom.meter_digital.service;

import com.noom.meter_digital.model.modbus.ModbusDataDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

@Service
public class ModbusScannerService {

    private static final Logger log = LoggerFactory.getLogger(ModbusScannerService.class);
    private int transactionIdCounter = 100;

    /**
     * Reads raw registers and returns a complete diagnostic report with Hex dump,
     * individual register list, and multi-format interpretations for AnyDesk/TeamViewer remote engineers.
     */
    public Map<String, Object> readRawRegisters(String ipAddress, int port, int slaveId, int functionCode, int startRegister, int registerCount, String protocolMode) {
        Map<String, Object> result = new HashMap<>();
        long start = System.currentTimeMillis();

        if (ipAddress == null || ipAddress.isBlank()) {
            result.put("success", false);
            result.put("message", "IP address is missing");
            return result;
        }

        int targetPort = (port > 0) ? port : 502;
        int fc = (functionCode == 3 || functionCode == 4) ? functionCode : 3;
        int count = Math.min(Math.max(registerCount, 1), 120); // Limit count max 120

        byte[] rawRegisterBytes = null;
        String protocolUsed = "";

        if ("MODBUS_RTU_OVER_TCP".equalsIgnoreCase(protocolMode)) {
            rawRegisterBytes = executeRtuOverTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
            protocolUsed = "MODBUS_RTU_OVER_TCP";
            if (rawRegisterBytes == null) {
                rawRegisterBytes = executeTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
                if (rawRegisterBytes != null) protocolUsed = "MODBUS_TCP";
            }
        } else if ("MODBUS_TCP".equalsIgnoreCase(protocolMode)) {
            rawRegisterBytes = executeTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
            protocolUsed = "MODBUS_TCP";
            if (rawRegisterBytes == null) {
                rawRegisterBytes = executeRtuOverTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
                if (rawRegisterBytes != null) protocolUsed = "MODBUS_RTU_OVER_TCP";
            }
        } else {
            // AUTO
            if (targetPort != 502) {
                rawRegisterBytes = executeRtuOverTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
                protocolUsed = "MODBUS_RTU_OVER_TCP";
                if (rawRegisterBytes == null) {
                    rawRegisterBytes = executeTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
                    if (rawRegisterBytes != null) protocolUsed = "MODBUS_TCP";
                }
            } else {
                rawRegisterBytes = executeTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
                protocolUsed = "MODBUS_TCP";
                if (rawRegisterBytes == null) {
                    rawRegisterBytes = executeRtuOverTcpRequest(ipAddress, targetPort, slaveId, fc, startRegister, count);
                    if (rawRegisterBytes != null) protocolUsed = "MODBUS_RTU_OVER_TCP";
                }
            }
        }

        long responseTime = System.currentTimeMillis() - start;

        if (rawRegisterBytes == null) {
            result.put("success", false);
            result.put("message", "No response from Modbus device at " + ipAddress + ":" + targetPort + " (Slave ID: " + slaveId + ")");
            result.put("responseTimeMs", responseTime);
            return result;
        }

        // Build Hex String
        StringBuilder hexSb = new StringBuilder();
        List<Map<String, Object>> registerList = new ArrayList<>();

        for (int i = 0; i < rawRegisterBytes.length; i += 2) {
            if (i + 1 < rawRegisterBytes.length) {
                int regVal = ((rawRegisterBytes[i] & 0xFF) << 8) | (rawRegisterBytes[i + 1] & 0xFF);
                int regAddr = startRegister + (i / 2);

                hexSb.append(String.format("%02X %02X ", rawRegisterBytes[i] & 0xFF, rawRegisterBytes[i + 1] & 0xFF));

                Map<String, Object> regMap = new HashMap<>();
                regMap.put("registerAddress", regAddr);
                regMap.put("hex", String.format("0x%04X", regVal));
                regMap.put("uint16", regVal);
                regMap.put("int16", (short) regVal);
                registerList.add(regMap);
            }
        }

        // Build Multi-Format Interpretations Table (for 2-word / 4-byte floating point & 32-bit values)
        List<Map<String, Object>> interpretations = new ArrayList<>();
        for (int byteOffset = 0; byteOffset + 3 < rawRegisterBytes.length; byteOffset += 2) {
            Map<String, Object> inter = ModbusDataDecoder.interpret4Bytes(rawRegisterBytes, byteOffset, startRegister);
            interpretations.add(inter);
        }

        result.put("success", true);
        result.put("message", "Modbus read successful!");
        result.put("responseTimeMs", responseTime);
        result.put("ipAddress", ipAddress);
        result.put("port", targetPort);
        result.put("slaveId", slaveId);
        result.put("functionCode", fc);
        result.put("startRegister", startRegister);
        result.put("registerCount", count);
        result.put("protocolUsed", protocolUsed);
        result.put("rawHex", hexSb.toString().trim());
        result.put("byteCount", rawRegisterBytes.length);
        result.put("registers", registerList);
        result.put("interpretations", interpretations);

        return result;
    }

    /**
     * Scan RS485 bus for active Modbus Slave IDs.
     */
    public Map<String, Object> scanSlaveIds(String ipAddress, int port, int startSlaveId, int endSlaveId, String protocolMode) {
        Map<String, Object> result = new HashMap<>();
        long startTotal = System.currentTimeMillis();

        int fromSlave = Math.max(1, Math.min(startSlaveId, 247));
        int toSlave = Math.max(fromSlave, Math.min(endSlaveId, 247));
        int targetPort = (port > 0) ? port : 502;

        List<Map<String, Object>> foundDevices = new ArrayList<>();
        int scannedCount = 0;

        log.info("Starting Modbus Slave ID scan on {}:{} range {} to {}", ipAddress, targetPort, fromSlave, toSlave);

        for (int slave = fromSlave; slave <= toSlave; slave++) {
            scannedCount++;
            long slaveStart = System.currentTimeMillis();

            byte[] res = executeRtuOverTcpRequestWithTimeout(ipAddress, targetPort, slave, 3, 0, 1, 800);
            String mode = "MODBUS_RTU_OVER_TCP";
            if (res == null) {
                res = executeTcpRequestWithTimeout(ipAddress, targetPort, slave, 3, 0, 1, 800);
                mode = "MODBUS_TCP";
            }

            if (res != null) {
                long elapsed = System.currentTimeMillis() - slaveStart;
                Map<String, Object> dev = new HashMap<>();
                dev.put("slaveId", slave);
                dev.put("status", "ONLINE");
                dev.put("responseTimeMs", elapsed);
                dev.put("protocol", mode);
                dev.put("sampleValueHex", String.format("0x%02X%02X", res[0] & 0xFF, res[1] & 0xFF));
                foundDevices.add(dev);
                log.info("Found Modbus Slave ID {} at {}:{} ({} ms)", slave, ipAddress, targetPort, elapsed);
            }
        }

        result.put("success", true);
        result.put("scannedCount", scannedCount);
        result.put("foundCount", foundDevices.size());
        result.put("foundDevices", foundDevices);
        result.put("totalTimeMs", System.currentTimeMillis() - startTotal);

        return result;
    }

    /**
     * Scan register ranges to discover where meter data registers reside.
     */
    public Map<String, Object> scanRegisterRange(String ipAddress, int port, int slaveId, int startRegister, int endRegister, int functionCode, String protocolMode) {
        Map<String, Object> result = new HashMap<>();
        long startTotal = System.currentTimeMillis();

        int startReg = Math.max(0, startRegister);
        int endReg = Math.max(startReg, Math.min(endRegister, 65535));
        int fc = (functionCode == 3 || functionCode == 4) ? functionCode : 3;

        List<Map<String, Object>> activeRegisters = new ArrayList<>();
        int chunkSize = 20;

        for (int current = startReg; current <= endReg; current += chunkSize) {
            int count = Math.min(chunkSize, endReg - current + 1);
            Map<String, Object> readRes = readRawRegisters(ipAddress, port, slaveId, fc, current, count, protocolMode);
            if (Boolean.TRUE.equals(readRes.get("success"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> regs = (List<Map<String, Object>>) readRes.get("registers");
                if (regs != null) {
                    for (Map<String, Object> r : regs) {
                        int uintVal = (Integer) r.get("uint16");
                        if (uintVal != 0 && uintVal != 0xFFFF) {
                            activeRegisters.add(r);
                        }
                    }
                }
            }
        }

        result.put("success", true);
        result.put("startRegister", startReg);
        result.put("endRegister", endReg);
        result.put("activeRegistersCount", activeRegisters.size());
        result.put("activeRegisters", activeRegisters);
        result.put("totalTimeMs", System.currentTimeMillis() - startTotal);

        return result;
    }

    private byte[] executeTcpRequest(String ip, int port, int slaveId, int fc, int startReg, int count) {
        return executeTcpRequestWithTimeout(ip, port, slaveId, fc, startReg, count, 2500);
    }

    private byte[] executeTcpRequestWithTimeout(String ip, int port, int slaveId, int fc, int startReg, int count, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            int txId = ++transactionIdCounter & 0xFFFF;
            byte[] request = new byte[12];
            request[0] = (byte) (txId >> 8);
            request[1] = (byte) (txId & 0xFF);
            request[2] = 0x00;
            request[3] = 0x00;
            request[4] = 0x00;
            request[5] = 0x06;
            request[6] = (byte) (slaveId & 0xFF);
            request[7] = (byte) (fc & 0xFF);
            request[8] = (byte) (startReg >> 8);
            request[9] = (byte) (startReg & 0xFF);
            request[10] = (byte) (count >> 8);
            request[11] = (byte) (count & 0xFF);

            out.write(request);
            out.flush();

            byte[] mbap = new byte[7];
            in.readFully(mbap);

            int respLen = ((mbap[4] & 0xFF) << 8) | (mbap[5] & 0xFF);
            byte[] pdu = new byte[respLen - 1];
            in.readFully(pdu);

            int respFc = pdu[0] & 0xFF;
            if (respFc >= 0x80) return null;

            int byteCount = pdu[1] & 0xFF;
            byte[] regData = new byte[byteCount];
            System.arraycopy(pdu, 2, regData, 0, Math.min(byteCount, pdu.length - 2));
            return regData;

        } catch (Exception e) {
            return null;
        }
    }

    private byte[] executeRtuOverTcpRequest(String ip, int port, int slaveId, int fc, int startReg, int count) {
        return executeRtuOverTcpRequestWithTimeout(ip, port, slaveId, fc, startReg, count, 2500);
    }

    private byte[] executeRtuOverTcpRequestWithTimeout(String ip, int port, int slaveId, int fc, int startReg, int count, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            byte[] request = new byte[8];
            request[0] = (byte) (slaveId & 0xFF);
            request[1] = (byte) (fc & 0xFF);
            request[2] = (byte) (startReg >> 8);
            request[3] = (byte) (startReg & 0xFF);
            request[4] = (byte) (count >> 8);
            request[5] = (byte) (count & 0xFF);

            int crc = ModbusMasterService.calculateCRC16(request, 6);
            request[6] = (byte) (crc & 0xFF);
            request[7] = (byte) ((crc >> 8) & 0xFF);

            out.write(request);
            out.flush();

            byte[] header = new byte[3];
            in.readFully(header);

            int respFc = header[1] & 0xFF;
            int byteCount = header[2] & 0xFF;

            if (respFc >= 0x80) return null;

            byte[] dataAndCrc = new byte[byteCount + 2];
            in.readFully(dataAndCrc);

            byte[] regData = new byte[byteCount];
            System.arraycopy(dataAndCrc, 0, regData, 0, Math.min(byteCount, dataAndCrc.length - 2));
            return regData;

        } catch (Exception e) {
            return null;
        }
    }
}
