import urllib.request
import urllib.parse
import socket
import sys

def configure_moxa_ip():
    print("==================================================")
    print("RECONFIGURING MOXA IP TO 169.254.51.66")
    print("==================================================")

    ip_params = {
        'IP': '169.254.51.66',
        'Netmask': '255.255.0.0',
        'Gateway': '0.0.0.0',
        'IPConfig': '0' # Static IP
    }

    url = f"http://192.168.127.254/Set.htm?{urllib.parse.urlencode(ip_params)}"
    print(f"Sending IP configuration to Moxa: {url}")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            print("IP updated successfully to 169.254.51.66!")

        # Save & Restart
        url_save = "http://192.168.127.254/09Set.htm"
        req_save = urllib.request.Request(url_save, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req_save, timeout=3) as resp:
            print("Save & Restart command sent successfully!")
    except Exception as e:
        print(f"HTTP Set IP result: {e}")

if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass
    configure_moxa_ip()
