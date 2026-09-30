#include <WiFi.h>
#include <HTTPClient.h>
#include <WebServer.h>
#include <DNSServer.h>
#include <ElegantOTA.h>
#include <EthernetENC.h>
#include <SPI.h>
#include <Preferences.h>
#include <vector>

#define MAX485_CONTROL_PIN 4
#define RS485_TRANSMIT HIGH
#define RS485_RECEIVE LOW

// =====================================================
// NVS Flash Config & DNS Server
// =====================================================
Preferences prefs;
DNSServer dnsServer;
const byte DNS_PORT = 53;

// =====================================================
// Meter Selection Control
// =====================================================
enum MeterModel {
  METER_SCHNEIDER_PM2200,
  METER_CIRCUTOR_CVMC5
};

MeterModel currentMeterModel = METER_CIRCUTOR_CVMC5;

// =====================================================
// Dynamic Config Variables
// =====================================================
String wifiSsid = "Galaxy A15 5G 4A21";
String wifiPassword = "0859631128";
String customServerIP = "10.221.143.108";
int customServerPort = 8080;
String customRoomNumber = "101";
String customMeterId = "METER001";
String customDeviceId = "OPTA-DORM-01";

const char* ssid = wifiSsid.c_str();
const char* password = wifiPassword.c_str();
const char* serverIP = customServerIP.c_str();
int serverPort = 8080;
const char* roomNumber = customRoomNumber.c_str();
const char* meterId = customMeterId.c_str();

String sendDataUrl = "";
String networkModeUrl = "";

bool isApMode = false;

// =====================================================
// ENC28J60 Pins (VSPI Standard ESP32)
// =====================================================
#define ETH_CS_PIN   5
#define ETH_SCK_PIN  18
#define ETH_MISO_PIN 19
#define ETH_MOSI_PIN 23

byte ethMac[] = { 0x02, 0xAA, 0xBB, 0xCC, 0xDD, 0x01 };
EthernetClient ethernetClient;

// =====================================================
// Web Server (WiFi Management & Log Dashboard)
// =====================================================
WebServer server(80);

// =====================================================
// Meter Values
// =====================================================
float voltage = 0.0f;
float energy = 0.0f;
float current = 0.0f;
float power = 0.0f;
float frequency = 0.0f;

// =====================================================
// Network Mode Control
// =====================================================
enum NetworkMode {
  MODE_WIFI,
  MODE_LAN
};

NetworkMode currentNetworkMode = MODE_WIFI;

unsigned long lastNetworkModeCheck = 0;
const unsigned long networkModeCheckInterval = 30000;
unsigned long lastReadTime = 0;
const unsigned long readInterval = 10000;
unsigned long lastWiFiReconnect = 0;
const unsigned long wifiReconnectInterval = 10000;

String logsBuffer;

// Function Declarations
uint16_t calculateCRC(const uint8_t* data, uint8_t length);
std::vector<uint8_t> packetCommandSchneider(const String& addressStr, uint16_t lengthValue);
std::vector<uint8_t> packetCommandCircutor(uint16_t hexAddress, uint16_t regLen);
float bytes4ToFloat(const uint8_t* data);
uint32_t bytes4ToUint32(const uint8_t* data);
float bytes8ToEnergy(const uint8_t* data);

bool sendModbusRequest(const std::vector<uint8_t>& command, std::vector<uint8_t>& response, uint32_t timeoutMs);
bool readSchneiderPM2200();
bool readCircutorCVMC5();

void sendReading();
void sendReadingWiFi();
void sendReadingLan();
void checkNetworkMode();
void setNetworkMode(NetworkMode mode);
void connectWiFi();
void maintainWiFi();
void setupEthernet();
void addLog(const String& message);
void loadConfigFromNVS();
void saveConfigToNVS();
void startApCaptivePortal();
void handleCaptivePortalSave();

