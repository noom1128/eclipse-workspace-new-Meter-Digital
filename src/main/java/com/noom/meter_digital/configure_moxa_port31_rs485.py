import urllib.request
import urllib.parse

ip = "169.254.51.66"
port_no = "31"

print(f"Configuring Moxa Port {port_no} for RS-485 2-Wire & TCP Server Mode...")

# 1. Set Serial Interface to RS-485 2-Wire (IfType=2) and 9600 8N1
serial_params = {
    'PortNo': port_no,
    'BaudRate': '10', # 9600
    'DataBits': '3',  # 8
    'StopBits': '0',  # 1
    'Parity': '0',    # None
    'FlowCtrl': '1',  # None
    'FIFO': '0',      # Enable
    'IfType': '2'     # RS-485 2-Wire!
}

url_ser = f"http://{ip}/Set.htm?{urllib.parse.urlencode(serial_params)}"
print(f"Sending Serial config: {url_ser}")
try:
    req = urllib.request.Request(url_ser, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=5) as resp:
        print("Serial interface updated to RS-485 2-Wire!")
except Exception as e:
    print(f"Serial config error: {e}")

# 2. Set Operation Mode to TCP Server Mode (OpMode=10), Local TCP Port = 4031
op_params = {
    'PortNo': port_no,
    'OpMode': '10',         # TCP Server Mode
    'LocalTcpPort': '4031', # Local TCP Port 4031
    'CommandPort': '981',
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

url_op = f"http://{ip}/Set.htm?{urllib.parse.urlencode(op_params)}"
print(f"Sending OpMode config: {url_op}")
try:
    req = urllib.request.Request(url_op, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=5) as resp:
        print("OpMode updated to TCP Server Mode on Port 4031!")
except Exception as e:
    print(f"OpMode config error: {e}")

# 3. Save & Restart Moxa
try:
    req_save = urllib.request.Request(f"http://{ip}/09Set.htm", headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req_save, timeout=5) as resp:
        print("Moxa Saved & Rebooted successfully!")
except Exception as e:
    print(f"Save/Restart error: {e}")
