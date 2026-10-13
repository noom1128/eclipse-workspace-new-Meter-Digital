import urllib.request
import re

url = "http://169.254.51.66/04Svr.htm?Port=01"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=3) as resp:
    html = resp.read().decode('latin1', errors='ignore')
    print("Form action and elements:")
    form_html = re.findall(r'<form.*?</form>', html, re.DOTALL | re.IGNORECASE)
    for f in form_html:
        print(f[:1500])