// =====================================================
// NVS FLASH FUNCTIONS
// =====================================================
void loadConfigFromNVS() {
  prefs.begin("meter_cfg", true);
  wifiSsid = prefs.getString("ssid", wifiSsid);
  wifiPassword = prefs.getString("pass", wifiPassword);
  customServerIP = prefs.getString("server_ip", customServerIP);
  customServerPort = prefs.getInt("server_port", customServerPort);
  customRoomNumber = prefs.getString("room", customRoomNumber);
  customMeterId = prefs.getString("meter_id", customMeterId);
  customDeviceId = prefs.getString("device_id", customDeviceId);
  int modelInt = prefs.getInt("meter_model", (int)METER_CIRCUTOR_CVMC5);
  currentMeterModel = (MeterModel)modelInt;
  prefs.end();

  ssid = wifiSsid.c_str();
  password = wifiPassword.c_str();
  serverIP = customServerIP.c_str();
  serverPort = customServerPort;
  roomNumber = customRoomNumber.c_str();
  meterId = customMeterId.c_str();

  sendDataUrl = "http://" + customServerIP + ":" + String(customServerPort) + "/api/meter";
  networkModeUrl = "http://" + customServerIP + ":" + String(customServerPort) + "/api/meter/network-mode?meterId=" + customMeterId;
}

void saveConfigToNVS() {
  prefs.begin("meter_cfg", false);
  prefs.putString("ssid", wifiSsid);
  prefs.putString("pass", wifiPassword);
  prefs.putString("server_ip", customServerIP);
  prefs.putInt("server_port", customServerPort);
  prefs.putString("room", customRoomNumber);
  prefs.putString("meter_id", customMeterId);
  prefs.putString("device_id", customDeviceId);
  prefs.putInt("meter_model", (int)currentMeterModel);
  prefs.end();
  addLog("Config saved to NVS Flash Memory!");
}

// =====================================================
// AP CAPTIVE PORTAL MODE
// =====================================================
void startApCaptivePortal() {
  isApMode = true;
  addLog("======================================");
  addLog("Starting AP Captive Portal Setup Mode");
  addLog("Connect Wi-Fi: Meter-Gateway-Setup");
  addLog("Open Browser: http://192.168.4.1");
  addLog("======================================");

  WiFi.mode(WIFI_AP);
  WiFi.softAP("Meter-Gateway-Setup", "12345678");

  dnsServer.start(DNS_PORT, "*", WiFi.softAPIP());

  server.on("/", []() {
    String html = "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                  "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                  "<title>⚡ Meter Gateway Provisioning Portal</title>"
                  "<style>"
                  "body{font-family:sans-serif;background:#0f172a;color:#fff;padding:20px;display:flex;justify-content:center;}"
                  ".card{background:#1e293b;border-radius:16px;padding:24px;max-width:420px;width:100%;box-shadow:0 10px 30px rgba(0,0,0,0.5);}"
                  "h2{color:#00f2fe;text-align:center;margin-bottom:6px;}"
                  "p.sub{color:#94a3b8;font-size:0.85rem;text-align:center;margin-bottom:20px;}"
                  ".form-group{margin-bottom:14px;}"
                  "label{display:block;font-size:0.85rem;color:#cbd5e1;margin-bottom:6px;}"
                  "input,select{width:100%;background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px;color:#fff;box-sizing:border-box;outline:none;}"
                  "input:focus,select:focus{border-color:#00f2fe;}"
                  ".btn{width:100%;background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;border:none;border-radius:10px;padding:12px;font-weight:bold;font-size:1rem;cursor:pointer;margin-top:10px;}"
                  "</style></head><body><div class='card'>"
                  "<h2>⚡ Opta / ESP32 Gateway</h2>"
                  "<p class='sub'>ตั้งค่าการเชื่อมต่อ Wi-Fi และเครื่อง Server หอพัก</p>"
                  "<form action='/save' method='POST'>"
                  "<div class='form-group'><label>ชื่อ Wi-Fi (SSID)</label><input type='text' name='ssid' value='" + wifiSsid + "' required></div>"
                  "<div class='form-group'><label>รหัสผ่าน Wi-Fi</label><input type='password' name='pass' value='" + wifiPassword + "' required></div>"
                  "<div class='form-group'><label>IP Address เครื่อง Server</label><input type='text' name='server_ip' value='" + customServerIP + "' required></div>"
                  "<div class='form-group'><label>Server Port</label><input type='number' name='server_port' value='" + String(customServerPort) + "' required></div>"
                  "<div class='form-group'><label>รุ่นมิเตอร์หลัก</label><select name='model'>"
                  "<option value='circutor'" + String(currentMeterModel == METER_CIRCUTOR_CVMC5 ? " selected" : "") + ">Circutor CVM-C5</option>"
                  "<option value='schneider'" + String(currentMeterModel == METER_SCHNEIDER_PM2200 ? " selected" : "") + ">Schneider PM2200</option>"
                  "</select></div>"
                  "<div class='form-group'><label>หมายเลขห้อง</label><input type='text' name='room' value='" + customRoomNumber + "' required></div>"
                  "<button type='submit' class='btn'>💾 บันทึกและเชื่อมต่อ (Save & Connect)</button>"
                  "</form></div></body></html>";
    server.send(200, "text/html", html);
  });

  server.on("/save", HTTP_POST, handleCaptivePortalSave);
  server.onNotFound([]() {
    server.sendHeader("Location", "http://192.168.4.1/", true);
    server.send(302, "text/plain", "");
  });

  server.begin();
}

