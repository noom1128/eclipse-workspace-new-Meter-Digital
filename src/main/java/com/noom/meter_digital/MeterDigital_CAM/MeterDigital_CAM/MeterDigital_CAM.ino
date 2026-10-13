#include "esp_camera.h"
#include <WiFi.h>
#include <HTTPClient.h>
#include <WebServer.h>
#include <DNSServer.h>
#include <Preferences.h>

// =====================================================
// BOARD SELECTION: AI-THINKER ESP32-CAM (4MB Flash)
// =====================================================
#define BOARD_AI_THINKER      // บอร์ด AI-Thinker ESP32-CAM (4MB Flash)
//#define BOARD_ESP32S3_CAM   // บอร์ด ESP32-S3 CAM

#if defined(BOARD_AI_THINKER)
// Pinout สำหรับ AI-Thinker ESP32-CAM (4MB Flash)
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM      0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27

#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM        5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22

#define FLASH_LED_PIN      4 // ไฟ Flash บนบอร์ด ESP32-CAM (GPIO 4)

#elif defined(BOARD_ESP32S3_CAM)
// Pinout สำหรับ ESP32-S3 CAM
#define PWDN_GPIO_NUM     -1
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM     15
#define SIOD_GPIO_NUM      4
#define SIOC_GPIO_NUM      5

#define Y9_GPIO_NUM       16
#define Y8_GPIO_NUM       17
#define Y7_GPIO_NUM       18
#define Y6_GPIO_NUM       12
#define Y5_GPIO_NUM       10
#define Y4_GPIO_NUM        8
#define Y3_GPIO_NUM        9
#define Y2_GPIO_NUM       11
#define VSYNC_GPIO_NUM     6
#define HREF_GPIO_NUM      7
#define PCLK_GPIO_NUM     13

#define FLASH_LED_PIN     -1
#endif

// =====================================================
// NVS Flash Config & Default Values
// =====================================================
Preferences prefs;
DNSServer dnsServer;
const byte DNS_PORT = 53;
const char* AP_SSID = "ESP32-CAM-Meter-Setup";
const char* AP_PASSWORD = "12345678";
const IPAddress AP_IP(192, 168, 4, 1);

// Dynamic Config Variables (Default Wi-Fi Hotspot for immediate connection)
String wifiSsid = "Galaxy A15 5G 4A21";
String wifiPassword = "0859631128";
String customServerIP = "10.21.72.108";
int customServerPort = 8080;

// *** Logic การผูกห้อง & โหมดการทำงาน ***
String customRoomNumber = "101";
String customDeviceId = "ESP32-CAM-ROOM101";
String targetMode = "DIRECT_OCR"; // "DIRECT_OCR" หรือ "LINE_FLEX_MESSAGE"

// โหมดการทำงาน: "MANUAL" (กดถ่ายเองช่วงทดสอบ) หรือ "AUTO" (อ่านอัตโนมัติเมื่อติดตั้งจริง)
String workMode = "MANUAL";
int captureIntervalMinutes = 5; // ถ่ายรูปอัตโนมัติทุกๆ กี่นาที (เมื่ออยู่โหมด AUTO)

// การปรับทิศทางภาพกล้อง (แก้ปัญหากล้องกลับหัว / สลับซ้ายขวา)
bool isVflip = false;   // พลิกบน-ล่าง
bool isHmirror = false; // พลิกซ้าย-ขวา

// สถานะการถ่ายรูปและไฟแฟลช
unsigned long lastCaptureTime = 0;
bool flashState = false; // ปิดแฟลชโดยเริ่มต้นเพื่อป้องกันแสงสะท้อนฝากระจกมิเตอร์

String lastCaptureStatus = "";
String lastCaptureReading = "";
String lastCaptureMsg = "";

WebServer server(80);
bool isApMode = false;
bool isCameraReady = false;
String logsBuffer = "";

// Function Declarations
void addLog(const String& message);
void loadConfigFromNVS();
void saveConfigToNVS();
bool initCamera();
void applyCameraOrientation();
bool captureAndSendPhoto();
void startApCaptivePortal();
void connectWiFi();
void handleJpgStream();

