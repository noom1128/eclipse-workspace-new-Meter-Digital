#include <WiFi.h>
#include <HTTPClient.h>
#include <WebServer.h>
#include <ElegantOTA.h>
#include <EthernetENC.h>
#include <SPI.h>
#include <vector>

#define MAX485_CONTROL_PIN 4
#define RS485_TRANSMIT HIGH
#define RS485_RECEIVE LOW

// =====================================================
// Meter Selection Control
// =====================================================
enum MeterModel {
  METER_SCHNEIDER_PM2200,
  METER_CIRCUTOR_CVMC5
};

// เลือกใช้อ่านค่าจากมิเตอร์รุ่นที่ต้องการ
// (ตั้งค่าเริ่มต้นเป็น Circutor CVM-C5 และปิด Schneider ไว้ตามต้องการ)
MeterModel currentMeterModel = METER_CIRCUTOR_CVMC5;

// =====================================================
// Wi-Fi Config
// =====================================================
const char* ssid = "Galaxy A15 5G 4A21";
const char* password = "0859631128";

// =====================================================
// Server Config
// =====================================================
const char* serverIP = "10.28.241.108";
const int serverPort = 8080;
const char* roomNumber = "101";
const char* meterId = "METER001";

String sendDataUrl = "http://" + String(serverIP) + ":" + String(serverPort) + "/api/meter";
String networkModeUrl = "http://" + String(serverIP) + ":" + String(serverPort) + "/api/meter/network-mode?meterId=" + String(meterId);

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

// Log Buffer สำหรับแสดงหน้า Web /log
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

