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

// Dynamic Config Variables
String wifiSsid = "";
String wifiPassword = "";
String customServerIP = "10.21.72.108";
int customServerPort = 8080;

// *** Logic การผูกห้อง & โหมดการทำงาน ***
String customRoomNumber = "101";
String customDeviceId = "ESP32-CAM-ROOM101";
String targetMode = "DIRECT_OCR"; // "DIRECT_OCR" หรือ "LINE_FLEX_MESSAGE"

// โหมดการทำงาน: "MANUAL" (กดถ่ายเองช่วงทดสอบ) หรือ "AUTO" (อ่านอัตโนมัติเมื่อติดตั้งจริง)
String workMode = "MANUAL";
int captureIntervalMinutes = 5; // ถ่ายรูปอัตโนมัติทุกๆ กี่นาที (เมื่ออยู่โหมด AUTO)

unsigned long lastCaptureTime = 0;
bool flashState = false;

WebServer server(80);
bool isApMode = false;
bool isCameraReady = false;
String logsBuffer = "";

// Function Declarations
void addLog(const String& message);
void loadConfigFromNVS();
void saveConfigToNVS();
bool initCamera();
bool captureAndSendPhoto();
void startApCaptivePortal();
void connectWiFi();
void handleJpgStream();

void setup() {
  Serial.begin(115200);
  delay(1000);

  if (FLASH_LED_PIN >= 0) {
    pinMode(FLASH_LED_PIN, OUTPUT);
    digitalWrite(FLASH_LED_PIN, LOW);
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
  }

  if (!isApMode) {
    // Web Dashboard หลักสำหรับกล้องจดมิเตอร์จานหมุน
    server.on("/", []() {
      String html = "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    "<title>📷 ESP32-CAM Retrofit Analog Meter</title>"
                    "<style>"
                    "body{font-family:sans-serif;background:#0f172a;color:#fff;padding:20px;display:flex;justify-content:center;}"
                    ".card{background:#1e293b;border-radius:18px;padding:24px;max-width:500px;width:100%;box-shadow:0 10px 30px rgba(0,0,0,0.5);}"
                    "h2{color:#00f2fe;text-align:center;margin-bottom:6px;}"
                    ".status-badge{display:inline-block;padding:6px 12px;border-radius:20px;font-size:0.85rem;font-weight:bold;margin-bottom:16px;}"
                    ".bg-green{background:rgba(16,185,129,0.2);color:#34d399;border:1px solid #10b981;}"
                    ".bg-yellow{background:rgba(245,158,11,0.2);color:#fbbf24;border:1px solid #f59e0b;}"
                    ".btn{display:block;width:100%;padding:14px;border-radius:12px;border:none;font-size:1rem;font-weight:bold;cursor:pointer;margin-bottom:10px;text-align:center;text-decoration:none;box-sizing:border-box;}"
                    ".btn-primary{background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;box-shadow:0 4px 15px rgba(0,242,254,0.3);}"
                    ".btn-secondary{background:#334155;color:#fff;}"
                    ".btn-warning{background:linear-gradient(135deg,#f59e0b,#d97706);color:#fff;}"
                    ".info-box{background:#0f172a;border:1px solid #334155;border-radius:12px;padding:14px;margin-bottom:16px;font-size:0.9rem;text-align:left;}"
                    "img.preview{width:100%;max-height:260px;object-fit:contain;border-radius:12px;border:1px solid #334155;margin-bottom:16px;background:#000;}"
                    "</style></head><body><div class='card'>"
                    "<h2>⚙️ ESP32-CAM อ่านมิเตอร์จานหมุน</h2>"
                    "<div style='text-align:center;'>"
                    "<span class='status-badge " + String(workMode == "MANUAL" ? "bg-yellow'>🧪 โหมดทดสอบ (Manual Only)" : "bg-green'>🚀 โหมดใช้งานจริง (Auto " + String(captureIntervalMinutes) + " นาที)") + "</span>"
                    "</div>"
                    "<div class='info-box'>"
                    "📍 <b>ผูกกับห้อง:</b> <b style='color:#00f2fe;font-size:1.1rem;'>" + customRoomNumber + "</b><br>"
                    "🆔 <b>Device ID:</b> " + customDeviceId + "<br>"
                    "🌐 <b>Spring Boot Server:</b> http://" + customServerIP + ":" + String(customServerPort) + "<br>"
                    "⚙️ <b>โหมดประมวลผล:</b> " + targetMode +
                    "</div>"
                    "<h3>📸 ภาพกล้องถ่ายสดสำหรับเล็งมุมหน้าปัดมิเตอร์:</h3>"
                    "<img src='/stream' class='preview' alt='Camera Stream'>"
                    "<a href='/capture' class='btn btn-primary'>📸 ถ่ายรูปสแกนส่ง Spring Boot เดี๋ยวนี้</a>"
                    "<a href='/toggle-flash' class='btn btn-warning'>💡 " + String(flashState ? "ปิดไฟ Flash LED" : "เปิดไฟ Flash LED (ส่องหน้าปัด)") + "</a>"
                    "<a href='/toggle-mode' class='btn btn-secondary'>🔄 สลับโหมด (ปัจจุบัน: " + workMode + ")</a>"
                    "<div style='margin-top:16px;font-size:0.85rem;'>"
                    "<a href='/log' style='color:#94a3b8;margin-right:12px;'>ดู System Log</a> | "
                    "<a href='/reset-ap' style='color:#ef4444;'>ตั้งค่า Wi-Fi / เลขห้องใหม่</a>"
                    "</div>"
                    "</div></body></html>";
      server.send(200, "text/html; charset=UTF-8", html);
    });

    // Endpoint ถ่ายภาพสดสำหรับส่องมุมมองหน้าปัดมิเตอร์
    server.on("/stream", HTTP_GET, handleJpgStream);

    // Manual Capture Trigger Endpoint
    server.on("/capture", []() {
      if (!isCameraReady) {
        server.send(400, "text/plain", "กล้องยังไม่พร้อมใช้งาน ตรวจสอบการเสียบสายกล้อง หรือการตั้งค่า PSRAM");
        return;
      }
      addLog("--- Manual Capture Requested via Web Dashboard ---");
      bool ok = captureAndSendPhoto();
      String msg = ok ? "✅ ถ่ายรูปสแกนส่ง Spring Boot สำเร็จ!" : "❌ ไม่สามารถส่งภาพไปที่ Spring Boot ได้ (เช็กการเชื่อมต่อ Server)";
      String html = "<html><head><meta charset='UTF-8'><meta http-equiv='refresh' content='3;url=/'></head>"
                    "<body style='background:#0f172a;color:#fff;font-family:sans-serif;text-align:center;padding:40px;'>"
                    "<h2>" + msg + "</h2><p style='color:#94a3b8;'>กำลังกลับไปที่หน้าหลักใน 3 วินาที...</p></body></html>";
      server.send(200, "text/html; charset=UTF-8", html);
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
// HANDLE JPG SINGLE STREAM/SNAPSHOT FOR PREVIEW
// =====================================================
void handleJpgStream() {
  if (!isCameraReady) {
    server.send(500, "text/plain", "Camera not ready");
    return;
  }
  camera_fb_t * fb = esp_camera_fb_get();
  if (!fb) {
    server.send(500, "text/plain", "Camera capture failed");
    return;
  }
  server.sendHeader("Content-Type", "image/jpeg");
  server.sendHeader("Content-Length", String(fb->len));
  server.sendHeader("Cache-Control", "no-cache");
  server.sendContent((const char *)fb->buf, fb->len);
  esp_camera_fb_return(fb);
}

// =====================================================
// CAPTURE PHOTO & HTTP MULTIPART POST TO SPRING BOOT
// =====================================================
bool captureAndSendPhoto() {
  if (WiFi.status() != WL_CONNECTED) {
    addLog("Wi-Fi not connected. Cannot send photo.");
    return false;
  }

  // เปิดไฟ Flash LED สั้นๆ ขณะถ่ายรูป หากไม่ได้เปิดไว้ก่อนหน้า
  bool tempFlash = false;
  if (FLASH_LED_PIN >= 0 && !flashState) {
    digitalWrite(FLASH_LED_PIN, HIGH);
    tempFlash = true;
    delay(150);
  }

  // Flush stale buffer frame to ensure fresh realtime image
  camera_fb_t * stale_fb = esp_camera_fb_get();
  if (stale_fb) esp_camera_fb_return(stale_fb);

  camera_fb_t * fb = esp_camera_fb_get();

  if (tempFlash && FLASH_LED_PIN >= 0) {
    digitalWrite(FLASH_LED_PIN, LOW);
  }

  if (!fb) {
    addLog("Camera Capture Failed!");
    return false;
  }

  addLog("Photo Captured! Size: " + String(fb->len) + " bytes");

  WiFiClient client;
  String serverHost = customServerIP;
  int serverPortNum = customServerPort;

  if (!client.connect(serverHost.c_str(), serverPortNum)) {
    addLog("Failed to connect to Spring Boot Server at " + serverHost + ":" + String(serverPortNum));
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
    if (millis() - timeout > 8000) {
      addLog("Timeout waiting for Spring Boot response.");
      client.stop();
      return false;
    }
  }

  String response = client.readString();
  addLog("Response from Spring Boot:");
  addLog(response.substring(0, 300));

  client.stop();
  return true;
}

// =====================================================
// NVS MEMORY & AP CAPTIVE PORTAL CONFIG
// =====================================================
void loadConfigFromNVS() {
  prefs.begin("cam_cfg", true);
  wifiSsid = prefs.getString("ssid", "");
  wifiPassword = prefs.getString("pass", "");
  customServerIP = prefs.getString("server_ip", customServerIP);
  customServerPort = prefs.getInt("server_port", customServerPort);
  customRoomNumber = prefs.getString("room", customRoomNumber);
  customDeviceId = prefs.getString("device_id", customDeviceId);
  targetMode = prefs.getString("target_mode", targetMode);
  workMode = prefs.getString("work_mode", "MANUAL"); // ค่าเริ่มต้นเป็น MANUAL สำหรับช่วงทดสอบ
  captureIntervalMinutes = prefs.getInt("interval_min", 5);
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
