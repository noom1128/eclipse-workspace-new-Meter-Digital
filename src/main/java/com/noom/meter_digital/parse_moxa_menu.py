import urllib.request
import re

url = "http://169.254.51.66/contents.htm"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=3) as resp:
    html = resp.read().decode('latin1', errors='ignore')
    links = re.findall(r'\"([^\"]+\.htm[l]?)\"', html)
    print("Found frame links:", set(links))
    
    # Print lines containing Folder or generateTree
    for line in html.splitlines():
        if "Folder" in line or "ins" in line or "htm" in line:
            print(line.strip())
