import socket
import concurrent.futures
import sys

def check_port(ip, port):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(0.5)
        res = sock.connect_ex((ip, port))
        sock.close()
        if res == 0:
            return port
    except Exception:
        pass
    return None

if __name__ == "__main__":
    ip = sys.argv[1] if len(sys.argv) > 1 else "169.254.51.66"
    print(f"Scanning open ports on {ip}...")

    # Focus on common Moxa / Modbus ports
    common_ports = [502, 503, 4001, 4002, 4003, 4004, 950, 951, 960, 961, 80, 443, 23, 22, 1001, 2001, 4000, 5000, 10001, 10502]
    all_ports = list(dict.fromkeys(common_ports + list(range(1, 1024)) + list(range(4000, 4010))))

    open_ports = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
        futures = {executor.submit(check_port, ip, p): p for p in all_ports}
        for future in concurrent.futures.as_completed(futures):
            p = future.result()
            if p:
                open_ports.append(p)
                print(f"[FOUND OPEN PORT] IP: {ip} | Port: {p}")

    print(f"Scan finished. Open ports: {sorted(open_ports)}")