void handleCaptivePortalSave() {
  if (server.hasArg("ssid")) wifiSsid = server.arg("ssid");
  if (server.hasArg("pass")) wifiPassword = server.arg("pass");
  if (server.hasArg("server_ip")) customServerIP = server.arg("server_ip");
  if (server.hasArg("server_port")) customServerPort = server.arg("server_port").toInt();
  if (server.hasArg("room")) customRoomNumber = server.arg("room");
  if (server.hasArg("model")) {
    String m = server.arg("model");
    if (m == "circutor") currentMeterModel = METER_CIRCUTOR_CVMC5;
    else if (m == "schneider") currentMeterModel = METER_SCHNEIDER_PM2200;
  }

  saveConfigToNVS();

  String html = "<html><head><meta charset='UTF-8'></head><body style='background:#0f172a;color:#00f2fe;font-family:sans-serif;text-align:center;padding:50px'>"
                "<h2>บันทึกข้อมูลเรียบร้อยแล้ว! 🎉</h2>"
                "<p style='color:#fff'>กำลังรีบูตบอร์ดเพื่อเชื่อมต่อเครือข่ายใหม่...</p></body></html>";
  server.send(200, "text/html", html);
  delay(2000);
  ESP.restart();
}

// =====================================================
// SETUP
// =====================================================
void setup() {
  Serial.begin(115200);
  delay(500);

  loadConfigFromNVS();

  addLog("======================================");
  addLog("ESP32 / Opta Meter Bridge Starting");
  addLog("Active Meter: " + String(currentMeterModel == METER_CIRCUTOR_CVMC5 ? "CIRCUTOR CVM-C5" : "SCHNEIDER PM2200"));
  addLog("Server Target: " + sendDataUrl);
  addLog("======================================");

  pinMode(MAX485_CONTROL_PIN, OUTPUT);
  digitalWrite(MAX485_CONTROL_PIN, RS485_RECEIVE);
  Serial2.begin(9600, SERIAL_8N1, 16, 17);

  connectWiFi();

  if (!isApMode) {
    setupEthernet();

    server.on("/", []() {
      String message = "ESP32 Meter Bridge - Active\n";
      message += "Active Meter Model: " + String(currentMeterModel == METER_CIRCUTOR_CVMC5 ? "CIRCUTOR CVM-C5" : "SCHNEIDER PM2200") + "\n";
      message += "Network Mode: " + String(currentNetworkMode == MODE_WIFI ? "WIFI" : "LAN") + "\n";
      message += "WiFi IP: " + WiFi.localIP().toString() + "\n";
      message += "LAN IP: " + Ethernet.localIP().toString() + "\n";
      message += "\n--- Latest Meter Values ---\n";
      message += "Voltage: " + String(voltage, 2) + " V\n";
      message += "Current: " + String(current, 3) + " A\n";
      message += "Power: " + String(power, 2) + " W\n";
      message += "Energy: " + String(energy, 2) + " kWh\n";
      message += "Frequency: " + String(frequency, 1) + " Hz\n";
      message += "\nReset config to AP Mode:\n";
      message += "  /reset-ap\n";
      server.send(200, "text/plain", message);
    });

    server.on("/reset-ap", []() {
      server.send(200, "text/plain", "Resetting config & restarting AP Captive Portal...");
      delay(1000);
      wifiSsid = "";
      saveConfigToNVS();
      ESP.restart();
    });

    server.on("/set-meter", []() {
      if (server.hasArg("model")) {
        String modelArg = server.arg("model");
        modelArg.toLowerCase();
        if (modelArg == "circutor") {
          currentMeterModel = METER_CIRCUTOR_CVMC5;
          saveConfigToNVS();
          addLog("Meter model switched to: CIRCUTOR CVM-C5");
          server.send(200, "text/plain", "Switched active meter to CIRCUTOR CVM-C5");
          return;
        } else if (modelArg == "schneider") {
          currentMeterModel = METER_SCHNEIDER_PM2200;
          saveConfigToNVS();
          addLog("Meter model switched to: SCHNEIDER PM2200");
          server.send(200, "text/plain", "Switched active meter to SCHNEIDER PM2200");
          return;
        }
      }
      server.send(400, "text/plain", "Invalid model parameter. Use ?model=circutor or ?model=schneider");
    });

    server.on("/log", []() {
      String html = "<html><head><meta http-equiv='refresh' content='3'></head>";
      html += "<body style='background:#1e1e1e;color:#00ff00;font-family:monospace;padding:20px'>";
      html += "<h2>ESP32 Meter Log Dashboard</h2>";
      html += "<p>Active Meter: <b>" + String(currentMeterModel == METER_CIRCUTOR_CVMC5 ? "CIRCUTOR CVM-C5" : "SCHNEIDER PM2200") + "</b></p>";
      html += "<pre>" + logsBuffer + "</pre></body></html>";
      server.send(200, "text/html", html);
    });

    server.on("/api/config", HTTP_POST, []() {
      if (server.hasArg("plain")) {
        String body = server.arg("plain");
        addLog("Received Remote Config Push: " + body);

        if (body.indexOf("CIRCUTOR_CVMC5") >= 0) {
          currentMeterModel = METER_CIRCUTOR_CVMC5;
          addLog("Meter Model updated -> CIRCUTOR CVM-C5");
        } else if (body.indexOf("SCHNEIDER_PM2200") >= 0) {
          currentMeterModel = METER_SCHNEIDER_PM2200;
          addLog("Meter Model updated -> SCHNEIDER PM2200");
        }

        saveConfigToNVS();
        server.send(200, "application/json", "{\"status\":\"SUCCESS\",\"message\":\"Device configuration saved to NVS Flash\"}");
        return;
      }
      server.send(400, "application/json", "{\"status\":\"ERROR\",\"message\":\"No body\"}");
    });

    ElegantOTA.begin(&server);
    server.begin();
  }
}

