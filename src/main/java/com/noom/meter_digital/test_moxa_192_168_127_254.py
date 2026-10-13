import socket
import struct
import sys

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
        s.settimeout(1.2)
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
    except Exception as e:
        print(f"Error connecting to {ip}:{port}: {e}")
    return None

if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    ip = "192.168.127.254"
    ports = [4045, 4001, 4032, 502]
    slave = 1

    print("==================================================")
    print(f"TESTING MOXA AT FACTORY IP: {ip}")
    print("==================================================")

    for port in ports:
        print(f"\nTesting IP {ip} | Port {port}...")
        raw_v = read_modbus_reg(ip, port, slave, 0x0000, 2)
        raw_t2 = read_modbus_reg(ip, port, slave, 0x006C, 2)

        if raw_v is not None or raw_t2 is not None:
            print(f"✅ [SUCCESSFUL MATCH!] IP={ip}, Port={port}, SlaveID={slave}")
            v_val = (raw_v / 10.0) if raw_v is not None else 0.0
            t2_val = (raw_t2 / 1000.0) if raw_t2 is not None else 0.0
            print(f"  Voltage V1  : {v_val:.2f} V")
            print(f"  Energy (T2) : {t2_val:.3f} kWh")
        else:
            print(f"  Port {port}: No response on Slave ID {slave}.")

    print("\n==================================================")
