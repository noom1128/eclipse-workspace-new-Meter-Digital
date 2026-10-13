import socket
import struct
import sys
import time

def calculate_crc16(data: bytes) -> int:
    crc = 0xFFFF
    for byte in data:
        crc ^= byte
        for _ in range(8):
            if crc & 0x0001:
                crc = (crc >> 1) ^ 0xA001
            else:
                crc >>= 1
    return crc & 0xFFFF

def read_modbus_rtu_over_tcp(ip, port, slave_id, func_code, start_reg, reg_count, timeout=2.5):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        s.connect((ip, port))

        req = bytearray([
            slave_id & 0xFF,
            func_code & 0xFF,
            (start_reg >> 8) & 0xFF,
            start_reg & 0xFF,
            (reg_count >> 8) & 0xFF,
            reg_count & 0xFF
        ])
        crc = calculate_crc16(req)
        req.append(crc & 0xFF)
        req.append((crc >> 8) & 0xFF)

        s.sendall(req)

        resp_hdr = s.recv(3)
        if len(resp_hdr) < 3:
            s.close()
            return None, f"Incomplete header response ({len(resp_hdr)} bytes)"

        slave_resp, fc_resp, byte_count = resp_hdr[0], resp_hdr[1], resp_hdr[2]
        if fc_resp >= 0x80:
            s.close()
            return None, f"Modbus Exception 0x{fc_resp:02X}"

        data_and_crc = s.recv(byte_count + 2)
        s.close()

        if len(data_and_crc) < byte_count + 2:
            return None, f"Incomplete payload response ({len(data_and_crc)} bytes)"

        raw_data = data_and_crc[:byte_count]
        recv_crc = data_and_crc[byte_count] | (data_and_crc[byte_count + 1] << 8)
        
        full_frame = resp_hdr + data_and_crc
        calc_crc = calculate_crc16(full_frame[:-2])

        crc_valid = (recv_crc == calc_crc)
        return raw_data, "OK" if crc_valid else "CRC_WARNING"

    except Exception as e:
        return None, str(e)

def read_modbus_tcp(ip, port, slave_id, func_code, start_reg, reg_count, timeout=2.5):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        s.connect((ip, port))

        tx_id = 1
        req = struct.pack(">HHHBBHH", tx_id, 0, 6, slave_id, func_code, start_reg, reg_count)
        s.sendall(req)

        resp_hdr = s.recv(7)
        if len(resp_hdr) < 7:
            s.close()
            return None, f"Incomplete MBAP header response ({len(resp_hdr)} bytes)"

        mbap = struct.unpack(">HHHBB", resp_hdr)
        pdu_len = mbap[2] - 1
        pdu = s.recv(pdu_len)
        s.close()

        if len(pdu) < 2:
            return None, "Incomplete PDU"

        fc_resp = pdu[0]
        if fc_resp >= 0x80:
            return None, f"Modbus Exception 0x{fc_resp:02X}"

        byte_count = pdu[1]
        raw_data = pdu[2:2+byte_count]
        return raw_data, "OK"

    except Exception as e:
        return None, str(e)

def decode_values(raw_bytes, base_reg=0):
    print("\n--- [REGISTER DATA DECODING] ---")
    print(f"Raw Hex: {raw_bytes.hex(' ').upper()}")
    
    print("\n16-Bit Registers:")
    for i in range(0, len(raw_bytes), 2):
        if i + 1 < len(raw_bytes):
            val_u16 = (raw_bytes[i] << 8) | raw_bytes[i+1]
            val_s16 = struct.unpack(">h", raw_bytes[i:i+2])[0]
            reg_addr = base_reg + (i // 2)
            print(f"  Register {reg_addr} (0x{reg_addr:04X}): Hex=0x{val_u16:04X}, UInt16={val_u16}, Int16={val_s16}")

    print("\n32-Bit Float Interpretations (2 registers):")
    for i in range(0, len(raw_bytes) - 3, 2):
        b0, b1, b2, b3 = raw_bytes[i], raw_bytes[i+1], raw_bytes[i+2], raw_bytes[i+3]
        reg_addr = base_reg + (i // 2)

        try:
            f_abcd = struct.unpack(">f", bytes([b0, b1, b2, b3]))[0]
            f_cdab = struct.unpack(">f", bytes([b2, b3, b0, b1]))[0]
            f_badc = struct.unpack(">f", bytes([b1, b0, b3, b2]))[0]
            f_dcba = struct.unpack(">f", bytes([b3, b2, b1, b0]))[0]

            print(f"  Reg {reg_addr}-{reg_addr+1}: ABCD={f_abcd:.4f} | CDAB={f_cdab:.4f} | BADC={f_badc:.4f} | DCBA={f_dcba:.4f}")
        except Exception:
            pass

def scan_slaves(ip, port, mode="AUTO"):
    print(f"\nScanning Modbus Slave IDs on {ip}:{port} (Mode: {mode})...")
    found = []
    for slave in range(1, 33):
        sys.stdout.write(f"\rTesting Slave ID {slave:2d}/32...")
        sys.stdout.flush()

        data = None
        if mode in ("RTU", "AUTO"):
            data, status = read_modbus_rtu_over_tcp(ip, port, slave, 3, 0, 2, timeout=0.8)
        if data is None and mode in ("TCP", "AUTO"):
            data, status = read_modbus_tcp(ip, port, slave, 3, 0, 2, timeout=0.8)

        if data is not None:
            found.append((slave, status, data.hex(' ').upper()))
            print(f"\n[FOUND DEVICE] Slave ID {slave}! Sample data: {data.hex(' ').upper()}")

    print(f"\nScan complete. Total devices found: {len(found)}")
    return found


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    ip = sys.argv[1] if len(sys.argv) > 1 else "192.168.127.254"
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 4001
    slave_id = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    func_code = int(sys.argv[4]) if len(sys.argv) > 4 else 3
    start_reg = int(sys.argv[5]) if len(sys.argv) > 5 else 0
    reg_count = int(sys.argv[6]) if len(sys.argv) > 6 else 8

    print("==========================================")
    print("MOXA MODBUS METER TEST READER & SCANNER")
    print("==========================================")
    print(f"Target: IP={ip}, Port={port}, SlaveID={slave_id}")
    print(f"Reading: FC={func_code}, StartRegister={start_reg}, Count={reg_count}")

    print(f"\nAttempting Modbus RTU over TCP ({ip}:{port})...")
    data, status = read_modbus_rtu_over_tcp(ip, port, slave_id, func_code, start_reg, reg_count)

    if data is None:
        print(f"RTU over TCP failed: {status}")
        print(f"Attempting Modbus TCP ({ip}:{port})...")
        data, status = read_modbus_tcp(ip, port, slave_id, func_code, start_reg, reg_count)

    if data is not None:
        print(f"READ SUCCESSFUL! Status: {status}")
        decode_values(data, start_reg)
    else:
        print(f"\nDirect read failed: {status}")
        scan_slaves(ip, port)

