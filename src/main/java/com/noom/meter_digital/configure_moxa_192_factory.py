import urllib.request
import urllib.parse
import time

ip = "192.168.127.254"

print("==================================================")
print("CONFIGURING FACTORY MOXA AT 192.168.127.254")
print("==================================================")

# 1. Set Serial Port 1 to RS-485 2-Wire (IfType=2)
ser_params = {
    'PortNo': '01',
    'BaudRate': '10', # 9600
    'DataBits': '3',  # 8
    'StopBits': '0',  # 1
    'Parity': '0',    # None
    'FlowCtrl': '1',  # None
    'FIFO': '0',
    'IfType': '2'     # RS-485 2-Wire
}
url_ser = f"http://{ip}/Set.htm?{urllib.parse.urlencode(ser_params)}"
print(f"1. Setting Serial Interface RS-485 2-Wire: {url_ser}")
try:
    req = urllib.request.Request(url_ser, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=3) as resp:
        print("   Serial Interface updated successfully!")
except Exception as e:
    print(f"   Serial config error: {e}")

# 2. Set Operation Mode Port 1 to TCP Server Mode on Local TCP Port 4001 (and 4045)
op_params = {
    'PortNo': '01',
    'OpMode': '10',         # TCP Server Mode
    'LocalTcpPort': '4001', # Port 4001
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
url_op = f"http://{ip}/Set.htm?{urllib.parse.urlencode(op_params)}"
print(f"2. Setting Operation Mode TCP Server Mode (Port 4001): {url_op}")
try:
    req = urllib.request.Request(url_op, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=3) as resp:
        print("   Operation Mode updated successfully!")
except Exception as e:
    print(f"   OpMode config error: {e}")

# 3. Save & Restart Moxa
url_save = f"http://{ip}/09Set.htm"
print(f"3. Submitting Save & Restart...")
try:
    req_save = urllib.request.Request(url_save, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req_save, timeout=3) as resp:
        print("   Moxa Saved & Restarted successfully!")
except Exception as e:
    print(f"   Save/Restart error: {e}")

print("==================================================")
