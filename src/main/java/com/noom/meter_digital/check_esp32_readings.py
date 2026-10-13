import urllib.request
import json

url = "http://localhost:8080/api/meter"
try:
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=3) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        print("==================================================")
        print("LATEST METER READINGS FROM SPRING BOOT DB:")
        print("==================================================")
        for r in data[:5]:
            print(f"Room: {r.get('roomNumber')}, MeterID: {r.get('meterId')}, Voltage: {r.get('voltage')}V, Current: {r.get('current')}A, Power: {r.get('power')}kW, Energy: {r.get('unit')}kWh, Time: {r.get('timestamp')}")
except Exception as e:
    print(f"Could not connect to localhost:8080/api/meter: {e}")