// =====================================================
// SETUP
// =====================================================
void setup() {
  Serial.begin(115200);
  delay(500);

  addLog("======================================");
  addLog("ESP32 Meter Bridge Starting");
  addLog("Active Meter: " + String(currentMeterModel == METER_CIRCUTOR_CVMC5 ? "CIRCUTOR CVM-C5" : "SCHNEIDER PM2200"));
  addLog("======================================");

  // RS485 Initialization
  pinMode(MAX485_CONTROL_PIN, OUTPUT);
  digitalWrite(MAX485_CONTROL_PIN, RS485_RECEIVE);
  Serial2.begin(9600, SERIAL_8N1, 16, 17);

  // Wi-Fi Connection
  connectWiFi();

  // Ethernet ENC28J60 Initialization
  setupEthernet();

  // Web Server Routes
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
    message += "\nSwitch meter via Web URL:\n";
    message += "  /set-meter?model=circutor\n";
    message += "  /set-meter?model=schneider\n";
    server.send(200, "text/plain", message);
  });

  server.on("/set-meter", []() {
    if (server.hasArg("model")) {
      String modelArg = server.arg("model");
      modelArg.toLowerCase();
      if (modelArg == "circutor") {
        currentMeterModel = METER_CIRCUTOR_CVMC5;
        addLog("Meter model switched to: CIRCUTOR CVM-C5");
        server.send(200, "text/plain", "Switched active meter to CIRCUTOR CVM-C5");
        return;
      } else if (modelArg == "schneider") {
        currentMeterModel = METER_SCHNEIDER_PM2200;
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

  ElegantOTA.begin(&server);
  server.begin();

  addLog("POST target: " + sendDataUrl);
  addLog("Default Network Mode: WIFI");
}

// =====================================================
// MAIN LOOP
// =====================================================
void loop() {
  server.handleClient();
  ElegantOTA.loop();

  maintainWiFi();

  // ตรวจสอบ Network Mode จาก Server ทุกๆ 30 วินาที
  if (millis() - lastNetworkModeCheck >= networkModeCheckInterval) {
    lastNetworkModeCheck = millis();
    checkNetworkMode();
  }

  // อ่านค่าจาก Meter ทุกๆ 10 วินาที
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
// NETWORK SEND & CONTROLS (WITH FAILSAFE FALLBACK)
// =====================================================
void sendReading() {
  // ตรวจสอบว่า LAN พร้อมใช้งานจริงหรือไม่ (มี IP และต่อสาย)
  bool isLanReady = (Ethernet.linkStatus() != LinkOFF) && (Ethernet.localIP() != IPAddress(0, 0, 0, 0));

  if (currentNetworkMode == MODE_LAN && isLanReady) {
    sendReadingLan();
  } else {
    if (currentNetworkMode == MODE_LAN && !isLanReady) {
      addLog("LAN not ready (No IP or Link OFF). Failsafe fallback -> Sending via Wi-Fi");
    }
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
  if (Ethernet.linkStatus() == LinkOFF) {
    addLog("LAN cable disconnected; reading was not sent.");
    return;
  }

  IPAddress ip = Ethernet.localIP();
  if (ip == IPAddress(0, 0, 0, 0)) {
    addLog("LAN has no IP address.");
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
        addLog("LAN Client timeout waiting for server response.");
        ethernetClient.stop();
        return;
      }
    }

    String statusLine = ethernetClient.readStringUntil('\n');
    addLog("LAN Data Sent: " + json + " | Status: " + statusLine);
    ethernetClient.stop();
  } else {
    addLog("LAN connection to server failed!");
  }
}

void checkNetworkMode() {
  if (WiFi.status() != WL_CONNECTED) {
    return;
  }

  WiFiClient client;
  HTTPClient http;
  http.setConnectTimeout(3000);
  http.setTimeout(3000);

  if (!http.begin(client, networkModeUrl)) {
    return;
  }

  int httpCode = http.GET();
  if (httpCode == 200) {
    String response = http.getString();
    response.trim();

    if (response.indexOf("\"LAN\"") >= 0 && currentNetworkMode != MODE_LAN) {
      setNetworkMode(MODE_LAN);
    } else if (response.indexOf("\"WIFI\"") >= 0 && currentNetworkMode != MODE_WIFI) {
      setNetworkMode(MODE_WIFI);
    }
  }
  http.end();
}

void setNetworkMode(NetworkMode mode) {
  currentNetworkMode = mode;
  addLog("====================================");
  addLog(String("NETWORK MODE CHANGED -> ") + (mode == MODE_WIFI ? "WIFI" : "LAN"));
  if (mode == MODE_LAN) {
    addLog("LAN IP: " + Ethernet.localIP().toString());
  }
  addLog("====================================");
}

void setupEthernet() {
  addLog("Initializing ENC28J60...");
  SPI.begin(ETH_SCK_PIN, ETH_MISO_PIN, ETH_MOSI_PIN, ETH_CS_PIN);
  Ethernet.init(ETH_CS_PIN);

  if (Ethernet.hardwareStatus() == EthernetNoHardware) {
    addLog("ENC28J60 hardware not found. Ethernet disabled (continuing on Wi-Fi).");
    return;
  }

  if (Ethernet.begin(ethMac, 3000, 1000) == 0) {
    addLog("ENC28J60 DHCP timeout / failed. Continuing on Wi-Fi.");
  } else {
    addLog("ENC28J60 connected successfully.");
    addLog("LAN IP: " + Ethernet.localIP().toString());
    addLog("LAN Gateway: " + Ethernet.gatewayIP().toString());
  }
}

void connectWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  addLog("Connecting to Wi-Fi...");

  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - start > 15000) {
      addLog("Wi-Fi connection timeout.");
      return;
    }
    delay(500);
  }
  addLog("Wi-Fi connected. IP: " + WiFi.localIP().toString());
}

void maintainWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  if (millis() - lastWiFiReconnect >= wifiReconnectInterval) {
    lastWiFiReconnect = millis();
    addLog("Wi-Fi disconnected. Reconnecting...");
    WiFi.disconnect();
    WiFi.begin(ssid, password);
  }
}

