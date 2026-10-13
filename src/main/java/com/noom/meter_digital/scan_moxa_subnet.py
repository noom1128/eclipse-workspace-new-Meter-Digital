import socket
import concurrent.futures
import ipaddress
import sys

def check_port(ip, port, timeout=0.3):
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        result = sock.connect_ex((str(ip), port))
        sock.close()
        if result == 0:
            return str(ip), port
    except Exception:
        pass
    return None

def scan_network(subnet_str, ports=[4001, 502, 80, 443]):
    try:
        network = ipaddress.ip_network(subnet_str, strict=False)
    except Exception as e:
        print(f"Invalid subnet: {subnet_str} ({e})")
        return []

    print(f"Scanning subnet {network} for open Modbus ports {ports}...")
    found = []
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
        futures = []
        for ip in network.hosts():
            for port in ports:
                futures.append(executor.submit(check_port, ip, port))
        
        for future in concurrent.futures.as_completed(futures):
            res = future.result()
            if res:
                found.append(res)
                print(f"[DISCOVERED DEVICE] IP: {res[0]} | Open Port: {res[1]}")
                
    return found

if __name__ == "__main__":
    subnets = ["10.221.143.0/24", "172.21.49.0/24", "192.168.127.0/24", "192.168.1.0/24"]
    if len(sys.argv) > 1:
        subnets = [sys.argv[1]]
        
    for sub in subnets:
        scan_network(sub)
