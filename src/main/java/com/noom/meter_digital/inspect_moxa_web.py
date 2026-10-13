import urllib.request
import re

ip = "169.254.51.66"
pages = ["contents.htm", "ser_set.htm", "op_mode.htm", "basic.htm", "net_set.htm"]

for page in pages:
    url = f"http://{ip}/{page}"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            html = resp.read().decode('latin1', errors='ignore')
            print(f"\n==================== PAGE: {page} ====================")
            # Remove scripts and tags for readable text
            text = re.sub(r'<script.*?</script>', '', html, flags=re.DOTALL)
            # Extract form values or selects
            inputs = re.findall(r'<input[^>]+>|<select[^>]+>.*?</select>', text, flags=re.DOTALL | re.IGNORECASE)
            clean_text = re.sub(r'<[^>]+>', ' ', text)
            clean_text = ' '.join(clean_text.split())
            print(clean_text[:1000])
            if inputs:
                print("\nForm Elements:")
                for inp in inputs[:15]:
                    print("  ", ' '.join(inp.split()))
    except Exception as e:
        print(f"Error fetching {page}: {e}")
