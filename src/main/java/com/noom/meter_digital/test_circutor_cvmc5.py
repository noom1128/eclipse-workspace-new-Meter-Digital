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

def bytes4_to_uint32(data: bytes) -> int:
    return (data[0] << 24) | (data[1] << 16) | (data[2] << 8) | data[3]

def read_modbus_reg(ip, port, slave_id, address, count=2):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1.5)
        s.connect((ip, port))

        req = bytearray([
            slave_id & 0xFF,
            0x03, # FC 3
            (address >> 8) & 0xFF,
            address & 0xFF,
            (count >> 8) & 0xFF,
            count & 0xFF
        ])
        crc = calculate_crc16(req)
        req.append(crc & 0xFF)
        req.append((crc >> 8) & 0xFF)

        s.sendall(req)
        resp = s.recv(256)
        s.close()

        if len(resp) >= 5:
            calc_crc = calculate_crc16(resp[:-2])
            recv_crc = resp[-2] | (resp[-1] << 8)
            if calc_crc == recv_crc:
                raw_bytes = resp[3:-2]
                return bytes4_to_uint32(raw_bytes)
    except Exception:
        pass
    return None

if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    ip = "169.254.51.66"
    ports = [4031, 4045, 4032]
    slave_ids = [1, 31, 2, 3]

    print("==================================================")
    print("CIRCUTOR CVM-C5 PORT 31 (TCP 4031) READ TEST")
    print("==================================================")

    for port in ports:
        print(f"\nTesting Moxa TCP Port {port}...")
        for slave in slave_ids:
            raw_v = read_modbus_reg(ip, port, slave, 0x0000, 2)
            raw_t2 = read_modbus_reg(ip, port, slave, 0x006C, 2)

            if raw_v is not None or raw_t2 is not None:
                print(f"[CIRCUTOR SUCCESSFUL MATCH!] IP={ip}, Port={port}, SlaveID={slave}")
                v_val = (raw_v / 10.0) if raw_v is not None else 0.0
                t2_val = (raw_t2 / 1000.0) if raw_t2 is not None else 0.0
                print(f"  Voltage V1  : {v_val:.2f} V")
                print(f"  Energy (T2) : {t2_val:.3f} kWh")
            else:
                sys.stdout.write(f"\rTesting Port {port} | Slave ID {slave}... Timeout")
                sys.stdout.flush()

    print("\n==================================================")
