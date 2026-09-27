# ทดสอบมิเตอร์จำลอง 5 ห้องด้วย Postman

เมื่อเริ่มโปรแกรม ระบบจะมีห้อง `101` ถึง `105` และสร้างผู้ใช้ `room101` ถึง `room105` (รหัสผ่าน `1234`) หากยังไม่มีในฐานข้อมูล

สำหรับฐานข้อมูลใหม่ บัญชีผู้ดูแลคือ `admin` / `1234`.

## 1. ส่งค่ามิเตอร์

สร้าง request แบบ `POST` ไปที่ `http://localhost:8080/api/meter` และตั้ง Header `Content-Type: application/json` แล้วส่ง Body แบบ raw JSON ตัวอย่างนี้สำหรับห้อง 101:

```json
{
  "roomNumber": "101",
  "meterId": "METER-101",
  "unit": 1245.60,
  "voltage": 228.4,
  "current": 1.32,
  "power": 286.1,
  "energy": 1245.60
}
```

เปลี่ยน `roomNumber`, `meterId`, และ `unit` เพื่อส่งให้ครบทุกห้อง:

| ห้อง | meterId | unit ตัวอย่าง |
| --- | --- | --- |
| 101 | METER-101 | 1245.60 |
| 102 | METER-102 | 978.25 |
| 103 | METER-103 | 1530.10 |
| 104 | METER-104 | 806.75 |
| 105 | METER-105 | 1112.40 |

`timestamp` จะถูกกำหนดโดยเซิร์ฟเวอร์ เพื่อป้องกันเวลาจากอุปกรณ์คลาดเคลื่อน และไม่จำเป็นต้องส่ง `room` แบบ nested object.

## 2. ดูผลในหน้า Admin

1. เข้าสู่ `http://localhost:8080/login` ด้วย `admin` / `1234` (เฉพาะเมื่อเป็นฐานข้อมูลใหม่)
2. หน้า dashboard จะแสดงการ์ด “ค่ามิเตอร์ล่าสุดแยกตามห้อง” ทั้ง 5 ห้อง และรีเฟรชทุก 5 วินาที

## 3. เรียก API สรุปโดยตรง

ล็อกอินก่อนโดย `POST http://localhost:8080/api/auth/login`:

```json
{ "username": "admin", "password": "1234" }
```

คัดลอก `token` ที่ตอบกลับไปใส่ Header `Authorization: Bearer <token>` แล้วเรียก:

```
GET http://localhost:8080/api/meter/latest-by-room
```

API นี้ให้ Admin เท่านั้น ส่วน `POST /api/meter` เปิดให้ ESP32/Postman ส่งข้อมูลได้โดยไม่ต้องใช้ JWT ในการสาธิต. ก่อนใช้งานจริงควรเพิ่ม API key ให้กับอุปกรณ์แต่ละตัว.
