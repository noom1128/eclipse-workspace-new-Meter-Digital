import urllib.request
import re

ip = "169.254.51.66"
urls = [
    f"http://{ip}/03.htm?Port=01",
    f"http://{ip}/04.htm?Port=01",
    f"http://{ip}/Mn_asynS.htm"
]

for url in urls:
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            html = resp.read().decode('latin1', errors='ignore')
            print(f"\n==================== URL: {url} ====================")
            # Find all select options and checked inputs
            selects = re.findall(r'<select[^>]+name=["\']?([^"\'>]+)["\']?[^>]*>(.*?)</select>', html, re.DOTALL | re.IGNORECASE)
            for sname, scontent in selects:
                selected = re.findall(r'<option[^>]*selected[^>]*>(.*?)</option>', scontent, re.IGNORECASE)
                print(f"  Select [{sname}]: Selected = {selected}")
            
            clean_text = re.sub(r'<script.*?</script>', '', html, flags=re.DOTALL)
            clean_text = re.sub(r'<[^>]+>', ' ', clean_text)
            clean_text = ' '.join(clean_text.split())
            print(clean_text[:1200])
    except Exception as e:
        print(f"Error fetching {url}: {e}")