// =====================================================
// MAIN LOOP
// =====================================================
void loop() {
  if (isApMode) {
    dnsServer.processNextRequest();
    server.handleClient();
    return;
  }

  server.handleClient();
  ElegantOTA.loop();
  maintainWiFi();

  if (millis() - lastNetworkModeCheck >= networkModeCheckInterval) {
    lastNetworkModeCheck = millis();
    checkNetworkMode();
  }

  if (millis() - lastReadTime >= readInterval) {
    lastReadTime = millis();
    addLog("--- Starting new read cycle ---");

    bool readSuccess = false;
    if (currentMeterModel == METER_CIRCUTOR_CVMC5) {
      readSuccess = readCircutorCVMC5();
    } else if (currentMeterModel == METER_SCHNEIDER_PM2200) {
      readSuccess = readSchneiderPM2200();
    }

    if (readSuccess) {
      sendReading();
    } else {
      addLog("Meter reading failed; data was not sent.");
    }
  }
}

// =====================================================
// NETWORK SEND & CONTROLS
// =====================================================
void sendReading() {
  bool isLanReady = (Ethernet.linkStatus() != LinkOFF) && (Ethernet.localIP() != IPAddress(0, 0, 0, 0));
  if (currentNetworkMode == MODE_LAN && isLanReady) {
    sendReadingLan();
  } else {
    sendReadingWiFi();
  }
}

void sendReadingWiFi() {
  if (WiFi.status() != WL_CONNECTED) {
    addLog("Wi-Fi disconnected; reading was not sent.");
    return;
  }

  String json = "{";
  json += "\"meterId\":\"" + String(meterId) + "\",";
  json += "\"roomNumber\":\"" + String(roomNumber) + "\",";
  json += "\"unit\":" + String(energy, 2) + ",";
  json += "\"value\":" + String(energy, 2) + ",";
  json += "\"voltage\":" + String(voltage, 2) + ",";
  json += "\"energy\":" + String(energy, 2) + ",";
  json += "\"current\":" + String(current, 3) + ",";
  json += "\"power\":" + String(power, 2);
  json += "}";

  WiFiClient client;
  HTTPClient http;
  http.setConnectTimeout(5000);
  http.setTimeout(5000);

  if (!http.begin(client, sendDataUrl)) {
    addLog("Cannot create Wi-Fi HTTP connection.");
    return;
  }

  http.addHeader("Content-Type", "application/json");
  int httpCode = http.POST(json);

  String message = "WIFI Data Sent | HTTP Status: " + String(httpCode);
  if (httpCode > 0) {
    message += " | Response: " + http.getString();
  } else {
    message += " | Error: " + HTTPClient::errorToString(httpCode);
  }

  addLog(message);
  http.end();
}

