import urllib.request
import urllib.parse
import sys

ip = "169.254.51.66"
port_no = "01"
local_tcp_port = "4045"

# OpMode 1 = TCP Server Mode
# OpMode 0 = Real COM Mode
params = {
    'PortNo': port_no,
    'OpMode': '1',  # 1 = TCP Server Mode
    'LocalTcpPort': local_tcp_port,
    'CommandPort': '966',
    'MaxConnect': '1',
    'TCPAliveCheck': '7',
    'InactivityTime': '0',
    'packlen': '0',
    'DelimiterChar1': '0',
    'DelimiterChar2': '0',
    'delimprocess': '1',
    'ForceTxTime': '0',
    'ignorejam': '0',
    'allowdrvctrl': '0'
}

query_string = urllib.parse.urlencode(params)
set_url = f"http://{ip}/Set.htm?{query_string}"

print(f"Sending configuration to Moxa: {set_url}")
try:
    req = urllib.request.Request(set_url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=5) as resp:
        res_text = resp.read().decode('latin1', errors='ignore')
        print("Set Response Success!")
        print(res_text[:500])

    # Save & Restart Moxa
    restart_url = f"http://{ip}/09Save.htm"
    print(f"\nSending Save/Restart command to Moxa...")
    req_save = urllib.request.Request(restart_url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req_save, timeout=5) as resp_save:
        print("Save/Restart submitted successfully!")
        
except Exception as e:
    print(f"Error submitting configuration: {e}")
