import socket
import concurrent.futures
import ipaddress
import sys

def check_moxa_ip(ip):
    for port in [80, 23, 4001, 502, 4045, 4032]:
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(0.2)
            res = sock.connect_ex((str(ip), port))
            sock.close()
            if res == 0:
                return str(ip), port
        except Exception:
            pass
    return None

if __name__ == "__main__":
    print("Scanning for Moxa after factory reset...")
    
    # 1. Test 192.168.127.0/24
    subnets = ["192.168.127.0/24"]
    
    # 2. Add ARP cache check
    found_ips = []
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
        futures = []
        for sub in subnets:
            net = ipaddress.ip_network(sub, strict=False)
            for ip in net.hosts():
                futures.append(executor.submit(check_moxa_ip, ip))
                
        for future in concurrent.futures.as_completed(futures):
            res = future.result()
            if res:
                found_ips.append(res)
                print(f"[FOUND MOXA!] IP: {res[0]} | Open Port: {res[1]}")

    print(f"Scan completed. Discovered: {found_ips}")
