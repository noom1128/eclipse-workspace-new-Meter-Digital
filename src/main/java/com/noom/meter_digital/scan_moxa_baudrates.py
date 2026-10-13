import urllib.request
import urllib.parse
import socket
import struct
import sys
import time

ip = "169.254.51.66"

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

def set_moxa_serial(port_no, baud_idx, parity_idx):
    params = {
        'PortNo': port_no,
        'BaudRate': str(baud_idx),
        'DataBits': '3',  # 8
        'StopBits': '0',  # 1
        'Parity': str(parity_idx), # 0=None, 1=Odd, 2=Even
        'FlowCtrl': '1',  # None
        'FIFO': '0',
        'IfType': '2'     # RS-485 2-Wire
    }
    url_ser = f"http://{ip}/Set.htm?{urllib.parse.urlencode(params)}"
    try:
        req = urllib.request.Request(url_ser, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            pass
        req_save = urllib.request.Request(f"http://{ip}/09Set.htm", headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req_save, timeout=3) as resp:
            pass
        time.sleep(2)
        return True
    except Exception as e:
        print(f"Set baud error: {e}")
        return False

def read_rtu_over_tcp(ip, port, slave_id, address=0, count=2):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(1.0)
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
                return resp
    except Exception:
        pass
    return None

if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    # Baudrate candidates: 9600 (10), 19200 (11), 38400 (12), 4800 (8), 2400 (7)
    baud_map = {10: "9600", 11: "19200", 12: "38400", 8: "4800", 7: "2400"}
    parity_map = {0: "None (8N1)", 2: "Even (8E1)"}
    slave_candidates = [1, 31, 2, 3]

    print("==================================================")
    print("AUTOMATIC BAUDRATE & PARITY MATRIX SCANNER")
    print("==================================================")

    found = []
    for b_idx, b_name in baud_map.items():
        for p_idx, p_name in parity_map.items():
            print(f"\nConfiguring Moxa Serial to {b_name} bps | Parity: {p_name}...")
            set_moxa_serial("01", b_idx, p_idx)
            set_moxa_serial("02", b_idx, p_idx)

            for port in [4045, 4032]:
                for slave in slave_candidates:
                    resp = read_rtu_over_tcp(ip, port, slave, 0, 2)
                    if resp is not None:
                        found.append((b_name, p_name, port, slave, resp))
                        print(f"\n[FOUND MATCHING BAUDRATE & PARITY!] Baud={b_name}, Parity={p_name}, Port={port}, SlaveID={slave}")
                        raw_bytes = resp[3:-2]
                        if len(raw_bytes) >= 4:
                            raw_v = (raw_bytes[0] << 24) | (raw_bytes[1] << 16) | (raw_bytes[2] << 8) | raw_bytes[3]
                            print(f"   Voltage V1: {raw_v / 10.0:.2f} V")

    print("\n==================================================")
    print(f"Matrix scan finished. Discovered matches: {len(found)}")
    print("==================================================")

    # Revert Moxa to 9600 8N1 default
    set_moxa_serial("01", 10, 0)
    set_moxa_serial("02", 10, 0)
