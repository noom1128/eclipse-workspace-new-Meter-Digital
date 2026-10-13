import urllib.request
import re

ip = "169.254.51.66"
urls = [
    f"http://{ip}/03RS485d.htm?Port=01",
    f"http://{ip}/04Svr.htm?Port=01",
    f"http://{ip}/04Real.htm?Port=01"
]

for url in urls:
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            html = resp.read().decode('latin1', errors='ignore')
            print(f"\n==================== URL: {url} ====================")
            clean_text = re.sub(r'<script.*?</script>', '', html, flags=re.DOTALL)
            clean_text = re.sub(r'<[^>]+>', ' ', clean_text)
            clean_text = ' '.join(clean_text.split())
            print(clean_text[:1500])
    except Exception as e:
        print(f"Error fetching {url}: {e}")
