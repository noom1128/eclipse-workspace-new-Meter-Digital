import urllib.request
import re

url = "http://169.254.51.66/04Svr.htm?Port=01"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=3) as resp:
    html = resp.read().decode('latin1', errors='ignore')
    print("Form tag:")
    forms = re.findall(r'<form[^>]+>', html, re.IGNORECASE)
    for f in forms:
        print(" ", f)
        
    print("\nScript functions:")
    scripts = re.findall(r'<script.*?>(.*?)</script>', html, re.DOTALL | re.IGNORECASE)
    for s in scripts:
        print(s[:1000])