void applyCameraOrientation() {
  sensor_t * s = esp_camera_sensor_get();
  if (s) {
    s->set_vflip(s, isVflip ? 1 : 0);
    s->set_hmirror(s, isHmirror ? 1 : 0);
    addLog("Camera orientation applied: vflip=" + String(isVflip ? "1" : "0") + " hmirror=" + String(isHmirror ? "1" : "0"));
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  if (FLASH_LED_PIN >= 0) {
    pinMode(FLASH_LED_PIN, OUTPUT);
    digitalWrite(FLASH_LED_PIN, LOW); // ตรวจสอบให้แน่ใจว่าปิดแฟลชไว้เสมอ
  }

  addLog("======================================");
  addLog("ESP32-CAM Retrofit Meter Reader Starting");
  addLog("======================================");

  loadConfigFromNVS();

  // รีเซ็ตการทำงาน Wi-Fi ให้สะอาด
  WiFi.persistent(false);
  WiFi.disconnect(true);
  delay(100);

  // 1. เริ่มระบบ Wi-Fi เป็นอันดับแรกเสมอ
  connectWiFi();

  // 2. เริ่มต้นโมดูลกล้อง
  isCameraReady = initCamera();
  if (!isCameraReady) {
    addLog("WARNING: Camera failed to initialize, but Wi-Fi web server is RUNNING.");
  } else {
    addLog("Camera Initialized Successfully.");
    applyCameraOrientation();
  }

  if (!isApMode) {
    // Web Dashboard หลักสำหรับกล้องจดมิเตอร์จานหมุน
    server.on("/", []() {
      String orientationStatus = (isVflip && isHmirror) ? "หมุนกลับหัว 180°" : ((isVflip || isHmirror) ? "ปรับแต่งมุมมอง" : "มุมมองมาตรฐาน");
      String serverUrl = "http://" + customServerIP + ":" + String(customServerPort);

      String html = "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    "<title>📷 ESP32-CAM เล็งและอ่านมิเตอร์จานหมุน</title>"
                    "<style>"
                    "body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;background:#0f172a;color:#f8fafc;padding:16px;display:flex;justify-content:center;margin:0;}"
                    ".card{background:#1e293b;border-radius:20px;padding:22px;max-width:520px;width:100%;box-shadow:0 12px 35px rgba(0,0,0,0.5);box-sizing:border-box;}"
                    "h2{color:#00f2fe;text-align:center;margin-top:0;margin-bottom:6px;font-size:1.4rem;}"
                    ".status-row{display:flex;justify-content:center;gap:8px;margin-bottom:14px;flex-wrap:wrap;}"
                    ".status-badge{display:inline-block;padding:5px 12px;border-radius:20px;font-size:0.8rem;font-weight:bold;}"
                    ".bg-green{background:rgba(16,185,129,0.2);color:#34d399;border:1px solid #10b981;}"
                    ".bg-yellow{background:rgba(245,158,11,0.2);color:#fbbf24;border:1px solid #f59e0b;}"
                    ".bg-cyan{background:rgba(0,242,254,0.15);color:#00f2fe;border:1px solid #00f2fe;}"
                    ".stream-container{position:relative;width:100%;border-radius:14px;overflow:hidden;background:#000;border:2px solid #334155;margin-bottom:12px;aspect-ratio:4/3;display:flex;align-items:center;justify-content:center;}"
                    ".stream-container img{width:100%;height:100%;object-fit:contain;display:block;}"
                    ".target-guide{position:absolute;top:25%;left:15%;right:15%;bottom:25%;border:2px dashed #22c55e;border-radius:8px;box-shadow:0 0 12px rgba(34,197,94,0.5);pointer-events:none;display:flex;align-items:center;justify-content:center;text-align:center;}"
                    ".target-label{background:rgba(15,23,42,0.8);color:#22c55e;font-size:0.75rem;padding:3px 8px;border-radius:4px;font-weight:bold;}"
                    ".stream-overlay{position:absolute;top:10px;left:10px;background:rgba(15,23,42,0.8);backdrop-filter:blur(4px);padding:4px 10px;border-radius:20px;font-size:0.75rem;color:#38bdf8;font-weight:bold;display:flex;align-items:center;gap:6px;}"
                    ".pulse-dot{width:8px;height:8px;border-radius:50%;background:#22c55e;display:inline-block;box-shadow:0 0 8px #22c55e;}"
                    ".mode-btn{padding:12px 10px;border-radius:12px;font-size:0.95rem;font-weight:bold;cursor:pointer;border:2px solid #334155;background:#1e293b;color:#94a3b8;transition:all 0.2s;text-align:center;box-sizing:border-box;}"
                    ".mode-btn.active{border-color:#00f2fe;background:rgba(0,242,254,0.15);color:#00f2fe;box-shadow:0 0 15px rgba(0,242,254,0.3);}"
                    ".btn{display:block;width:100%;padding:14px;border-radius:12px;border:none;font-size:1rem;font-weight:bold;cursor:pointer;margin-bottom:10px;text-align:center;text-decoration:none;box-sizing:border-box;transition:all 0.2s;}"
                    ".btn:active{transform:scale(0.98);}"
                    ".btn-primary{background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;box-shadow:0 4px 15px rgba(0,242,254,0.3);}"
                    ".btn-rotate{background:linear-gradient(135deg,#8b5cf6,#6366f1);color:#fff;box-shadow:0 4px 15px rgba(99,102,241,0.3);}"
                    ".btn-secondary{background:#334155;color:#f8fafc;}"
                    ".btn-warning{background:linear-gradient(135deg,#f59e0b,#d97706);color:#fff;}"
                    ".btn-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:10px;}"
                    ".info-box{background:#0f172a;border:1px solid #334155;border-radius:12px;padding:12px 14px;margin-bottom:14px;font-size:0.85rem;text-align:left;line-height:1.5;}"
                    ".result-box{background:rgba(0,242,254,0.08);border:1px solid #00f2fe;border-radius:12px;padding:14px;margin-bottom:14px;text-align:center;}"
                    ".tip-box{background:rgba(245,158,11,0.1);border:1px solid rgba(245,158,11,0.3);border-radius:10px;padding:10px;margin-bottom:12px;font-size:0.8rem;color:#fcd34d;line-height:1.4;text-align:left;}"
                    "</style></head><body><div class='card'>"
                    "<h2>📷 ESP32-CAM ส่องอ่านมิเตอร์</h2>"
                    "<div class='status-row'>"
                    "<span class='status-badge " + String(workMode == "MANUAL" ? "bg-yellow'>🧪 โหมดทดสอบ (Manual)" : "bg-green'>🚀 โหมดใช้งานจริง (Auto " + String(captureIntervalMinutes) + " นาที)") + "</span>"
                    "<span class='status-badge bg-cyan'>📍 ห้อง " + customRoomNumber + "</span>"
                    "<span class='status-badge bg-yellow'>📐 " + orientationStatus + "</span>"
                    "</div>"
                    
                    "<div style='display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:12px;'>"
                    "<button id='btnLive' type='button' class='mode-btn' onclick='setMode(true)'>🟢 LIVE VIEW (สตรีมสด)</button>"
                    "<button id='btnSnap' type='button' class='mode-btn active' onclick='setMode(false)'>📸 SNAP (ดูทีละภาพ)</button>"
                    "</div>"

                    "<div class='stream-container'>"
                    "<img id='liveImg' src='/stream' alt='Live Viewfinder'>"
                    "<div class='target-guide'><div class='target-label'>🎯 เล็งตัวเลขให้อยู่ในกรอบนี้</div></div>"
                    "<div class='stream-overlay' id='overlayBadge'>📷 SNAP VIEW (ภาพนิ่งล่าสุด)</div>"
                    "</div>"

                    "<div id='snapControls' style='margin-bottom:12px;'>"
                    "<button type='button' class='btn' style='background:linear-gradient(135deg,#0284c7,#2563eb);color:#fff;margin-bottom:0;' onclick='triggerSnap()'>📸 กดถ่ายภาพ SNAP ใหม่ (ดูมุมเล็ง)</button>"
                    "</div>"

                    "<div class='tip-box'>"
                    "💡 <b>โหมดการดูภาพ:</b><br>"
                    "• <b>SNAP (แนะนำ):</b> โหลดทีละภาพ รวดเร็ว ไม่กินเน็ต ไม่กระตุก<br>"
                    "• <b>LIVE VIEW:</b> ภาพสดต่อเนื่องสำหรับขยับปรับมุมกล้อง<br>"
                    "• <b>การบันทึก:</b> ทิศทางหมุนกล้อง 180° บันทึกลงชิปถาวรแล้ว ปิดเครื่องก็ไม่หายครับ"
                    "</div>";

      if (lastCaptureStatus != "") {
        html += "<div class='result-box'>"
                "<div style='font-size:0.85rem;color:#94a3b8;'>ผลการอ่านภาพล่าสุด:</div>"
                "<div style='font-size:1.4rem;font-weight:bold;color:#00f2fe;margin:4px 0;'>" + (lastCaptureReading != "" ? lastCaptureReading + " kWh" : "ประมวลผลสำเร็จ") + "</div>"
                "<div style='font-size:0.8rem;color:#cbd5e1;'>" + lastCaptureMsg + "</div>"
                "<div style='margin-top:8px;'><a href='" + serverUrl + "/api/meter/image/last' target='_blank' style='color:#38bdf8;font-size:0.85rem;text-decoration:underline;'>🖼️ คลิกเปิดดูภาพล่าสุดที่เซิร์ฟเวอร์บันทึกไว้</a></div>"
                "</div>";
      }

      html += "<a href='/capture' class='btn btn-primary'>📸 ถ่ายรูปสแกนส่ง Spring Boot เดี๋ยวนี้</a>"
              "<a href='/toggle-rotate' class='btn btn-rotate'>🔄 หมุนภาพ 180° (แก้กล้องกลับหัว)</a>"
              
              "<div class='btn-grid'>"
              "<a href='/toggle-flash' class='btn " + String(flashState ? "btn-warning" : "btn-secondary") + "'>💡 " + String(flashState ? "ปิดไฟ Flash" : "เปิดไฟ Flash") + "</a>"
              "<a href='/toggle-mode' class='btn btn-secondary'>⚙️ สลับโหมด (" + workMode + ")</a>"
              "</div>"

              "<div class='btn-grid'>"
              "<a href='/toggle-vflip' class='btn btn-secondary' style='font-size:0.85rem;'>↕️ พลิกบน-ล่าง (" + String(isVflip ? "ON" : "OFF") + ")</a>"
              "<a href='/toggle-hmirror' class='btn btn-secondary' style='font-size:0.85rem;'>↔️ พลิกซ้าย-ขวา (" + String(isHmirror ? "ON" : "OFF") + ")</a>"
              "</div>"

              "<div class='info-box'>"
              "🌐 <b>Spring Boot Server:</b> <a href='" + serverUrl + "' target='_blank' style='color:#00f2fe;text-decoration:none;'>" + serverUrl + "</a><br>"
              "🆔 <b>Device ID:</b> " + customDeviceId + " | ⚙️ <b>โหมด:</b> " + targetMode +
              "</div>"

              "<div style='text-align:center;font-size:0.85rem;'>"
              "<a href='/log' style='color:#94a3b8;margin-right:14px;'>ดู System Log</a> | "
              "<a href='" + serverUrl + "' target='_blank' style='color:#00f2fe;margin-right:14px;'>🌐 เปิด Dashboard หลัก</a> | "
              "<a href='/reset-ap' style='color:#ef4444;'>ตั้งค่า Wi-Fi ใหม่</a>"
              "</div>"

              "<script>"
              "let img = document.getElementById('liveImg');"
              "let overlay = document.getElementById('overlayBadge');"
              "let isLive = false;"
              "let isFetching = false;"
              "let frameCount = 0;"
              "let lastTime = Date.now();"
              "function setMode(live) {"
              "  isLive = live;"
              "  document.getElementById('btnLive').className = live ? 'mode-btn active' : 'mode-btn';"
              "  document.getElementById('btnSnap').className = !live ? 'mode-btn active' : 'mode-btn';"
              "  document.getElementById('snapControls').style.display = live ? 'none' : 'block';"
              "  if (live) {"
              "    overlay.innerHTML = '<span class=\"pulse-dot\"></span> LIVE VIEW (<span id=\"fpsVal\">--</span> fps)';"
              "    fetchNextFrame();"
              "  } else {"
              "    overlay.innerHTML = '📷 SNAP VIEW (ภาพนิ่งล่าสุด)';"
              "    triggerSnap();"
              "  }"
              "}"
              "function triggerSnap() {"
              "  if (isFetching) return;"
              "  isFetching = true;"
              "  let n = new Image();"
              "  n.src = '/stream?t=' + Date.now();"
              "  n.onload = function() { img.src = n.src; isFetching = false; };"
              "  n.onerror = function() { isFetching = false; };"
              "}"
              "function fetchNextFrame() {"
              "  if (!isLive || isFetching) return;"
              "  isFetching = true;"
              "  let n = new Image();"
              "  n.src = '/stream?t=' + Date.now();"
              "  n.onload = function() {"
              "    img.src = n.src;"
              "    isFetching = false;"
              "    frameCount++;"
              "    let now = Date.now();"
              "    if (now - lastTime >= 1500) {"
              "      let fps = (frameCount / ((now - lastTime) / 1000)).toFixed(1);"
              "      let span = document.getElementById('fpsVal');"
              "      if (span) span.innerText = fps;"
              "      frameCount = 0;"
              "      lastTime = now;"
              "    }"
              "    if (isLive) setTimeout(fetchNextFrame, 350);"
              "  };"
              "  n.onerror = function() {"
              "    isFetching = false;"
              "    if (isLive) setTimeout(fetchNextFrame, 1000);"
              "  };"
              "}"
              "triggerSnap();"
              "</script>"

              "</div></body></html>";
      server.send(200, "text/html; charset=UTF-8", html);
    });

    // Endpoint ถ่ายภาพสดสำหรับส่องมุมมองหน้าปัดมิเตอร์ (Real-time Viewfinder)
    server.on("/stream", HTTP_GET, handleJpgStream);

    // Manual Capture Trigger Endpoint
    server.on("/capture", []() {
      if (!isCameraReady) {
        server.send(400, "text/plain", "กล้องยังไม่พร้อมใช้งาน ตรวจสอบการเสียบสายกล้อง หรือการตั้งค่า PSRAM");
        return;
      }
      addLog("--- Manual Capture Requested via Web Dashboard ---");
      bool ok = captureAndSendPhoto();
      String serverUrl = "http://" + customServerIP + ":" + String(customServerPort);
      String msg = ok ? "✅ ถ่ายรูปสแกนส่ง Spring Boot สำเร็จ!" : "❌ ไม่สามารถส่งภาพไปที่ Spring Boot ได้ (เช็กการเชื่อมต่อ Server)";
      
      String html = "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    "<title>📸 ผลการถ่ายภาพมิเตอร์</title>"
                    "<style>"
                    "body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;background:#0f172a;color:#fff;padding:20px;text-align:center;display:flex;justify-content:center;margin:0;}"
                    ".card{background:#1e293b;border-radius:20px;padding:24px;max-width:480px;width:100%;box-shadow:0 10px 30px rgba(0,0,0,0.5);}"
                    ".reading{font-size:2rem;font-weight:bold;color:#00f2fe;margin:12px 0;}"
                    ".btn{display:block;width:100%;padding:14px;border-radius:12px;border:none;font-size:1rem;font-weight:bold;cursor:pointer;margin-bottom:10px;text-decoration:none;box-sizing:border-box;}"
                    ".btn-primary{background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;}"
                    ".btn-secondary{background:#334155;color:#fff;}"
                    "img.preview{width:100%;max-height:260px;object-fit:contain;border-radius:12px;border:1px solid #334155;margin:12px 0;background:#000;}"
                    "</style></head><body><div class='card'>"
                    "<h2>" + msg + "</h2>";

      if (lastCaptureReading != "") {
        html += "<div class='reading'>⚡ " + lastCaptureReading + " kWh</div>"
                "<p style='color:#94a3b8;'>ห้อง <b>" + customRoomNumber + "</b> | บันทึกลงฐานข้อมูลเรียบร้อย</p>";
      }

      html += "<p style='color:#cbd5e1;font-size:0.9rem;'>🖼️ ภาพที่เซิร์ฟเวอร์ Spring Boot ได้รับและบันทึกไว้:</p>"
              "<img src='" + serverUrl + "/api/meter/image/last?t=" + String(millis()) + "' class='preview' alt='Captured Meter Photo'>"
              "<a href='/' class='btn btn-primary'>⬅️ กลับไปหน้าเล็งมุมกล้อง</a>"
              "<a href='" + serverUrl + "' target='_blank' class='btn btn-secondary'>🌐 เปิดหน้าหลักระบบ Meter Digital</a>"
              "</div></body></html>";

      server.send(200, "text/html; charset=UTF-8", html);
    });

    // ปุ่มลัดหมุนภาพ 180 องศา (แก้ปัญหากล้องกลับหัวในคลิกเดียว)
    server.on("/toggle-rotate", []() {
      isVflip = !isVflip;
      isHmirror = !isHmirror;
      saveConfigToNVS();
      applyCameraOrientation();
      addLog("Camera 180 Rotation Toggled. vflip=" + String(isVflip) + " hmirror=" + String(isHmirror));
      server.sendHeader("Location", "/", true);
      server.send(302, "text/plain", "");
    });

    // ปรับละเอียด: สลับพลิกบน-ล่าง (V-Flip)
    server.on("/toggle-vflip", []() {
      isVflip = !isVflip;
      saveConfigToNVS();
      applyCameraOrientation();
      addLog("Camera V-Flip Toggled: " + String(isVflip));
      server.sendHeader("Location", "/", true);
      server.send(302, "text/plain", "");
    });

    // ปรับละเอียด: สลับพลิกซ้าย-ขวา (H-Mirror)
    server.on("/toggle-hmirror", []() {
      isHmirror = !isHmirror;
      saveConfigToNVS();
      applyCameraOrientation();
      addLog("Camera H-Mirror Toggled: " + String(isHmirror));
      server.sendHeader("Location", "/", true);
      server.send(302, "text/plain", "");
    });

    // สลับเปิด/ปิดไฟ Flash LED ส่องหน้าปัด
    server.on("/toggle-flash", []() {
      flashState = !flashState;
      if (FLASH_LED_PIN >= 0) {
        digitalWrite(FLASH_LED_PIN, flashState ? HIGH : LOW);
      }
      addLog("Flash LED Toggled: " + String(flashState ? "ON" : "OFF"));
      server.sendHeader("Location", "/", true);
      server.send(302, "text/plain", "");
    });

    // สลับโหมดการทำงานระหว่าง MANUAL กับ AUTO
    server.on("/toggle-mode", []() {
      workMode = (workMode == "MANUAL") ? "AUTO" : "MANUAL";
      saveConfigToNVS();
      addLog("Work Mode Switched to: " + workMode);
      server.sendHeader("Location", "/", true);
      server.send(302, "text/plain", "");
    });

    // Log Dashboard Endpoint
    server.on("/log", []() {
      String html = "<html><head><meta http-equiv='refresh' content='5'></head>";
      html += "<body style='background:#1e1e1e;color:#00ff00;font-family:monospace;padding:20px'>";
      html += "<h2>ESP32-CAM System Log</h2>";
      html += "<pre>" + logsBuffer + "</pre></body></html>";
      server.send(200, "text/html", html);
    });

    // Quick Server IP Update Endpoint (e.g. /set-server?ip=10.21.72.108&port=8080)
    server.on("/set-server", []() {
      if (server.hasArg("ip")) customServerIP = server.arg("ip");
      if (server.hasArg("port")) customServerPort = server.arg("port").toInt();
      saveConfigToNVS();
      addLog("Server IP updated to: " + customServerIP + ":" + String(customServerPort));
      server.send(200, "text/plain", "Updated Server IP to: " + customServerIP + ":" + String(customServerPort));
    });

    // Reset Config to AP Mode
    server.on("/reset-ap", []() {
      server.send(200, "text/plain", "Resetting config & restarting AP Captive Portal...");
      delay(1000);
      wifiSsid = "";
      saveConfigToNVS();
      ESP.restart();
    });

    server.begin();
    addLog("Web Server Started on port 80");
  }
}

void loop() {
  if (isApMode) {
    dnsServer.processNextRequest();
    server.handleClient();
    return;
  }

  server.handleClient();

  // หากอยู่ในโหมด AUTO (ใช้งานจริงเมื่อติดตั้งหน้าห้อง): ถ่ายรูปอัตโนมัติเมื่อครบกำหนดเวลา
  if (workMode == "AUTO" && isCameraReady) {
    unsigned long intervalMs = (unsigned long)captureIntervalMinutes * 60000;
    if (millis() - lastCaptureTime >= intervalMs) {
      lastCaptureTime = millis();
      addLog("--- Executing Automatic Scheduled Capture (Mode: AUTO) ---");
      captureAndSendPhoto();
    }
  }
}

// =====================================================
// CAMERA INITIALIZATION WITH SAFE FALLBACK
// =====================================================
bool initCamera() {
  camera_config_t config;
  memset(&config, 0, sizeof(camera_config_t));

  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;

  if (psramFound()) {
    config.frame_size = FRAMESIZE_VGA; // 640x480 เหมาะสแกนตัวเลขอ่านมิเตอร์
    config.jpeg_quality = 10;
    config.fb_count = 2;
    config.grab_mode = CAMERA_GRAB_LATEST;
  } else {
    config.frame_size = FRAMESIZE_CIF; // 400x296
    config.jpeg_quality = 12;
    config.fb_count = 1;
    config.grab_mode = CAMERA_GRAB_WHEN_EMPTY;
  }

  esp_err_t err = esp_camera_init(&config);
  if (err != ESP_OK) {
    addLog("Camera init failed with error 0x" + String(err, HEX));
    return false;
  }

  sensor_t * s = esp_camera_sensor_get();
  if (s) {
    s->set_brightness(s, 1);
    s->set_contrast(s, 1);
  }
  return true;
}

// =====================================================
// HANDLE JPG STREAM / SNAPSHOT FOR LIVE VIEWFINDER
// =====================================================
void handleJpgStream() {
  if (!isCameraReady) {
    server.send(503, "text/plain", "Camera not ready");
    return;
  }

  camera_fb_t * fb = esp_camera_fb_get();
  if (!fb) {
    server.send(500, "text/plain", "Camera capture failed");
    return;
  }

  WiFiClient client = server.client();
  if (client) {
    client.println("HTTP/1.1 200 OK");
    client.println("Content-Type: image/jpeg");
    client.println("Content-Length: " + String(fb->len));
    client.println("Access-Control-Allow-Origin: *");
    client.println("Cache-Control: no-cache, no-store, must-revalidate");
    client.println("Pragma: no-cache");
    client.println("Connection: close");
    client.println();

    uint8_t *buf = fb->buf;
    size_t len = fb->len;
    size_t chunk = 2048;
    for (size_t i = 0; i < len; i += chunk) {
      size_t toWrite = (i + chunk < len) ? chunk : (len - i);
      client.write(buf + i, toWrite);
    }
  }
  esp_camera_fb_return(fb);
}

// =====================================================
// CAPTURE PHOTO & HTTP MULTIPART POST TO SPRING BOOT
// =====================================================
bool captureAndSendPhoto() {
  if (WiFi.status() != WL_CONNECTED) {
    addLog("Wi-Fi not connected. Cannot send photo.");
    lastCaptureMsg = "Wi-Fi ขาดการเชื่อมต่อ ไม่สามารถส่งรูปได้";
    return false;
  }

  // ตรวจสอบสถานะไฟแฟลช: จะเปิดเฉพาะเมื่อผู้ใช้สั่งเปิดแฟลชเท่านั้น (ป้องกันแสงสะท้อนบนฝากระจกมิเตอร์)
  if (FLASH_LED_PIN >= 0) {
    digitalWrite(FLASH_LED_PIN, flashState ? HIGH : LOW);
  }

  // Flush stale buffer frame to ensure fresh realtime image
  camera_fb_t * stale_fb = esp_camera_fb_get();
  if (stale_fb) esp_camera_fb_return(stale_fb);

  camera_fb_t * fb = esp_camera_fb_get();

  if (!fb) {
    addLog("Camera Capture Failed!");
    lastCaptureMsg = "บันทึกภาพจากเซนเซอร์กล้องไม่สำเร็จ";
    return false;
  }

  addLog("Photo Captured! Size: " + String(fb->len) + " bytes");

  WiFiClient client;
  String serverHost = customServerIP;
  int serverPortNum = customServerPort;

  if (!client.connect(serverHost.c_str(), serverPortNum)) {
    addLog("Failed to connect to Spring Boot Server at " + serverHost + ":" + String(serverPortNum));
    lastCaptureMsg = "ไม่สามารถเชื่อมต่อ Server " + serverHost + ":" + String(serverPortNum);
    esp_camera_fb_return(fb);
    return false;
  }

  String boundary = "----ESP32CAMBoundary" + String(millis());
  String head = "--" + boundary + "\r\n";
  head += "Content-Disposition: form-data; name=\"roomNumber\"\r\n\r\n" + customRoomNumber + "\r\n";
  head += "--" + boundary + "\r\n";
  head += "Content-Disposition: form-data; name=\"targetMode\"\r\n\r\n" + targetMode + "\r\n";
  head += "--" + boundary + "\r\n";
  head += "Content-Disposition: form-data; name=\"file\"; filename=\"meter_room" + customRoomNumber + ".jpg\"\r\n";
  head += "Content-Type: image/jpeg\r\n\r\n";

  String tail = "\r\n--" + boundary + "--\r\n";

  uint32_t totalLen = head.length() + fb->len + tail.length();

  addLog("Sending Photo to Spring Boot (/api/meter/submit-photo) for Room " + customRoomNumber + "...");

  client.println("POST /api/meter/submit-photo HTTP/1.1");
  client.println("Host: " + serverHost + ":" + String(serverPortNum));
  client.println("Content-Type: multipart/form-data; boundary=" + boundary);
  client.println("Content-Length: " + String(totalLen));
  client.println("Connection: close");
  client.println();
  client.print(head);

  uint8_t *fbBuf = fb->buf;
  size_t fbLen = fb->len;
  size_t chunkSize = 1024;
  for (size_t n = 0; n < fbLen; n += chunkSize) {
    size_t size = (n + chunkSize < fbLen) ? chunkSize : (fbLen - n);
    client.write(fbBuf + n, size);
  }

  client.print(tail);

  esp_camera_fb_return(fb);

  unsigned long timeout = millis();
  while (client.available() == 0) {
    if (millis() - timeout > 35000) { // รอได้ถึง 35 วินาที เพื่อให้ Cloud AI ประมวลผลและตอบกลับสำเร็จ
      addLog("Timeout waiting for Spring Boot response.");
      lastCaptureMsg = "หมดเวลาการรอผลตอบกลับจากเซิร์ฟเวอร์ (Timeout)";
      client.stop();
      return false;
    }
    delay(100);
  }

  String response = client.readString();
  addLog("Response from Spring Boot:");
  addLog(response.substring(0, 300));

  client.stop();

  if (response.indexOf("\"success\":true") >= 0) {
    lastCaptureStatus = "SUCCESS";
    int rIdx = response.indexOf("\"meterReading\":");
    if (rIdx >= 0) {
      int start = rIdx + 15;
      int end = response.indexOf(",", start);
      if (end < 0) end = response.indexOf("}", start);
      if (end > start) {
        lastCaptureReading = response.substring(start, end);
        lastCaptureReading.trim();
      }
    }
    lastCaptureMsg = "ระบบอ่านค่าสำเร็จและบันทึกลงฐานข้อมูลแล้ว";
    return true;
  } else {
    lastCaptureStatus = "FAILED";
    lastCaptureMsg = "เซิร์ฟเวอร์แจ้งข้อผิดพลาด: ตรวจสอบ Log";
    return false;
  }
}

// =====================================================
// NVS MEMORY & AP CAPTIVE PORTAL CONFIG
// =====================================================
void loadConfigFromNVS() {
  prefs.begin("cam_cfg", true);
  wifiSsid = prefs.getString("ssid", wifiSsid);
  wifiPassword = prefs.getString("pass", wifiPassword);
  customServerIP = prefs.getString("server_ip", customServerIP);
  customServerPort = prefs.getInt("server_port", customServerPort);
  customRoomNumber = prefs.getString("room", customRoomNumber);
  customDeviceId = prefs.getString("device_id", customDeviceId);
  targetMode = prefs.getString("target_mode", targetMode);
  workMode = prefs.getString("work_mode", "MANUAL"); // ค่าเริ่มต้นเป็น MANUAL สำหรับช่วงทดสอบ
  captureIntervalMinutes = prefs.getInt("interval_min", 5);
  isVflip = prefs.getBool("vflip", false);
  isHmirror = prefs.getBool("hmirror", false);
  prefs.end();
}

void saveConfigToNVS() {
  prefs.begin("cam_cfg", false);
  prefs.putString("ssid", wifiSsid);
  prefs.putString("pass", wifiPassword);
  prefs.putString("server_ip", customServerIP);
  prefs.putInt("server_port", customServerPort);
  prefs.putString("room", customRoomNumber);
  prefs.putString("device_id", customDeviceId);
  prefs.putString("target_mode", targetMode);
  prefs.putString("work_mode", workMode);
  prefs.putInt("interval_min", captureIntervalMinutes);
  prefs.putBool("vflip", isVflip);
  prefs.putBool("hmirror", isHmirror);
  prefs.end();
  addLog("Config saved to NVS Flash Memory!");
}

void connectWiFi() {
  wifiSsid.trim();
  if (wifiSsid == "") {
    addLog("No saved Wi-Fi configuration. Starting setup Access Point...");
    startApCaptivePortal();
    return;
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(wifiSsid.c_str(), wifiPassword.c_str());
  addLog("Connecting to Wi-Fi: " + wifiSsid + "...");

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - start > 8000) {
      addLog("Wi-Fi timeout. Starting AP Setup Mode...");
      startApCaptivePortal();
      return;
    }
    delay(500);
  }
  addLog("Wi-Fi connected. Local IP: " + WiFi.localIP().toString());
}

