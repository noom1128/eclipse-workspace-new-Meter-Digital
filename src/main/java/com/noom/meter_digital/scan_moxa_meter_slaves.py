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

def read_rtu_over_tcp(ip, port, slave_id, fc, start_reg, reg_count, timeout=0.8):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        s.connect((ip, port))

        req = bytearray([
            slave_id & 0xFF,
            fc & 0xFF,
            (start_reg >> 8) & 0xFF,
            start_reg & 0xFF,
            (reg_count >> 8) & 0xFF,
            reg_count & 0xFF
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
        elif len(resp) > 0:
            return resp, "SHORT_RESP"
    except Exception as e:
        return None, str(e)

    return None, "TIMEOUT"

if __name__ == "__main__":
    ip = "169.254.51.66"
    ports = [4032, 4045]
    print(f"Scanning Slave IDs 1 to 16 on {ip} across ports {ports}...")

    found = []
    for port in ports:
        print(f"\n--- Testing TCP Port {port} ---")
        for slave in range(1, 17):
            for fc in [3, 4]:
                sys.stdout.write(f"\rPort {port} | Testing Slave ID {slave:2d} (FC={fc})...")
                sys.stdout.flush()

                resp, status = read_rtu_over_tcp(ip, port, slave, fc, 0, 2, timeout=0.8)
                if resp is not None:
                    found.append((port, slave, fc, status, resp.hex(' ').upper()))
                    print(f"\n[FOUND DEVICE!] Port={port}, Slave ID={slave}, FunctionCode={fc}, Status={status}, Data={resp.hex(' ').upper()}")
                    
                    if len(resp) >= 7:
                        raw_bytes = resp[3:-2]
                        if len(raw_bytes) >= 4:
                            f_abcd = struct.unpack(">f", raw_bytes[:4])[0]
                            f_cdab = struct.unpack(">f", raw_bytes[2:4] + raw_bytes[:2])[0]
                            print(f"   Decoded Float32: ABCD={f_abcd:.2f}, CDAB={f_cdab:.2f}")

    print(f"\nScan finished. Total responsive combinations: {len(found)}")
