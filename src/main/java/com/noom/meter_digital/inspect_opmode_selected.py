import urllib.request
import re

ip = "169.254.51.66"

for port in ["01", "02"]:
    url = f"http://{ip}/04Svr.htm?Port={port}"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=3) as resp:
        html = resp.read().decode('latin1', errors='ignore')
        print(f"\n==================== PORT {port} OP MODE ====================")
        op_modes = ["Real COM Mode", "TCP Server Mode", "TCP Client Mode", "UDP Mode", "Pair Connection Slave", "Pair Connection Master", "Reverse Telnet Mode", "Disabled"]
        
        # Check sS arguments: sS(opmode_idx, ...)
        m = re.search(r'sS\(["\'](\d+)["\']', html)
        if m:
            idx = int(m.group(1))
            mode_str = op_modes[idx] if idx < len(op_modes) else f"Unknown ({idx})"
            print(f"  Selected OpMode Index: {idx} -> {mode_str}")
        else:
            print("  Could not find sS function call")