void startApCaptivePortal() {
  isApMode = true;
  WiFi.mode(WIFI_OFF);
  delay(100);
  WiFi.mode(WIFI_AP);
  delay(100);
  WiFi.softAPConfig(AP_IP, AP_IP, IPAddress(255, 255, 255, 0));
  
  bool ok = WiFi.softAP("ESP32-CAM-Meter-Setup");
  if (!ok) {
    WiFi.softAP("ESP32-CAM-Meter-Setup", "12345678");
  }

  dnsServer.start(DNS_PORT, "*", WiFi.softAPIP());

  server.on("/", []() {
    String html = "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                  "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                  "<title>📷 ESP32-CAM Setup Portal</title>"
                  "<style>"
                  "body{font-family:sans-serif;background:#0f172a;color:#fff;padding:20px;display:flex;justify-content:center;}"
                  ".card{background:#1e293b;border-radius:16px;padding:24px;max-width:420px;width:100%;}"
                  "h2{color:#00f2fe;text-align:center;}"
                  "label{display:block;font-size:0.85rem;color:#cbd5e1;margin-top:10px;}"
                  "input,select{width:100%;background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px;color:#fff;box-sizing:border-box;}"
                  ".btn{width:100%;background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;border:none;border-radius:10px;padding:12px;font-weight:bold;margin-top:16px;cursor:pointer;}"
                  "</style></head><body><div class='card'>"
                  "<h2>📷 ESP32-CAM Setup</h2>"
                  "<form action='/save' method='POST'>"
                  "<label>ชื่อ Wi-Fi (SSID)</label><input type='text' name='ssid' value='" + wifiSsid + "' required>"
                  "<label>รหัสผ่าน Wi-Fi</label><input type='password' name='pass' value='" + wifiPassword + "' required>"
                  "<label>IP Address เครื่อง Spring Boot Server</label><input type='text' name='server_ip' value='" + customServerIP + "' required>"
                  "<label>Port Server</label><input type='number' name='server_port' value='" + String(customServerPort) + "' required>"
                  "<label>หมายเลขห้องที่กล้องนี้ติดตั้ง (Room Number)</label><input type='text' name='room' value='" + customRoomNumber + "' required>"
                  "<label>โหมดเริ่มต้นการทำงาน</label><select name='work_mode'>"
                  "<option value='MANUAL'" + String(workMode == "MANUAL" ? " selected" : "") + ">🧪 MANUAL (กดถ่ายรูปเองช่วงทดสอบ)</option>"
                  "<option value='AUTO'" + String(workMode == "AUTO" ? " selected" : "") + ">🚀 AUTO (ถ่ายรูปอ่านค่าอัตโนมัติเมื่อติดตั้งจริง)</option>"
                  "</select>"
                  "<label>ความถี่การถ่ายภาพอัตโนมัติ (นาที)</label><input type='number' name='interval_min' value='" + String(captureIntervalMinutes) + "' required>"
                  "<button type='submit' class='btn'>💾 บันทึกและรีสตาร์ท</button>"
                  "</form></div></body></html>";
    server.send(200, "text/html; charset=UTF-8", html);
  });

  server.on("/save", HTTP_POST, []() {
    if (server.hasArg("ssid")) wifiSsid = server.arg("ssid");
    if (server.hasArg("pass")) wifiPassword = server.arg("pass");
    if (server.hasArg("server_ip")) customServerIP = server.arg("server_ip");
    if (server.hasArg("server_port")) customServerPort = server.arg("server_port").toInt();
    if (server.hasArg("room")) customRoomNumber = server.arg("room");
    if (server.hasArg("work_mode")) workMode = server.arg("work_mode");
    if (server.hasArg("interval_min")) captureIntervalMinutes = server.arg("interval_min").toInt();
    saveConfigToNVS();
    server.send(200, "text/html; charset=UTF-8", "<html><head><meta charset='UTF-8'></head><body style='background:#0f172a;color:#00f2fe;font-family:sans-serif;text-align:center;padding:40px;'><h2>บันทึกเรียบร้อยแล้ว! 🎉</h2><p style='color:#fff;'>กำลังรีบูตบอร์ดเพื่อเชื่อมต่อ Wi-Fi หอพัก...</p></body></html>");
    delay(2000);
    ESP.restart();
  });

  server.begin();
  addLog("AP Captive Portal started. Connect to Wi-Fi: " + String(AP_SSID));
}

void addLog(const String& message) {
  Serial.println(message);
  logsBuffer += message + "\n";
  if (logsBuffer.length() > 2000) {
    logsBuffer = logsBuffer.substring(logsBuffer.length() - 1500);
  }
}