void sendReadingLan() {
  if (Ethernet.linkStatus() == LinkOFF || Ethernet.localIP() == IPAddress(0, 0, 0, 0)) return;

  String json = "{";
  json += "\"meterId\":\"" + String(meterId) + "\",";
  json += "\"roomNumber\":\"" + String(roomNumber) + "\",";
  json += "\"unit\":" + String(energy, 2) + ",";
  json += "\"value\":" + String(energy, 2) + ",";
  json += "\"voltage\":" + String(voltage, 2) + ",";
  json += "\"energy\":" + String(energy, 2) + ",";
  json += "\"current\":" + String(current, 3) + ",";
  json += "\"power\":" + String(power, 2);
  json += "}";

  if (ethernetClient.connect(serverIP, serverPort)) {
    ethernetClient.println("POST /api/meter HTTP/1.1");
    ethernetClient.println("Host: " + String(serverIP) + ":" + String(serverPort));
    ethernetClient.println("Content-Type: application/json");
    ethernetClient.print("Content-Length: ");
    ethernetClient.println(json.length());
    ethernetClient.println("Connection: close");
    ethernetClient.println();
    ethernetClient.println(json);

    unsigned long timeout = millis();
    while (ethernetClient.available() == 0) {
      if (millis() - timeout > 5000) {
        ethernetClient.stop();
        return;
      }
    }

    String statusLine = ethernetClient.readStringUntil('\n');
    addLog("LAN Data Sent: " + json + " | Status: " + statusLine);
    ethernetClient.stop();
  }
}

void checkNetworkMode() {
  if (WiFi.status() != WL_CONNECTED) return;
  WiFiClient client;
  HTTPClient http;
  http.setConnectTimeout(3000);
  http.setTimeout(3000);
  if (!http.begin(client, networkModeUrl)) return;
  int httpCode = http.GET();
  if (httpCode == 200) {
    String response = http.getString();
    if (response.indexOf("\"LAN\"") >= 0 && currentNetworkMode != MODE_LAN) setNetworkMode(MODE_LAN);
    else if (response.indexOf("\"WIFI\"") >= 0 && currentNetworkMode != MODE_WIFI) setNetworkMode(MODE_WIFI);
  }
  http.end();
}

void setNetworkMode(NetworkMode mode) {
  currentNetworkMode = mode;
  addLog(String("NETWORK MODE CHANGED -> ") + (mode == MODE_WIFI ? "WIFI" : "LAN"));
}

void setupEthernet() {
  addLog("Initializing ENC28J60...");
  SPI.begin(ETH_SCK_PIN, ETH_MISO_PIN, ETH_MOSI_PIN, ETH_CS_PIN);
  Ethernet.init(ETH_CS_PIN);

  if (Ethernet.hardwareStatus() == EthernetNoHardware) {
    addLog("ENC28J60 hardware not found. Continuing on Wi-Fi.");
    return;
  }

  if (Ethernet.begin(ethMac, 3000, 1000) == 0) {
    addLog("ENC28J60 DHCP timeout / failed. Continuing on Wi-Fi.");
  } else {
    addLog("ENC28J60 connected successfully. LAN IP: " + Ethernet.localIP().toString());
  }
}

void connectWiFi() {
  if (wifiSsid == "") {
    startApCaptivePortal();
    return;
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  addLog("Connecting to Wi-Fi SSID: " + wifiSsid + "...");

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - start > 12000) {
      addLog("Wi-Fi connection failed. Starting AP Captive Portal...");
      startApCaptivePortal();
      return;
    }
    delay(500);
  }
  addLog("Wi-Fi connected. IP: " + WiFi.localIP().toString());
}

void maintainWiFi() {
  if (isApMode || WiFi.status() == WL_CONNECTED) return;
  if (millis() - lastWiFiReconnect >= wifiReconnectInterval) {
    lastWiFiReconnect = millis();
    addLog("Wi-Fi disconnected. Reconnecting...");
    WiFi.disconnect();
    WiFi.begin(ssid, password);
  }
}

