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

def read_rtu_over_tcp(ip, port, slave_id, address=0, count=2, timeout=0.15):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
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
            return resp, "OK" if calc_crc == recv_crc else "CRC_ERR"
    except Exception:
        pass
    return None, "TIMEOUT"

if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    ip = "169.254.51.66"
    ports = [4045, 4032]
    
    print("==================================================")
    print("AUTOMATIC DEEP SCANNER (SLAVE IDs 1 TO 247)")
    print("==================================================")

    found = []
    for port in ports:
        print(f"\nScanning Port {port} for active Slave IDs (1..247)...")
        for slave in range(1, 248):
            if slave % 10 == 0 or slave == 1:
                sys.stdout.write(f"\rPort {port} | Scanning Slave ID {slave:3d}/247...")
                sys.stdout.flush()

            resp, status = read_rtu_over_tcp(ip, port, slave, 0, 2, timeout=0.12)
            if resp is not None:
                found.append((port, slave, resp))
                print(f"\n[FOUND RESPONSIVE SLAVE ID!] Port={port}, SlaveID={slave}, Status={status}")
                raw_bytes = resp[3:-2] if len(resp) >= 5 else resp
                if len(raw_bytes) >= 4:
                    raw_v = (raw_bytes[0] << 24) | (raw_bytes[1] << 16) | (raw_bytes[2] << 8) | raw_bytes[3]
                    print(f"   Voltage V1: {raw_v / 10.0:.2f} V")

    print(f"\n==================================================")
    print(f"Scan complete. Total active slaves discovered: {len(found)}")
    print("==================================================")
