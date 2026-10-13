import urllib.request

ip = "169.254.51.66"
url = f"http://{ip}/09Set.htm"

try:
    print(f"Submitting Save/Restart to {url}...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=5) as resp:
        html = resp.read().decode('latin1', errors='ignore')
        print("Save/Restart Triggered!")
        print(html[:500])
except Exception as e:
    print(f"Error submitting 09Set.htm: {e}")