// =====================================================
// MODBUS RTU REQUEST HANDLER
// =====================================================
bool sendModbusRequest(const std::vector<uint8_t>& command, std::vector<uint8_t>& response, uint32_t timeoutMs) {
  while (Serial2.available() > 0) Serial2.read();

  digitalWrite(MAX485_CONTROL_PIN, RS485_TRANSMIT);
  Serial2.write(command.data(), command.size());
  Serial2.flush();
  delayMicroseconds(500);
  digitalWrite(MAX485_CONTROL_PIN, RS485_RECEIVE);

  size_t index = 0;
  unsigned long startTime = millis();
  while (millis() - startTime < timeoutMs && index < response.size()) {
    if (Serial2.available()) {
      response[index++] = static_cast<uint8_t>(Serial2.read());
    }
  }

  if (index < response.size()) return false;

  if (response.size() >= 5) {
    uint16_t calculatedCrc = calculateCRC(response.data(), response.size() - 2);
    uint16_t receivedCrc = response[response.size() - 2] | (response[response.size() - 1] << 8);
    if (calculatedCrc != receivedCrc) {
      addLog("Modbus CRC mismatch!");
      return false;
    }
  }
  return true;
}

// =====================================================
// CIRCUTOR CVM-C5 READ
// =====================================================
bool readCircutorCVMC5() {
  addLog("[Circutor CVM-C5] Reading values...");

  std::vector<uint8_t> cmdVAP = packetCommandCircutor(0x0000, 6);
  std::vector<uint8_t> respVAP(17);

  bool blockOk = sendModbusRequest(cmdVAP, respVAP, 500);
  if (blockOk && respVAP.size() >= 17) {
    uint32_t rawV = bytes4ToUint32(&respVAP[3]);
    uint32_t rawA = bytes4ToUint32(&respVAP[7]);
    uint32_t rawP = bytes4ToUint32(&respVAP[11]);

    voltage = rawV / 10.0f;
    current = rawA / 1000.0f;
    power   = static_cast<float>(rawP);
    addLog("[Circutor] Block Read OK | V: " + String(voltage, 2) + "V, I: " + String(current, 3) + "A, P: " + String(power, 2) + "W");
  } else {
    addLog("[Circutor] Block Read failed; attempting fallbacks...");
    std::vector<uint8_t> cmdV = packetCommandCircutor(0x0000, 2);
    std::vector<uint8_t> respV(9);
    if (sendModbusRequest(cmdV, respV, 500)) voltage = bytes4ToUint32(&respV[3]) / 10.0f;

    std::vector<uint8_t> cmdA = packetCommandCircutor(0x0002, 2);
    std::vector<uint8_t> respA(9);
    if (sendModbusRequest(cmdA, respA, 500)) current = bytes4ToUint32(&respA[3]) / 1000.0f;

    std::vector<uint8_t> cmdP = packetCommandCircutor(0x0004, 2);
    std::vector<uint8_t> respP(9);
    if (sendModbusRequest(cmdP, respP, 500)) power = static_cast<float>(bytes4ToUint32(&respP[3]));
  }
  delay(50);

  std::vector<uint8_t> cmdHz = packetCommandCircutor(0x0028, 2);
  std::vector<uint8_t> respHz(9);
  if (sendModbusRequest(cmdHz, respHz, 500)) {
    frequency = bytes4ToUint32(&respHz[3]) / 10.0f;
  }
  delay(50);

  uint32_t rawTariff1 = 0;
  uint32_t rawTariff2 = 0;
  bool energyOk = false;

  std::vector<uint8_t> cmdT1 = packetCommandCircutor(0x003C, 2);
  std::vector<uint8_t> respT1(9);
  if (sendModbusRequest(cmdT1, respT1, 500)) {
    rawTariff1 = bytes4ToUint32(&respT1[3]);
    energyOk = true;
  }
  delay(50);

  std::vector<uint8_t> cmdT2 = packetCommandCircutor(0x006C, 2);
  std::vector<uint8_t> respT2(9);
  if (sendModbusRequest(cmdT2, respT2, 500)) {
    rawTariff2 = bytes4ToUint32(&respT2[3]);
  }

  if (energyOk) {
    float t1_kWh = rawTariff1 / 1000.0f;
    float t2_kWh = rawTariff2 / 1000.0f;
    energy = t1_kWh + t2_kWh;
    addLog("[Circutor] Energy T1: " + String(t1_kWh, 3) + " kWh | T2: " + String(t2_kWh, 3) + " kWh | Total: " + String(energy, 3) + " kWh");
  } else {
    addLog("[Circutor] Energy (kWh) read failed!");
  }

  return energyOk;
}

