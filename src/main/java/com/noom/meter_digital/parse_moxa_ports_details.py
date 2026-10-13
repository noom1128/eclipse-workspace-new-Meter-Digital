import urllib.request
import re

ip = "169.254.51.66"

for port_num in ["01", "02"]:
    url = f"http://{ip}/04Svr.htm?Port={port_num}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            html = resp.read().decode('latin1', errors='ignore')
            print(f"\n==================== PORT {port_num} CONFIG ====================")
            
            # Find window_onload or set values
            onloads = re.findall(r'function window_onload\(\)\s*\{(.*?)\}', html, re.DOTALL)
            if onloads:
                print("Onload Initializer JavaScript:")
                print(" ", onloads[0].strip())
                
            inputs = re.findall(r'<input[^>]+name=["\']?([^"\'>\s]+)["\']?[^>]*value=["\']?([^"\'>\s]*)["\']?', html, re.IGNORECASE)
            print("Form Inputs:")
            for name, val in inputs:
                if any(k in name.lower() for k in ['port', 'mode', 'tcp', 'ip', 'op', 'connect']):
                    print(f"  {name} = {val}")
                    
    except Exception as e:
        print(f"Error fetching Port {port_num}: {e}")
