import socket
import struct
import sys
import time

def broadcast_moxa_discovery():
    print("==================================================")
    print("MOXA HARDWARE UDP BROADCAST DISCOVERY")
    print("==================================================")

    # Standard Moxa NPort UDP Broadcast Search Packets
    # NPort Search payload (Magic header: 0x01, 0x00, 0x00, 0x08... or Moxa Search command)
    payloads = [
        bytes([0x01, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00]),
        bytes([0x45, 0x58, 0x50, 0x4c, 0x4f, 0x52, 0x45, 0x52]), # "EXPLORER"
        bytes.fromhex("0100000800000000"),
        bytes.fromhex("506f6c6c00000000") # "Poll"
    ]

    ports = [4800, 4000, 1029, 1028]
    found = []

    for port in ports:
        print(f"Broadcasting search packet on UDP Port {port}...")
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            sock.settimeout(1.5)

            for payload in payloads:
                sock.sendto(payload, ("255.255.255.255", port))

            start_time = time.time()
            while time.time() - start_time < 2.0:
                try:
                    data, addr = sock.recvfrom(1024)
                    found.append((addr[0], port, data.hex(' ').upper()))
                    print(f"✅ [DISCOVERED MOXA!] IP Address: {addr[0]} | UDP Port: {port}")
                    print(f"   Response Hex: {data.hex(' ').upper()[:60]}")
                except socket.timeout:
                    break
            sock.close()
        except Exception as e:
            print(f"Error on UDP port {port}: {e}")

    print("==================================================")
    print(f"Discovery complete. Discovered devices: {len(found)}")
    print("==================================================")

if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass
    broadcast_moxa_discovery()