// =====================================================
// SCHNEIDER PM2200 READ
// =====================================================
bool readSchneiderPM2200() {
  addLog("[Schneider PM2200] Reading values...");

  auto readModbusSchneider = [](const String& addressStr, uint16_t regLen, uint8_t responseLen, uint8_t valueType) -> bool {
    std::vector<uint8_t> command = packetCommandSchneider(addressStr, regLen);
    std::vector<uint8_t> response(responseLen);

    if (!sendModbusRequest(command, response, 500)) return false;

    if (valueType == 0) voltage = bytes4ToFloat(&response[3]);
    if (valueType == 1) energy  = bytes8ToEnergy(&response[3]);
    if (valueType == 2) current = bytes4ToFloat(&response[3]);
    if (valueType == 3) power   = bytes4ToFloat(&response[3]);

    return true;
  };

  bool voltageOk = readModbusSchneider("3028", 2, 9, 0);
  delay(100);
  bool energyOk  = readModbusSchneider("3204", 4, 13, 1);
  delay(100);
  bool currentOk = readModbusSchneider("3010", 2, 9, 2);
  delay(100);
  bool powerOk   = readModbusSchneider("3060", 2, 9, 3);

  return energyOk;
}

// =====================================================
// PACKET HELPERS & UTILS
// =====================================================
std::vector<uint8_t> packetCommandCircutor(uint16_t hexAddress, uint16_t regLen) {
  std::vector<uint8_t> packet;
  packet.push_back(0x01);
  packet.push_back(0x03);
  packet.push_back((hexAddress >> 8) & 0xFF);
  packet.push_back(hexAddress & 0xFF);
  packet.push_back((regLen >> 8) & 0xFF);
  packet.push_back(regLen & 0xFF);
  uint16_t crc = calculateCRC(packet.data(), packet.size());
  packet.push_back(crc & 0xFF);
  packet.push_back((crc >> 8) & 0xFF);
  return packet;
}

std::vector<uint8_t> packetCommandSchneider(const String& addressStr, uint16_t lengthValue) {
  std::vector<uint8_t> packet;
  packet.push_back(0x01);
  packet.push_back(0x03);
  uint16_t address = static_cast<uint16_t>(addressStr.toInt()) - 1;
  packet.push_back((address >> 8) & 0xFF);
  packet.push_back(address & 0xFF);
  packet.push_back((lengthValue >> 8) & 0xFF);
  packet.push_back(lengthValue & 0xFF);
  uint16_t crc = calculateCRC(packet.data(), packet.size());
  packet.push_back(crc & 0xFF);
  packet.push_back((crc >> 8) & 0xFF);
  return packet;
}

uint16_t calculateCRC(const uint8_t* data, uint8_t length) {
  uint16_t crc = 0xFFFF;
  for (uint8_t pos = 0; pos < length; pos++) {
    crc ^= data[pos];
    for (uint8_t i = 0; i < 8; i++) {
      crc = (crc & 1) ? (crc >> 1) ^ 0xA001 : crc >> 1;
    }
  }
  return crc;
}

float bytes4ToFloat(const uint8_t* data) {
  union { uint8_t bytes[4]; float value; } converter;
  converter.bytes[3] = data[0];
  converter.bytes[2] = data[1];
  converter.bytes[1] = data[2];
  converter.bytes[0] = data[3];
  return converter.value;
}

uint32_t bytes4ToUint32(const uint8_t* data) {
  return (static_cast<uint32_t>(data[0]) << 24) |
         (static_cast<uint32_t>(data[1]) << 16) |
         (static_cast<uint32_t>(data[2]) << 8)  |
         (static_cast<uint32_t>(data[3]));
}

float bytes8ToEnergy(const uint8_t* data) {
  uint64_t rawValue = 0;
  for (uint8_t i = 0; i < 8; i++) {
    rawValue = (rawValue << 8) | data[i];
  }
  return static_cast<float>(rawValue);
}

void addLog(const String& message) {
  Serial.println(message);
  logsBuffer += message + "\n";
  if (logsBuffer.length() > 2000) {
    logsBuffer = logsBuffer.substring(logsBuffer.length() - 1500);
  }
}