// =====================================================
// MODBUS RTU GENERIC REQUEST HANDLER
// =====================================================
bool sendModbusRequest(const std::vector<uint8_t>& command, std::vector<uint8_t>& response, uint32_t timeoutMs) {
  while (Serial2.available() > 0) {
    Serial2.read();
  }

  digitalWrite(MAX485_CONTROL_PIN, RS485_TRANSMIT);
  Serial2.write(command.data(), command.size());
  Serial2.flush();
  delayMicroseconds(500); // รอให้ Stop Bit ของ RS485 ส่งออกครบสมบูรณ์ก่อนสลับโหมด
  digitalWrite(MAX485_CONTROL_PIN, RS485_RECEIVE);

  size_t index = 0;
  unsigned long startTime = millis();
  while (millis() - startTime < timeoutMs && index < response.size()) {
    if (Serial2.available()) {
      response[index++] = static_cast<uint8_t>(Serial2.read());
    }
  }

  if (index < response.size()) {
    return false;
  }

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
// CIRCUTOR CVM-C5 MODBUS RTU READ
// =====================================================
bool readCircutorCVMC5() {
  addLog("[Circutor CVM-C5] Reading values...");

  // 1. อ่านแบบ Block Read (Voltage 0x0000, Current 0x0002, Active Power 0x0004) -> 6 Registers (12 Bytes data)
  std::vector<uint8_t> cmdVAP = packetCommandCircutor(0x0000, 6);
  std::vector<uint8_t> respVAP(17); // 3 bytes header + 12 bytes payload + 2 bytes CRC = 17 bytes

  bool blockOk = sendModbusRequest(cmdVAP, respVAP, 500);
  if (blockOk && respVAP.size() >= 17) {
    uint32_t rawV = bytes4ToUint32(&respVAP[3]);  // 0x0000-0x0001
    uint32_t rawA = bytes4ToUint32(&respVAP[7]);  // 0x0002-0x0003
    uint32_t rawP = bytes4ToUint32(&respVAP[11]); // 0x0004-0x0005

    voltage = rawV / 10.0f;       // V ÷ 10
    current = rawA / 1000.0f;     // mA -> A
    power   = static_cast<float>(rawP); // W
    addLog("[Circutor] Block Read OK | Voltage: " + String(voltage, 2) + "V, Current: " + String(current, 3) + "A, Power: " + String(power, 2) + "W");
  } else {
    addLog("[Circutor] Block Read failed; attempting individual register reads...");

    // Fallback: Read Voltage L1 (0x0000)
    std::vector<uint8_t> cmdV = packetCommandCircutor(0x0000, 2);
    std::vector<uint8_t> respV(9);
    if (sendModbusRequest(cmdV, respV, 500)) {
      voltage = bytes4ToUint32(&respV[3]) / 10.0f;
    }

    // Fallback: Read Current L1 (0x0002)
    std::vector<uint8_t> cmdA = packetCommandCircutor(0x0002, 2);
    std::vector<uint8_t> respA(9);
    if (sendModbusRequest(cmdA, respA, 500)) {
      current = bytes4ToUint32(&respA[3]) / 1000.0f;
    }

    // Fallback: Read Active Power L1 (0x0004)
    std::vector<uint8_t> cmdP = packetCommandCircutor(0x0004, 2);
    std::vector<uint8_t> respP(9);
    if (sendModbusRequest(cmdP, respP, 500)) {
      power = static_cast<float>(bytes4ToUint32(&respP[3]));
    }
  }
  delay(50);

  // 2. อ่าน Frequency (0x0028)
  std::vector<uint8_t> cmdHz = packetCommandCircutor(0x0028, 2);
  std::vector<uint8_t> respHz(9);
  if (sendModbusRequest(cmdHz, respHz, 500)) {
    uint32_t rawHz = bytes4ToUint32(&respHz[3]);
    frequency = rawHz / 10.0f; // Hz ÷ 10
  }
  delay(50);

  // 3. อ่าน Energy Tariff 1 (0x003C) & Tariff 2 (0x006C) ในหน่วย Wh แล้วหาร 1000 เป็น kWh
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
    energy = t1_kWh + t2_kWh; // รวมค่าพลังงาน T1 + T2 (kWh)
    addLog("[Circutor] Energy T1: " + String(t1_kWh, 3) + " kWh | T2: " + String(t2_kWh, 3) + " kWh | Total: " + String(energy, 3) + " kWh");
  } else {
    addLog("[Circutor] Energy (kWh) read failed!");
  }

  return energyOk;
}

// =====================================================
// SCHNEIDER PM2200 MODBUS RTU READ (INACTIVE BY DEFAULT)
// =====================================================
bool readSchneiderPM2200() {
  addLog("[Schneider PM2200] Reading values...");

  auto readModbusSchneider = [](const String& addressStr, uint16_t regLen, uint8_t responseLen, uint8_t valueType) -> bool {
    std::vector<uint8_t> command = packetCommandSchneider(addressStr, regLen);
    std::vector<uint8_t> response(responseLen);

    if (!sendModbusRequest(command, response, 500)) {
      return false;
    }

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

  if (!energyOk) {
    addLog("[Schneider] Energy read failed.");
    return false;
  }

  if (!voltageOk || !currentOk || !powerOk) {
    addLog("[Schneider] Some instantaneous readings failed; using available values.");
  }

  return true;
}

// =====================================================
// MODBUS PACKET HELPERS
// =====================================================
std::vector<uint8_t> packetCommandCircutor(uint16_t hexAddress, uint16_t regLen) {
  std::vector<uint8_t> packet;
  packet.push_back(0x01); // Slave ID
  packet.push_back(0x03); // Function Code 0x03

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
  union {
    uint8_t bytes[4];
    float value;
  } converter;

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
