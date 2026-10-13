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

def read_modbus_rtu(ip, port, slave_id, fc, start_reg, reg_count):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1.5)
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
            byte_cnt = resp[2]
            return resp[3:3+byte_cnt]
    except Exception as e:
        print(f"Read error FC={fc}, start={start_reg}: {e}")
    return None

if __name__ == "__main__":
    ip = "169.254.51.66"
    port = 4045
    slave = 1

    for fc in [4, 3]:
        print(f"\n==================== READING FC {fc} (Start=0, Count=20) ====================")
        data = read_modbus_rtu(ip, port, slave, fc, 0, 20)
        if data:
            print(f"Raw Hex ({len(data)} bytes): {data.hex(' ').upper()}\n")
            for i in range(0, len(data) - 3, 2):
                reg_addr = i // 2
                u16 = (data[i] << 8) | data[i+1]
                s16 = struct.unpack(">h", data[i:i+2])[0]
                
                b0, b1, b2, b3 = data[i], data[i+1], data[i+2], data[i+3]
                f_abcd = struct.unpack(">f", bytes([b0, b1, b2, b3]))[0]
                f_cdab = struct.unpack(">f", bytes([b2, b3, b0, b1]))[0]
                
                u32_abcd = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
                u32_cdab = (b2 << 24) | (b3 << 16) | (b0 << 8) | b1
                
                print(f"Reg {reg_addr:2d} (0x{reg_addr:02X}): UInt16={u16:5d} | Int16={s16:5d} | Float ABCD={f_abcd:10.2f} | Float CDAB={f_cdab:10.2f} | UInt32={u32_abcd} | UInt32_CDAB={u32_cdab}")
