import urllib.request
import re

ip = "169.254.51.66"
pages = [
    "main.htm",
    "01.htm",
    "02.htm",
    "05.htm",
    "Mn_line.htm",
    "Mn_asyn.htm",
    "Mn_asynS.htm"
]

print("==================================================")
print("FULL MOXA GATEWAY CONFIGURATION AUDIT")
print("==================================================")

for page in pages:
    url = f"http://{ip}/{page}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            html = resp.read().decode('latin1', errors='ignore')
            print(f"\n--- PAGE: {page} ---")
            clean_text = re.sub(r'<script.*?</script>', '', html, flags=re.DOTALL)
            clean_text = re.sub(r'<[^>]+>', ' ', clean_text)
            clean_text = ' '.join(clean_text.split())
            print(clean_text[:1200])
    except Exception as e:
        print(f"Error fetching {page}: {e}")
