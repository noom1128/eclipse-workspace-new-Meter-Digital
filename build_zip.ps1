$baseDir = "D:\Nopporn\eclipse-workspace-new\Meter-Digital"
$stageDir = Join-Path $baseDir "dist\staging\MeterDigitalPro"
$zipPath = Join-Path $baseDir "dist\MeterDigitalPro-Setup.zip"

if (Test-Path $stageDir) { Remove-Item $stageDir -Recurse -Force }
New-Item -ItemType Directory -Path $stageDir -Force | Out-Null

# 1. Copy application jar
Copy-Item (Join-Path $baseDir "target\meter-digital-0.0.1-SNAPSHOT.jar") (Join-Path $stageDir "meter-digital.jar") -Force

# 2. Copy launcher exe & batch
Copy-Item (Join-Path $baseDir "Meter-Digital.exe") (Join-Path $stageDir "Meter-Digital.exe") -Force
Copy-Item (Join-Path $baseDir "run-meter-digital.bat") (Join-Path $stageDir "run-meter-digital.bat") -Force
Copy-Item (Join-Path $baseDir "create-desktop-shortcut.bat") (Join-Path $stageDir "create-desktop-shortcut.bat") -Force

# 3. Copy icon
Copy-Item (Join-Path $baseDir "meter-pro.ico") (Join-Path $stageDir "meter-pro.ico") -Force

# 4. Copy initial database
if (Test-Path (Join-Path $baseDir "dorm.db")) {
    Copy-Item (Join-Path $baseDir "dorm.db") (Join-Path $stageDir "dorm.db") -Force
}

# 5. Create user manual
$readme = @"
====================================================================
     METER DIGITAL PRO - ระบบบริหารจัดการอ่านมิเตอร์ไฟฟ้าดิจิทัล
====================================================================

ยินดีต้อนรับสู่ระบบ Meter Digital Pro (ระบบจดมิเตอร์และคำนวณบิลค่าไฟอัจฉริยะ)

[ขั้นตอนการติดตั้งและเริ่มใช้งานง่ายๆ]
1. แตกไฟล์ (Extract All) ทั้งหมดไว้ที่โฟลเดอร์ตามต้องการ 
   (แนะนำวางไว้ที่หน้า Desktop หรือโฟลเดอร์ C:\MeterDigital)
2. (แนะนำ) ดับเบิลคลิกไฟล์ "create-desktop-shortcut.bat" 1 ครั้ง
   -> เพื่อสร้างไอคอนทางลัดรูปมิเตอร์สวยงามไว้บนหน้าจอ Desktop ของคุณทันที!
3. เริ่มเปิดระบบได้ 2 วิธี:
   - ดับเบิลคลิกที่ไอคอน "Meter Digital Pro" บนหน้าจอ Desktop
   - หรือดับเบิลคลิกที่ไฟล์ "Meter-Digital.exe" ในโฟลเดอร์นี้
4. เมื่อเปิดแล้ว หน้าจอสีดำจะแสดงสถานะระบบ ให้เปิดเว็บเบราว์เซอร์ไปที่:
   👉 เข้าสู่ระบบ: https://localhost:8443
   👉 สแกนอ่านมิเตอร์ผ่านมือถือ: https://localhost:8443/meter-reader
   👉 พิมพ์สติกเกอร์ QR Code ติดห้อง: https://localhost:8443/meter-stickers

[ข้อมูลการเข้าสู่ระบบเริ่มต้น]
- Username: admin
- Password:  admin123

* หมายเหตุ: ต้องติดตั้ง Java Runtime (Java 17 ขึ้นไป) ในเครื่องก่อนเริ่มใช้งาน
  หากยังไม่มี สามารถดาวน์โหลด Java 17 ได้ฟรีที่:
  https://adoptium.net/temurin/releases/?version=17
"@

[System.IO.File]::WriteAllText((Join-Path $stageDir "README-วิธีใช้งาน.txt"), $readme, [System.Text.Encoding]::UTF8)

# 6. Create Zip archive
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Write-Output "Creating ZIP archive at: $zipPath..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($stageDir, $zipPath, [System.IO.Compression.CompressionLevel]::Optimal, $false)

# Clean up staging
Remove-Item (Join-Path $baseDir "dist\staging") -Recurse -Force

$zipItem = Get-Item $zipPath
Write-Output "Successfully built ZIP file!"
Write-Output "Path: $($zipItem.FullName)"
Write-Output "Size: $([math]::Round($zipItem.Length / 1MB, 2)) MB"
