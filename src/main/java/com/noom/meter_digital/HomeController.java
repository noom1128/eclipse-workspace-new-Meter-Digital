package com.noom.meter_digital;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String home() {
        String[] paths = { "/dashboard.html", "/static/dashboard.html", "/templates/dashboard.html",
                "/public/dashboard.html" };
        for (String path : paths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return injectAdminAndRoomScript(html);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return getInlineDashboardHtml(null);
    }

    @GetMapping(value = "/old-dashboard", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String oldDashboard() {
        return home();
    }

    @GetMapping(value = { "/download", "/download.html", "/landing", "/store",
            "/product" }, produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String downloadLanding() {
        String[] paths = { "/download.html", "/static/download.html", "/public/download.html" };
        for (String path : paths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return "<h1>Meter Digital Pro Download Center</h1>";
    }

    @GetMapping(value = "/api/download/setup-exe", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> downloadSetupExe() {
        try {
            java.io.File file = new java.io.File("dist/MeterDigitalPro-Setup.zip");
            if (!file.exists()) {
                file = new java.io.File("dist/MeterDigitalSetup.exe");
            }
            if (!file.exists()) {
                file = new java.io.File("target/meter-digital-0.0.1-SNAPSHOT.jar");
            }
            if (!file.exists()) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String filename = file.getName();
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(bytes);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = { "/maintenance", "/meter-gateway-setup",
            "/gateway-setup", "/config", "/meter-config" }, produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String maintenance() {
        return getInlineMaintenanceHtml();
    }

    @GetMapping(value = { "/meter-reader", "/meter-reader.html", "/mobile-reader",
            "/mobile-reader.html" }, produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String meterReader() {
        return getInlineMeterReaderHtml();
    }

    @GetMapping(value = { "/modbus-debugger", "/modbus-remote-tool" }, produces = MediaType.TEXT_HTML_VALUE
            + ";charset=UTF-8")
    @ResponseBody
    public String modbusDebugger() {
        String[] paths = { "/modbus-remote-tool.html", "/static/modbus-remote-tool.html",
                "/public/modbus-remote-tool.html" };
        for (String path : paths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return getInlineModbusDebuggerHtml();
    }

    @GetMapping(value = "/live-dashboard", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String liveDashboard(@RequestParam(required = false, defaultValue = "101") String roomNumber) {
        return getInlineDashboardHtml(roomNumber);
    }

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String login() {
        String[] paths = { "/login.html", "/static/login.html", "/templates/login.html", "/public/login.html" };
        for (String path : paths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    return injectLoginTokenStoreScript(html);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return injectLoginTokenStoreScript(getInlineLoginHtml());
    }

    @GetMapping(value = "/billing", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String billing() {
        try (InputStream is = getClass().getResourceAsStream("/billing.html")) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<h1>Billing page unavailable</h1>";
    }

    private String injectLoginTokenStoreScript(String originalHtml) {
        String script = "<script>"
                + "(function() {"
                + "  const originalFetch = window.fetch;"
                + "  if (originalFetch) {"
                + "    window.fetch = async function() {"
                + "      const response = await originalFetch.apply(this, arguments);"
                + "      try {"
                + "        const clone = response.clone();"
                + "        const data = await clone.json();"
                + "        if (data && data.token) {"
                + "          localStorage.setItem('token', data.token);"
                + "          localStorage.setItem('role', data.role || 'ROLE_ADMIN');"
                + "          localStorage.setItem('username', data.username || '');"
                + "        }"
                + "      } catch (e) {}"
                + "      return response;"
                + "    };"
                + "  }"
                + "})();"
                + "</script>";

        if (originalHtml.contains("</body>")) {
            return originalHtml.replace("</body>", script + "</body>");
        }
        return originalHtml + script;
    }

    private String injectAdminAndRoomScript(String originalHtml) {
        String script = "<script>"
                + "document.addEventListener('DOMContentLoaded', function() {"
                + "  const token = localStorage.getItem('token');"
                + "  const role = localStorage.getItem('role');"
                + "  injectAdminRoomControls(token, role);"
                + "});"
                + "function injectAdminRoomControls(token, role) {"
                + "  if (document.getElementById('adminRoomBar')) return;"
                + "  const bar = document.createElement('div');"
                + "  bar.id = 'adminRoomBar';"
                + "  bar.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:99999;display:flex;gap:10px;background:rgba(15,23,42,0.95);border:2px solid #00f2fe;padding:12px 20px;border-radius:18px;box-shadow:0 12px 30px rgba(0,0,0,0.6);color:#fff;align-items:center;font-family:sans-serif;backdrop-filter:blur(10px);';"
                + "  if (token && role === 'ROLE_ADMIN') {"
                + "    bar.innerHTML = '<span>👑 <b>Admin Mode Active</b></span>' +"
                + "      '<button id=\"btnGatewaySetup\" style=\"background:linear-gradient(135deg,#10b981,#059669);color:#fff;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">🛠️ Meter Gateway Setup</button>' +"
                + "      '<button id=\"btnModbusDebuggerNav\" style=\"background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">🔬 Remote Scanner & Debugger</button>' +"
                + "      '<button id=\"btnMeterReaderNav\" style=\"background:linear-gradient(135deg,#f59e0b,#d97706);color:#0f172a;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">📲 เดินจดมิเตอร์</button>' +"
                + "      '<button id=\"btnLiveDashboardNav\" style=\"background:linear-gradient(135deg,#8b5cf6,#6d28d9);color:#fff;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">⚡ Realtime Monitor</button>' +"
                + "      '<button id=\"btnLogoutAdmin\" style=\"background:#ef4444;color:#fff;border:none;padding:8px 12px;border-radius:12px;cursor:pointer;font-weight:600;\">ออกจากระบบ</button>';"
                + "    document.body.appendChild(bar);"
                + "    document.getElementById('btnGatewaySetup').onclick = function() { window.location.href = '/meter-gateway-setup'; };"
                + "    document.getElementById('btnModbusDebuggerNav').onclick = function() { window.location.href = '/modbus-debugger'; };"
                + "    document.getElementById('btnMeterReaderNav').onclick = function() { window.location.href = '/meter-reader'; };"
                + "    document.getElementById('btnLiveDashboardNav').onclick = function() { window.location.href = '/live-dashboard?roomNumber=101'; };"
                + "    document.getElementById('btnLogoutAdmin').onclick = function() { localStorage.clear(); window.location.reload(); };"
                + "  } else {"
                + "    bar.innerHTML = '<span>🔒 ยังไม่ได้เข้าสู่ระบบ Admin</span>' +"
                + "      '<button id=\"btnMeterReaderGuest\" style=\"background:linear-gradient(135deg,#f59e0b,#d97706);color:#0f172a;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:700;\">📲 สแกนทดสอบมิเตอร์</button>' +"
                + "      '<button id=\"btnGatewaySetupGuest\" style=\"background:linear-gradient(135deg,#10b981,#059669);color:#fff;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:600;\">🛠️ Meter Gateway Setup</button>' +"
                + "      '<button id=\"btnModbusDebuggerGuest\" style=\"background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:600;\">🔬 Remote Modbus Tool</button>' +"
                + "      '<button id=\"btnLoginAdmin\" style=\"background:#0284c7;color:#fff;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:600;\">🔑 ไปหน้าล็อกอิน Admin</button>';"
                + "    document.body.appendChild(bar);"
                + "    document.getElementById('btnMeterReaderGuest').onclick = function() { window.location.href = '/meter-reader'; };"
                + "    document.getElementById('btnGatewaySetupGuest').onclick = function() { window.location.href = '/meter-gateway-setup'; };"
                + "    document.getElementById('btnModbusDebuggerGuest').onclick = function() { window.location.href = '/modbus-debugger'; };"
                + "    document.getElementById('btnLoginAdmin').onclick = function() { window.location.href = '/login'; };"
                + "  }"
                + "}"
                + "</script>";

        if (originalHtml.contains("</body>")) {
            return originalHtml.replace("</body>", script + "</body>");
        }
        return originalHtml + script;
    }

    private String getInlineLoginHtml() {
        return "<h1>Login Page</h1>";
    }

    private String getInlineDashboardHtml(String roomParam) {
        return "<h1>Dashboard Page</h1>";
    }

    private String getInlineMaintenanceHtml() {
        return "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>🛠️ Meter Gateway Setup & Configuration</title>"
                + "<link href='https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;600;700&display=swap' rel='stylesheet'>"
                + "<style>"
                + "* { box-sizing: border-box; font-family: 'Kanit', sans-serif; transition: all 0.2s ease; }"
                + "body { background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; min-height: 100vh; }"
                + ".container { max-width: 1100px; margin: 0 auto; }"
                + ".header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; background: rgba(30,41,59,0.8); padding: 16px 24px; border-radius: 16px; border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); }"
                + ".header h1 { margin: 0; font-size: 1.5rem; color: #38bdf8; display: flex; align-items: center; gap: 10px; }"
                + ".btn-nav { background: #334155; color: #f8fafc; text-decoration: none; padding: 8px 16px; border-radius: 10px; font-weight: 600; font-size: 0.9rem; }"
                + ".btn-nav:hover { background: #475569; color: #38bdf8; }"
                + ".card { background: rgba(30,41,59,0.7); border: 1px solid rgba(255,255,255,0.1); border-radius: 18px; padding: 24px; margin-bottom: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.3); backdrop-filter: blur(10px); }"
                + ".card-title { font-size: 1.2rem; font-weight: 700; color: #f1f5f9; margin-top: 0; margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 10px; display: flex; align-items: center; gap: 8px; }"
                + ".grid-form { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 20px; }"
                + ".form-group { display: flex; flex-direction: column; gap: 6px; }"
                + ".form-group label { font-size: 0.85rem; color: #94a3b8; font-weight: 600; }"
                + ".form-group input, .form-group select { background: #0f172a; border: 1px solid #334155; color: #f8fafc; padding: 10px 14px; border-radius: 10px; font-size: 0.95rem; outline: none; }"
                + ".form-group input:focus, .form-group select:focus { border-color: #38bdf8; box-shadow: 0 0 0 2px rgba(56,189,248,0.2); }"
                + ".btn-actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 10px; }"
                + ".btn { padding: 12px 20px; border-radius: 12px; border: none; font-weight: 700; font-size: 0.95rem; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; }"
                + ".btn-primary { background: linear-gradient(135deg, #0284c7, #2563eb); color: #fff; box-shadow: 0 4px 12px rgba(37,99,235,0.4); }"
                + ".btn-primary:hover { opacity: 0.9; transform: translateY(-2px); }"
                + ".btn-success { background: linear-gradient(135deg, #10b981, #059669); color: #fff; box-shadow: 0 4px 12px rgba(16,185,129,0.4); }"
                + ".btn-success:hover { opacity: 0.9; transform: translateY(-2px); }"
                + ".result-box { margin-top: 20px; padding: 16px; border-radius: 14px; background: #0f172a; border: 1px solid #334155; display: none; }"
                + ".result-success { border-color: #10b981; background: rgba(16,185,129,0.1); color: #6ee7b7; }"
                + ".result-error { border-color: #ef4444; background: rgba(239,68,68,0.1); color: #fca5a5; }"
                + ".telemetry-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 12px; text-align: center; }"
                + ".telemetry-item { background: rgba(15,23,42,0.6); padding: 12px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.05); }"
                + ".telemetry-val { font-size: 1.4rem; font-weight: 700; color: #38bdf8; }"
                + ".telemetry-lbl { font-size: 0.75rem; color: #94a3b8; }"
                + "table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 0.9rem; }"
                + "th, td { padding: 12px 14px; text-align: left; border-bottom: 1px solid rgba(255,255,255,0.08); }"
                + "th { background: rgba(15,23,42,0.8); color: #94a3b8; font-weight: 600; }"
                + ".badge { padding: 4px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; display: inline-block; }"
                + ".badge-online { background: rgba(16,185,129,0.2); color: #34d399; border: 1px solid #10b981; }"
                + ".badge-offline { background: rgba(239,68,68,0.2); color: #f87171; border: 1px solid #ef4444; }"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "  <h1>🛠️ Meter Gateway Setup & Configuration</h1>"
                + "  <div style='display:flex;gap:10px;'>"
                + "    <a href='/' class='btn-nav'>🏠 Dashboard</a>"
                + "    <a href='/modbus-debugger' class='btn-nav'>🔬 Modbus Debugger</a>"
                + "  </div>"
                + "</div>"
                + "<div class='card'>"
                + "  <div class='card-title'>⚙️ ตั้งค่าอุปกรณ์เชื่อมต่อมิตเตอร์ (Moxa / Gateway Config)</div>"
                + "  <form id='gatewayForm'>"
                + "    <div class='grid-form'>"
                + "      <div class='form-group'>"
                + "        <label>Device ID (ชื่ออุปกรณ์)</label>"
                + "        <input type='text' id='deviceId' value='MOXA-GATEWAY-01' required>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>Target Room Number (ห้องที่ติดตั้ง)</label>"
                + "        <input type='text' id='targetRoomNumber' value='101' required>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>IP Address (Moxa IP)</label>"
                + "        <input type='text' id='ipAddress' value='192.168.127.254' required>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>Port (Local TCP Port)</label>"
                + "        <input type='number' id='port' value='4001' required>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>Slave ID (ID มิตเตอร์)</label>"
                + "        <input type='number' id='slaveId' value='1' required>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>Reader Mode (โหมดอุปกรณ์)</label>"
                + "        <select id='readerMode'>"
                + "          <option value='MODE_MOXA_SERIAL' selected>MOXA Gateway (Modbus Serial/TCP)</option>"
                + "          <option value='MODE_MODBUS_WIFI'>RS485 to Wi-Fi Converter</option>"
                + "          <option value='MODE_MODBUS_LAN'>RS485 to LAN Converter</option>"
                + "          <option value='MODE_ESP32_WIFI'>ESP32 (Wi-Fi Direct)</option>"
                + "        </select>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>Protocol Mode</label>"
                + "        <select id='protocolMode'>"
                + "          <option value='MODBUS_RTU_OVER_TCP' selected>MODBUS RTU OVER TCP (Moxa Default)</option>"
                + "          <option value='MODBUS_TCP'>MODBUS TCP (Standard Port 502)</option>"
                + "          <option value='AUTO'>AUTO Detect</option>"
                + "        </select>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>Active Meter Model (รุ่นมิตเตอร์)</label>"
                + "        <select id='activeMeterModel'>"
                + "          <option value='CIRCUTOR_CVMC5' selected>Circutor CVM-C5 Power Analyzer (แนะนำตรงรุ่น)</option>"
                + "          <option value='SCHNEIDER_PM2200'>Schneider PM2200 / PM2100</option>"
                + "          <option value='EASTRON_SDM120'>Eastron SDM120 Single Phase</option>"
                + "          <option value='EASTRON_SDM630'>Eastron SDM630 Three Phase</option>"
                + "          <option value='CHINT_DTSU666'>Chint DTSU666 Smart Energy Meter</option>"
                + "          <option value='ACREL_DTSD1352'>Acrel DTSD1352 / SDM Meter</option>"
                + "          <option value='MODEL_A'>Generic Float32 Meter (MODEL_A)</option>"
                + "        </select>"
                + "      </div>"
                + "      <div class='form-group'>"
                + "        <label>Polling Interval (วินาที)</label>"
                + "        <input type='number' id='pollingIntervalSeconds' value='10'>"
                + "      </div>"
                + "    </div>"
                + "    <div class='btn-actions'>"
                + "      <button type='button' class='btn btn-primary' onclick='testConnection()'>⚡ ทดสอบการเชื่อมต่อ (Test Connection)</button>"
                + "      <button type='button' class='btn btn-success' onclick='saveConfig()'>💾 บันทึกการตั้งค่าเกตเวย์ (Save Config)</button>"
                + "    </div>"
                + "  </form>"
                + "  <div id='resultBox' class='result-box'></div>"
                + "</div>"
                + "<div class='card'>"
                + "  <div class='card-title'>📋 รายการอุปกรณ์เกตเวย์ในระบบ (Configured Devices)</div>"
                + "  <table id='devicesTable'>"
                + "    <thead><tr><th>Device ID</th><th>Target Room</th><th>IP Address:Port</th><th>Slave ID</th><th>Mode</th><th>Status</th><th>Action</th></tr></thead>"
                + "    <tbody id='devicesList'><tr><td colspan='7' style='text-align:center;'>กำลังโหลดข้อมูล...</td></tr></tbody>"
                + "  </table>"
                + "</div>"
                + "</div>"
                + "<script>"
                + "async function loadDevices() {"
                + "  try {"
                + "    const res = await fetch('/api/v1/meter-configs');"
                + "    const data = await res.json();"
                + "    const tbody = document.getElementById('devicesList');"
                + "    if (!data || data.length === 0) {"
                + "      tbody.innerHTML = '<tr><td colspan=\"7\" style=\"text-align:center;\">ยังไม่มีการเพิ่มอุปกรณ์</td></tr>';"
                + "      return;"
                + "    }"
                + "    tbody.innerHTML = data.map(d => `<tr>"
                + "      <td><b>${d.deviceId || '-'}</b></td>"
                + "      <td>ห้อง ${d.targetRoomNumber || '-'}</td>"
                + "      <td><code>${d.ipAddress || '-'}:${d.port || 502}</code></td>"
                + "      <td>${d.slaveId || 1}</td>"
                + "      <td>${d.readerMode || '-'}</td>"
                + "      <td><span class=\"badge ${d.status === 'ONLINE' ? 'badge-online' : 'badge-offline'}\">${d.status || 'OFFLINE'}</span></td>"
                + "      <td>"
                + "        <button style=\"background:#0284c7;color:#fff;border:none;padding:4px 8px;border-radius:6px;cursor:pointer;\" onclick=\"testConnectionById(${d.id})\">🧪 Test</button> "
                + "        <button style=\"background:#ef4444;color:#fff;border:none;padding:4px 8px;border-radius:6px;cursor:pointer;\" onclick=\"deleteDevice(${d.id})\">🗑️</button>"
                + "      </td>"
                + "    </tr>`).join('');"
                + "  } catch(e) { console.error(e); }"
                + "}"
                + "async function testConnection() {"
                + "  const box = document.getElementById('resultBox');"
                + "  box.style.display = 'block';"
                + "  box.className = 'result-box';"
                + "  box.innerHTML = '⏳ กำลังส่งแพ็กเกจทดสอบไปยัง Moxa...';"
                + "  const payload = {"
                + "    ipAddress: document.getElementById('ipAddress').value,"
                + "    port: parseInt(document.getElementById('port').value) || 4045,"
                + "    slaveId: parseInt(document.getElementById('slaveId').value) || 1,"
                + "    activeMeterModel: document.getElementById('activeMeterModel').value,"
                + "    protocolMode: document.getElementById('protocolMode').value"
                + "  };"
                + "  try {"
                + "    const res = await fetch('/api/v1/meter-configs/test-connection', {"
                + "      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)"
                + "    });"
                + "    const data = await res.json();"
                + "    if (data.success) {"
                + "      box.className = 'result-box result-success';"
                + "      box.innerHTML = `<div>✅ <b>${data.message}</b> (${data.responseTimeMs} ms) [Protocol: ${data.protocolUsed}]</div>` +"
                + "        `<div class=\"telemetry-grid\">` +"
                + "        `<div class=\"telemetry-item\"><div class=\"telemetry-val\">${data.voltage || 0} V</div><div class=\"telemetry-lbl\">แรงดันไฟฟ้า</div></div>` +"
                + "        `<div class=\"telemetry-item\"><div class=\"telemetry-val\">${data.current || 0} A</div><div class=\"telemetry-lbl\">กระแสไฟฟ้า</div></div>` +"
                + "        `<div class=\"telemetry-item\"><div class=\"telemetry-val\">${data.power || 0} kW</div><div class=\"telemetry-lbl\">กำลังไฟฟ้า</div></div>` +"
                + "        `<div class=\"telemetry-item\"><div class=\"telemetry-val\">${data.energyKwh || 0} kWh</div><div class=\"telemetry-lbl\">หน่วยไฟฟ้าสะสม</div></div>` +"
                + "        `</div>`;"
                + "    } else {"
                + "      box.className = 'result-box result-error';"
                + "      box.innerHTML = `❌ <b>การเชื่อมต่อไม่สำเร็จ:</b> ${data.message} (${data.responseTimeMs || 0} ms)`;"
                + "    }"
                + "  } catch(e) {"
                + "    box.className = 'result-box result-error';"
                + "    box.innerHTML = '❌ เกิดข้อผิดพลาดในการส่งคำขอ: ' + e.message;"
                + "  }"
                + "}"
                + "async function saveConfig() {"
                + "  const payload = {"
                + "    deviceId: document.getElementById('deviceId').value,"
                + "    targetRoomNumber: document.getElementById('targetRoomNumber').value,"
                + "    ipAddress: document.getElementById('ipAddress').value,"
                + "    port: parseInt(document.getElementById('port').value) || 4045,"
                + "    slaveId: parseInt(document.getElementById('slaveId').value) || 1,"
                + "    readerMode: document.getElementById('readerMode').value,"
                + "    protocolMode: document.getElementById('protocolMode').value,"
                + "    activeMeterModel: document.getElementById('activeMeterModel').value,"
                + "    pollingIntervalSeconds: parseInt(document.getElementById('pollingIntervalSeconds').value) || 10"
                + "  };"
                + "  try {"
                + "    const res = await fetch('/api/v1/meter-configs', {"
                + "      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)"
                + "    });"
                + "    if (res.ok) { alert('บันทึกการตั้งค่า Moxa เกตเวย์สำเร็จ!'); loadDevices(); }"
                + "    else { alert('ไม่สามารถบันทึกได้'); }"
                + "  } catch(e) { alert('Error: ' + e.message); }"
                + "}"
                + "async function testConnectionById(id) {"
                + "  const res = await fetch('/api/v1/meter-configs/' + id + '/test-connection', { method: 'POST' });"
                + "  const data = await res.json();"
                + "  alert((data.success ? '✅ Success: ' : '❌ Failed: ') + data.message);"
                + "}"
                + "async function deleteDevice(id) {"
                + "  if (confirm('ยืนยันลบอุปกรณ์นี้?')) {"
                + "    await fetch('/api/v1/meter-configs/' + id, { method: 'DELETE' });"
                + "    loadDevices();"
                + "  }"
                + "}"
                + "document.addEventListener('DOMContentLoaded', loadDevices);"
                + "</script></body></html>";
    }

    private String getInlineModbusDebuggerHtml() {
        return "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>🔬 Remote Modbus Debugger & Bus Scanner</title>"
                + "<link href='https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;600;700&display=swap' rel='stylesheet'>"
                + "<style>"
                + "* { box-sizing: border-box; font-family: 'Kanit', sans-serif; transition: all 0.2s ease; }"
                + "body { background: #0f172a; color: #f8fafc; margin: 0; padding: 24px; min-height: 100vh; }"
                + ".container { max-width: 1100px; margin: 0 auto; }"
                + ".header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; background: rgba(30,41,59,0.8); padding: 16px 24px; border-radius: 16px; border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); }"
                + ".header h1 { margin: 0; font-size: 1.5rem; color: #00f2fe; display: flex; align-items: center; gap: 10px; }"
                + ".btn-nav { background: #334155; color: #f8fafc; text-decoration: none; padding: 8px 16px; border-radius: 10px; font-weight: 600; font-size: 0.9rem; }"
                + ".btn-nav:hover { background: #475569; color: #00f2fe; }"
                + ".card { background: rgba(30,41,59,0.7); border: 1px solid rgba(255,255,255,0.1); border-radius: 18px; padding: 24px; margin-bottom: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.3); backdrop-filter: blur(10px); }"
                + ".grid-form { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 20px; }"
                + ".form-group { display: flex; flex-direction: column; gap: 6px; }"
                + ".form-group label { font-size: 0.85rem; color: #94a3b8; font-weight: 600; }"
                + ".form-group input, .form-group select { background: #0f172a; border: 1px solid #334155; color: #f8fafc; padding: 10px 14px; border-radius: 10px; font-size: 0.95rem; outline: none; }"
                + ".btn { padding: 12px 20px; border-radius: 12px; border: none; font-weight: 700; font-size: 0.95rem; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; background: linear-gradient(135deg,#00f2fe,#4facfe); color:#0f172a; }"
                + ".btn:hover { opacity: 0.9; transform: translateY(-2px); }"
                + ".output-box { background: #090d16; border: 1px solid #1e293b; border-radius: 14px; padding: 16px; font-family: monospace; color: #38bdf8; font-size: 0.9rem; white-space: pre-wrap; overflow-x: auto; max-height: 400px; margin-top: 16px; }"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "  <h1>🔬 Remote Modbus Debugger & Bus Scanner</h1>"
                + "  <div style='display:flex;gap:10px;'>"
                + "    <a href='/' class='btn-nav'>🏠 Dashboard</a>"
                + "    <a href='/meter-gateway-setup' class='btn-nav'>🛠️ Gateway Setup</a>"
                + "  </div>"
                + "</div>"
                + "<div class='card'>"
                + "  <h3>📡 1. Read Raw Modbus Registers (อ่านค่า Register ดิบ)</h3>"
                + "  <div class='grid-form'>"
                + "    <div class='form-group'><label>IP Address</label><input type='text' id='dbgIp' value='192.168.127.254'></div>"
                + "    <div class='form-group'><label>Port</label><input type='number' id='dbgPort' value='4001'></div>"
                + "    <div class='form-group'><label>Slave ID</label><input type='number' id='dbgSlave' value='1'></div>"
                + "    <div class='form-group'><label>Function Code</label><select id='dbgFc'><option value='3' selected>FC 3 (Read Holding Registers)</option><option value='4'>FC 4 (Read Input Registers)</option></select></div>"
                + "    <div class='form-group'><label>Start Register</label><input type='number' id='dbgStart' value='0'></div>"
                + "    <div class='form-group'><label>Register Count</label><input type='number' id='dbgCount' value='8'></div>"
                + "    <div class='form-group'><label>Protocol</label><select id='dbgProto'><option value='MODBUS_RTU_OVER_TCP' selected>RTU OVER TCP (Moxa)</option><option value='MODBUS_TCP'>MODBUS TCP</option><option value='AUTO'>AUTO</option></select></div>"
                + "  </div>"
                + "  <button class='btn' onclick='readRegisters()'>🔍 อ่านค่า Register ดิบ (Read Raw Registers)</button>"
                + "  <div id='dbgOut' class='output-box' style='display:none;'></div>"
                + "</div>"
                + "<div class='card'>"
                + "  <h3>🔍 2. Bus Slave ID Scanner (สแกนหาไอดีมิตเตอร์บนสาย RS485)</h3>"
                + "  <div class='grid-form'>"
                + "    <div class='form-group'><label>IP Address</label><input type='text' id='scanIp' value='192.168.127.254'></div>"
                + "    <div class='form-group'><label>Port</label><input type='number' id='scanPort' value='4001'></div>"
                + "    <div class='form-group'><label>Start Slave ID</label><input type='number' id='scanStart' value='1'></div>"
                + "    <div class='form-group'><label>End Slave ID</label><input type='number' id='scanEnd' value='16'></div>"
                + "  </div>"
                + "  <button class='btn' onclick='scanSlaves()'>🚀 สแกนหา Slave ID (Scan Bus)</button>"
                + "  <div id='scanOut' class='output-box' style='display:none;'></div>"
                + "</div>"
                + "</div>"
                + "<script>"
                + "async function readRegisters() {"
                + "  const out = document.getElementById('dbgOut');"
                + "  out.style.display = 'block'; out.innerText = '⏳ กำลังอ่านค่า Register จาก Moxa...';"
                + "  const payload = {"
                + "    ipAddress: document.getElementById('dbgIp').value,"
                + "    port: parseInt(document.getElementById('dbgPort').value) || 4045,"
                + "    slaveId: parseInt(document.getElementById('dbgSlave').value) || 1,"
                + "    functionCode: parseInt(document.getElementById('dbgFc').value) || 3,"
                + "    startRegister: parseInt(document.getElementById('dbgStart').value) || 0,"
                + "    registerCount: parseInt(document.getElementById('dbgCount').value) || 8,"
                + "    protocolMode: document.getElementById('dbgProto').value"
                + "  };"
                + "  try {"
                + "    const res = await fetch('/api/v1/modbus-debug/read-registers', {"
                + "      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)"
                + "    });"
                + "    const data = await res.json();"
                + "    out.innerText = JSON.stringify(data, null, 2);"
                + "  } catch(e) { out.innerText = 'Error: ' + e.message; }"
                + "}"
                + "</script></body></html>";
    }

    private String getInlineMeterReaderHtml() {
        return "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>"
                + "<title>📲 มือถือจดมิเตอร์ & OCR / LINE Approval</title>"
                + "<link href='https://fonts.googleapis.com/css2?family=Kanit:wght@300;400;600;700&display=swap' rel='stylesheet'>"
                + "<style>"
                + "* { box-sizing: border-box; font-family: 'Kanit', sans-serif; transition: all 0.2s ease; }"
                + "body { background: #0f172a; color: #f8fafc; margin: 0; padding: 16px; min-height: 100vh; }"
                + ".container { max-width: 600px; margin: 0 auto; }"
                + ".header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; background: rgba(30,41,59,0.8); padding: 14px 20px; border-radius: 16px; border: 1px solid rgba(255,255,255,0.1); backdrop-filter: blur(10px); }"
                + ".header h1 { margin: 0; font-size: 1.2rem; color: #38bdf8; display: flex; align-items: center; gap: 8px; }"
                + ".btn-nav { background: #334155; color: #f8fafc; text-decoration: none; padding: 6px 12px; border-radius: 10px; font-size: 0.85rem; font-weight: 600; }"
                + ".card { background: rgba(30,41,59,0.8); border: 1px solid rgba(255,255,255,0.12); border-radius: 20px; padding: 20px; margin-bottom: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.4); backdrop-filter: blur(12px); }"
                + ".card-title { font-size: 1.1rem; font-weight: 700; color: #f1f5f9; margin-top: 0; margin-bottom: 16px; border-bottom: 1px solid rgba(255,255,255,0.1); padding-bottom: 8px; display: flex; align-items: center; gap: 8px; }"
                + ".form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }"
                + ".form-group label { font-size: 0.9rem; color: #94a3b8; font-weight: 600; }"
                + ".form-group input, .form-group select { background: #0f172a; border: 1px solid #334155; color: #f8fafc; padding: 12px 14px; border-radius: 12px; font-size: 1rem; outline: none; }"
                + ".form-group input:focus, .form-group select:focus { border-color: #38bdf8; box-shadow: 0 0 0 3px rgba(56,189,248,0.2); }"
                + ".mode-selector { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 16px; }"
                + ".mode-card { border: 2px solid #334155; background: #1e293b; border-radius: 14px; padding: 12px; cursor: pointer; text-align: center; font-size: 0.85rem; }"
                + ".mode-card.active { border-color: #10b981; background: rgba(16,185,129,0.15); color: #34d399; font-weight: 700; }"
                + ".camera-box { border: 2px dashed #0284c7; border-radius: 16px; padding: 24px; text-align: center; background: rgba(2,132,199,0.05); cursor: pointer; margin-bottom: 16px; }"
                + ".camera-box:hover { background: rgba(2,132,199,0.1); }"
                + "#previewImg { max-width: 100%; max-height: 250px; border-radius: 12px; display: none; margin: 10px auto 0; border: 1px solid rgba(255,255,255,0.2); }"
                + ".btn-submit { width: 100%; padding: 14px; border-radius: 14px; border: none; font-weight: 700; font-size: 1.05rem; cursor: pointer; background: linear-gradient(135deg, #10b981, #059669); color: #fff; box-shadow: 0 4px 15px rgba(16,185,129,0.4); display: flex; align-items: center; justify-content: center; gap: 8px; }"
                + ".btn-submit:hover { opacity: 0.9; transform: translateY(-2px); }"
                + ".result-box { margin-top: 16px; padding: 16px; border-radius: 14px; background: #0f172a; border: 1px solid #334155; display: none; font-size: 0.95rem; }"
                + ".badge { padding: 4px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; display: inline-block; }"
                + ".badge-pending { background: rgba(245,158,11,0.2); color: #fbbf24; border: 1px solid #f59e0b; }"
                + ".badge-success { background: rgba(16,185,129,0.2); color: #34d399; border: 1px solid #10b981; }"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "  <h1>📲 เดินจดมิเตอร์มือถือ</h1>"
                + "  <a href='/' class='btn-nav'>🏠 หน้าหลัก</a>"
                + "</div>"
                + "<div class='card' style='border: 2px solid #0284c7; background: rgba(2,132,199,0.08);'>"
                + "  <div class='card-title' style='color:#38bdf8;'>📱 วิธีการเข้าใช้งานจากมือถือ (เปิดกล้องถ่ายสดได้ทันที)</div>"
                + "  <div style='font-size:0.9rem; color:#e2e8f0; line-height:1.6;'>"
                + "    <div style='display:flex; gap:10px; margin-bottom:12px; justify-content:center; flex-wrap:wrap;'>"
                + "      <button type='button' id='btnTabCloud' style='background:linear-gradient(135deg,#0284c7,#2563eb);color:#fff;border:none;padding:8px 16px;border-radius:10px;font-weight:700;cursor:pointer;' onclick='switchQrMode(\"cloud\")'>🌐 ออนไลน์ (Cloudflare Tunnel) ⭐ แนะนำ</button>"
                + "      <button type='button' id='btnTabLan' style='background:#334155;color:#f8fafc;border:none;padding:8px 16px;border-radius:10px;font-weight:700;cursor:pointer;' onclick='switchQrMode(\"lan\")'>📶 วงแลนในบ้าน (Wi-Fi เดียวกัน)</button>"
                + "    </div>"
                + "    <div id='hostUrlDesc' style='color:#94a3b8; text-align:center; margin-bottom:6px;'>ลิงก์ปลอดภัยสากล เปิดได้จากเน็ต 4G/5G และ Wi-Fi ทุกที่โดยไม่มีปัญหา SSL:</div>"
                + "    <div id='hostUrlBox' style='margin:6px 0; background:#0f172a; padding:10px; border-radius:10px; border:1px solid #0284c7; font-size:1.1rem; color:#38bdf8; text-align:center; font-weight:700; word-break:break-all;'>"
                + "      https://laws-creativity-scales-tour.trycloudflare.com/meter-reader"
                + "    </div>"
                + "    <div style='text-align:center; margin-top:12px;'>"
                + "      <img id='qrCodeImg' src='https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=https%3A%2F%2Flaws-creativity-scales-tour.trycloudflare.com%2Fmeter-reader' alt='QR Code' style='border-radius:14px; border:3px solid #38bdf8; width:180px; height:180px; display:inline-block; background:#fff; padding:6px;'>"
                + "      <div style='font-size:0.85rem; color:#38bdf8; margin-top:8px; font-weight:600;'>📷 เอากล้องมือถือสแกน QR Code นี้เพื่อเปิดหน้าจดมิเตอร์ได้เลยครับ!</div>"
                + "    </div>"
                + "  </div>"
                + "</div>"
                + "<div class='card'>"
                + "  <div class='card-title'>📷 ถ่ายรูปมิเตอร์ & เลือกวิธีประมวลผล</div>"
                + "  <form id='meterForm'>"
                + "    <div class='form-group'>"
                + "      <label>📍 เลือก/กรอก เลขห้อง (Room Number)</label>"
                + "      <input type='text' id='roomNumber' value='101' placeholder='เช่น 101, 102' required>"
                + "    </div>"
                + "    <div class='form-group' style='background:rgba(2,132,199,0.12);border:1px solid #0284c7;padding:12px;border-radius:12px;margin-bottom:14px;'>"
                + "      <label style='display:flex;align-items:center;gap:10px;cursor:pointer;color:#38bdf8;font-weight:700;font-size:0.95rem;margin:0;'>"
                + "        <input type='checkbox' id='useCloudAi' checked style='width:20px;height:20px;accent-color:#0284c7;cursor:pointer;'>"
                + "        <span>⚡ <b>เปิดใช้ Cloud AI Vision (ฟรี 1,000+ ครั้ง/วัน)</b> - อ่านแม่นยำ 99.9% รวมทศนิยมกรอบแดง</span>"
                + "      </label>"
                + "    </div>"
                + "    <div class='form-group'>"
                + "      <label>⚙️ เลือกช่องทางประมวลผล (Target Mode)</label>"
                + "      <div class='mode-selector'>"
                + "        <div class='mode-card active' id='modeDirect' onclick='selectMode(\"DIRECT_OCR\")'>"
                + "          <div style='font-size:1.3rem;'>⚡ Option 1</div>"
                + "          <div><b>ส่งประมวลผลทันที</b></div>"
                + "          <div style='font-size:0.75rem;color:#94a3b8;margin-top:4px;'>อ่านและบันทึกอัตโนมัติ</div>"
                + "        </div>"
                + "        <div class='mode-card' id='modeLine' onclick='selectMode(\"LINE_FLEX_MESSAGE\")'>"
                + "          <div style='font-size:1.3rem;'>📲 Option 2</div>"
                + "          <div><b>ส่ง LINE เจ้าของหอพัก</b></div>"
                + "          <div style='font-size:0.75rem;color:#94a3b8;margin-top:4px;'>Flex Message รออนุมัติ</div>"
                + "        </div>"
                + "      </div>"
                + "      <input type='hidden' id='targetMode' value='DIRECT_OCR'>"
                + "    </div>"
                + "    <div style='display:grid; grid-template-columns: 1fr 1fr; gap:10px; margin-bottom:12px;'>"
                + "      <button type='button' style='background:linear-gradient(135deg,#0284c7,#2563eb);color:#fff;border:none;padding:12px;border-radius:12px;font-weight:700;cursor:pointer;' onclick='startWebcam()'>📷 เปิดกล้องถ่ายสด (Live Camera)</button>"
                + "      <button type='button' style='background:#334155;color:#fff;border:none;padding:12px;border-radius:12px;font-weight:700;cursor:pointer;' onclick='document.getElementById(\"cameraInput\").click()'>🖼️ เลือกรูปจากอัลบั้ม</button>"
                + "    </div>"
                + "    <div id='webcamContainer' style='display:none; text-align:center; margin-bottom:16px; background:#000; border-radius:16px; padding:10px; border:2px solid #0284c7;'>"
                + "      <video id='webcamVideo' autoplay playsinline style='width:100%; max-height:300px; border-radius:10px;'></video>"
                + "      <button type='button' style='margin-top:10px; background:#ef4444; color:#fff; border:none; padding:10px 20px; border-radius:12px; font-weight:700; cursor:pointer;' onclick='snapWebcam()'>🔴 ถ่ายรูปเดี๋ยวนี้ (Snap Photo)</button>"
                + "    </div>"
                + "    <div class='camera-box' onclick='document.getElementById(\"cameraInput\").click()'>"
                + "      <div style='font-size:2rem;margin-bottom:6px;'>📸</div>"
                + "      <div style='font-weight:700;color:#38bdf8;'>แตะเพื่อถ่ายรูปหรือเลือกไฟล์รูปภาพ</div>"
                + "      <div style='font-size:0.8rem;color:#94a3b8;'>รองรับทั้งการถ่ายสดและเลือกจากอัลบั้ม</div>"
                + "      <input type='file' id='cameraInput' accept='image/*' capture='environment' style='display:none' onchange='handleFileSelect(this)'>"
                + "      <img id='previewImg' alt='Meter Preview'>"
                + "    </div>"
                + "    <div class='form-group'>"
                + "      <label>✏️ ระบุ/แก้ไข ตัวเลขมิเตอร์ด้วยมือ (Optional - หากต้องการปรับแก้ตัวเลขก่อนส่ง LINE)</label>"
                + "      <input type='number' step='0.1' id='manualReading' placeholder='ปล่อยว่างไว้เพื่อให้ Spring Boot อ่าน OCR อัตโนมัติ'>"
                + "    </div>"
                + "    <button type='submit' class='btn-submit'>🚀 ส่งรูปภาพประมวลผล (Submit Photo)</button>"
                + "  </form>"
                + "  <div id='resultBox' class='result-box'></div>"
                + "</div>"
                + "<div class='card'>"
                + "  <div class='card-title'>⏳ รายการรูปภาพรอเจ้าของหอพักอนุมัติ (LINE Pending Approvals)</div>"
                + "  <div id='pendingList' style='font-size:0.9rem;'>กำลังโหลดข้อมูล...</div>"
                + "</div>"
                + "</div>"
                + "<script>"
                + "let selectedFile = null;"
                + "function selectMode(mode) {"
                + "  document.getElementById('targetMode').value = mode;"
                + "  if (mode === 'DIRECT_OCR') {"
                + "    document.getElementById('modeDirect').className = 'mode-card active';"
                + "    document.getElementById('modeLine').className = 'mode-card';"
                + "  } else {"
                + "    document.getElementById('modeDirect').className = 'mode-card';"
                + "    document.getElementById('modeLine').className = 'mode-card active';"
                + "  }"
                + "}"
                + "let webcamStream = null;"
                + "async function startWebcam() {"
                + "  const container = document.getElementById('webcamContainer');"
                + "  const video = document.getElementById('webcamVideo');"
                + "  container.style.display = 'block';"
                + "  try {"
                + "    webcamStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });"
                + "    video.srcObject = webcamStream;"
                + "  } catch(e) {"
                + "    alert('ไม่สามารถเปิดกล้องสดได้ (อาจต้องเปิดผ่าน HTTPS หรือเบราว์เซอร์ไม่อนุญาต): ' + e.message);"
                + "    document.getElementById('cameraInput').click();"
                + "  }"
                + "}"
                + "function compressImage(file, maxDimension, quality, callback) {"
                + "  const reader = new FileReader();"
                + "  reader.onload = function(e) {"
                + "    const img = new Image();"
                + "    img.onload = function() {"
                + "      let width = img.width;"
                + "      let height = img.height;"
                + "      if (width > maxDimension || height > maxDimension) {"
                + "        if (width > height) {"
                + "          height = Math.round((height * maxDimension) / width);"
                + "          width = maxDimension;"
                + "        } else {"
                + "          width = Math.round((width * maxDimension) / height);"
                + "          height = maxDimension;"
                + "        }"
                + "      }"
                + "      const canvas = document.createElement('canvas');"
                + "      canvas.width = width;"
                + "      canvas.height = height;"
                + "      const ctx = canvas.getContext('2d');"
                + "      ctx.drawImage(img, 0, 0, width, height);"
                + "      canvas.toBlob(function(blob) {"
                + "        const compressedFile = new File([blob], 'meter_' + Date.now() + '.jpg', { type: 'image/jpeg' });"
                + "        callback(compressedFile, URL.createObjectURL(blob));"
                + "      }, 'image/jpeg', quality);"
                + "    };"
                + "    img.src = e.target.result;"
                + "  };"
                + "  reader.readAsDataURL(file);"
                + "}"
                + "function snapWebcam() {"
                + "  const video = document.getElementById('webcamVideo');"
                + "  const canvas = document.createElement('canvas');"
                + "  canvas.width = video.videoWidth || 1280;"
                + "  canvas.height = video.videoHeight || 720;"
                + "  const ctx = canvas.getContext('2d');"
                + "  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);"
                + "  canvas.toBlob((blob) => {"
                + "    selectedFile = new File([blob], 'meter_' + Date.now() + '.jpg', { type: 'image/jpeg' });"
                + "    const img = document.getElementById('previewImg');"
                + "    img.src = URL.createObjectURL(blob);"
                + "    img.style.display = 'block';"
                + "    if (webcamStream) webcamStream.getTracks().forEach(t => t.stop());"
                + "    document.getElementById('webcamContainer').style.display = 'none';"
                + "  }, 'image/jpeg', 0.85);"
                + "}"
                + "function handleFileSelect(input) {"
                + "  if (input.files && input.files[0]) {"
                + "    const box = document.getElementById('resultBox');"
                + "    box.style.display = 'block';"
                + "    box.style.border = '1px solid #0284c7';"
                + "    box.innerHTML = '⚡ กำลังปรับขนาดรูปภาพให้เหมาะสมเพื่อความเร็วในการส่งสูงสุด...';"
                + "    compressImage(input.files[0], 1280, 0.85, function(compressedFile, previewUrl) {"
                + "      selectedFile = compressedFile;"
                + "      const img = document.getElementById('previewImg');"
                + "      img.src = previewUrl;"
                + "      img.style.display = 'block';"
                + "      box.style.display = 'none';"
                + "    });"
                + "  }"
                + "}"
                + "document.getElementById('meterForm').onsubmit = async function(e) {"
                + "  e.preventDefault();"
                + "  if (!selectedFile) { alert('กรุณากดถ่ายรูปหรือเลือกรูปมิเตอร์ก่อนครับ!'); return; }"
                + "  const box = document.getElementById('resultBox');"
                + "  box.style.display = 'block';"
                + "  box.style.border = '1px solid #38bdf8';"
                + "  box.innerHTML = '⚡ <b>กำลังส่งและประมวลผลด้วย Google Cloud AI (Gemini)...</b><br><span style=\"font-size:0.8rem;color:#94a3b8;\">กำลังอ่านตัวเลขหลักดำและทศนิยมหลักแดง</span>';"
                + "  const formData = new FormData();"
                + "  formData.append('file', selectedFile);"
                + "  formData.append('roomNumber', document.getElementById('roomNumber').value);"
                + "  formData.append('targetMode', document.getElementById('targetMode').value);"
                + "  const chkCloud = document.getElementById('useCloudAi');"
                + "  formData.append('useCloudAI', chkCloud ? chkCloud.checked : true);"
                + "  const mVal = document.getElementById('manualReading').value;"
                + "  if (mVal) formData.append('manualReading', mVal);"
                + "  try {"
                + "    const res = await fetch('/api/meter/submit-photo', { method: 'POST', body: formData });"
                + "    const data = await res.json();"
                + "    if (data.success) {"
                + "      if (data.targetMode === 'LINE_FLEX_MESSAGE') {"
                + "        box.style.border = '1px solid #f59e0b';"
                + "        box.innerHTML = `<div>📲 <b>ส่ง LINE Flex Message สำเร็จ!</b></div>` +"
                + "          `<div>ห้อง: <b>${data.roomNumber}</b> | ค่าที่ประมวลผลได้: <b style=\"color:#10b981;\">${data.detectedReading || '-'} kWh</b></div>` +"
                + "          `<div style=\"color:#fbbf24;margin-top:6px;\">${data.message}</div>`;"
                + "      } else {"
                + "        box.style.border = '1px solid #10b981';"
                + "        box.innerHTML = `<div>⚡ <b>สแกนและประมวลผล OCR สำเร็จ!</b></div>` +"
                + "          `<div style=\"font-size:1.3rem;color:#34d399;font-weight:700;margin:6px 0;\">ค่าที่อ่านได้: ${data.meterReading || 0} kWh</div>` +"
                + "          `<div>ข้อความดิบ: <code>${data.rawText || '-'}</code> (ความแม่นยำ: ${data.confidence || '-'})</div>` +"
                + "          `<div style=\"color:#6ee7b7;font-size:0.85rem;margin-top:4px;\">💾 บันทึกเข้าระบบเรียบร้อยแล้ว</div>`;"
                + "      }"
                + "      loadPendingPhotos();"
                + "    } else {"
                + "      box.style.border = '1px solid #ef4444';"
                + "      box.innerHTML = `❌ <b>เกิดข้อผิดพลาด:</b> ${data.message}`;"
                + "    }"
                + "  } catch(err) {"
                + "    box.style.border = '1px solid #ef4444';"
                + "    box.innerHTML = `❌ <b>Error:</b> ${err.message}`;"
                + "  }"
                + "};"
                + "async function loadPendingPhotos() {"
                + "  try {"
                + "    const res = await fetch('/api/meter/pending-photos');"
                + "    const data = await res.json();"
                + "    const container = document.getElementById('pendingList');"
                + "    if (!data || data.length === 0) {"
                + "      container.innerHTML = '<div style=\"color:#94a3b8;text-align:center;\">ไม่มีรายการรูปภาพรออนุมัติ</div>';"
                + "      return;"
                + "    }"
                + "    container.innerHTML = data.map(p => `"
                + "      <div style=\"background:#0f172a;border:1px solid #334155;border-radius:12px;padding:12px;margin-bottom:8px;display:flex;justify-content:space-between;align-items:center;\">"
                + "        <div>"
                + "          <div><b>ห้อง ${p.roomNumber || '-'}</b> <span class=\"badge badge-pending\">รอเจ้าของหออนุมัติ</span></div>"
                + "          <div style=\"font-size:0.75rem;color:#94a3b8;\">ID: ${p.photoId} | เวลา: ${p.createdAt ? p.createdAt.replace('T', ' ').substring(0,19) : '-'}</div>"
                + "        </div>"
                + "        <button style=\"background:linear-gradient(135deg,#10b981,#059669);color:#fff;border:none;padding:6px 12px;border-radius:8px;cursor:pointer;font-weight:700;font-size:0.8rem;\" onclick=\"approvePhoto('${p.photoId}')\">🟢 อนุมัติ & OCR</button>"
                + "      </div>"
                + "    `).join('');"
                + "  } catch(e) { console.error(e); }"
                + "}"
                + "async function approvePhoto(photoId) {"
                + "  if (!confirm('ยืนยันให้ Spring Boot อ่านค่า OCR และบันทึกรูปภาพนี้?')) return;"
                + "  try {"
                + "    const res = await fetch('/api/meter/approve-pending/' + photoId, { method: 'POST' });"
                + "    const data = await res.json();"
                + "    alert(data.message || 'ประมวลผลสำเร็จ!');"
                + "    loadPendingPhotos();"
                + "  } catch(e) { alert('Error: ' + e.message); }"
                + "}"
                + "const CLOUD_URL = 'https://laws-creativity-scales-tour.trycloudflare.com/meter-reader';"
                + "const LAN_URL = 'https://10.21.72.108:8443/meter-reader';"
                + "function switchQrMode(mode) {"
                + "  const urlBox = document.getElementById('hostUrlBox');"
                + "  const qrImg = document.getElementById('qrCodeImg');"
                + "  const desc = document.getElementById('hostUrlDesc');"
                + "  const btnCloud = document.getElementById('btnTabCloud');"
                + "  const btnLan = document.getElementById('btnTabLan');"
                + "  if (mode === 'cloud') {"
                + "    urlBox.innerText = CLOUD_URL;"
                + "    qrImg.src = 'https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=' + encodeURIComponent(CLOUD_URL);"
                + "    desc.innerText = 'ลิงก์ออนไลน์ Cloudflare Tunnel ปลอดภัยสากล เปิดได้จากเน็ต 4G/5G และ Wi-Fi ทุกที่:';"
                + "    btnCloud.style.background = 'linear-gradient(135deg,#0284c7,#2563eb)';"
                + "    btnLan.style.background = '#334155';"
                + "  } else {"
                + "    urlBox.innerText = LAN_URL;"
                + "    qrImg.src = 'https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=' + encodeURIComponent(LAN_URL);"
                + "    desc.innerText = 'ลิงก์วงแลน (มือถือต้องต่อ Wi-Fi เดียวกันกับคอมพิวเตอร์ และกดยืนยันใบรับรองความปลอดภัย):';"
                + "    btnCloud.style.background = '#334155';"
                + "    btnLan.style.background = 'linear-gradient(135deg,#0284c7,#2563eb)';"
                + "  }"
                + "}"
                + "document.addEventListener('DOMContentLoaded', function() {"
                + "  loadPendingPhotos();"
                + "  if (window.location.hostname.includes('trycloudflare.com')) {"
                + "    switchQrMode('cloud');"
                + "  }"
                + "});"
                + "</script></body></html>";
    }
}
