import urllib.request
import urllib.parse
import sys

ip = "169.254.51.66"

def set_moxa_parity(parity_val): # 0 = None, 1 = Odd, 2 = Even
    params = {
        'PortNo': '01',
        'BaudRate': '10', # 9600
        'DataBits': '3',  # 8
        'StopBits': '0',  # 1
        'Parity': str(parity_val),
        'FlowCtrl': '1',  # None
        'FIFO': '0',      # Enable
        'IfType': '2'     # RS-485 2-Wire
    }

    url = f"http://{ip}/Set.htm?{urllib.parse.urlencode(params)}"
    print(f"Setting Moxa Parity={parity_val} ({'None' if parity_val==0 else 'Even'}): {url}")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            pass

        # Save/Restart
        req_save = urllib.request.Request(f"http://{ip}/09Set.htm", headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req_save, timeout=3) as resp_save:
            pass
        print("Moxa updated & restarted successfully!")
    except Exception as e:
        print(f"Error setting Moxa serial: {e}")

if __name__ == "__main__":
    parity = int(sys.argv[1]) if len(sys.argv) > 1 else 2
    set_moxa_parity(parity)
