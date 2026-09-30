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
    // Test Git change
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

    @GetMapping(value = { "/maintenance", "/meter-gateway-setup",
            "/gateway-setup" }, produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String maintenance() {
        return getInlineMaintenanceHtml();
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
                + "  function makeRoomsClickable() {"
                + "    const all = document.body.getElementsByTagName('*');"
                + "    for (let el of all) {"
                + "      if (el.children.length === 0 && el.textContent) {"
                + "        const txt = el.textContent.trim();"
                + "        const m = txt.match(/^(?:ห้อง\\s*)?(\\d{3})$/);"
                + "        if (m) {"
                + "          const roomNum = m[1];"
                + "          if (!el.dataset.roomClickAttached) {"
                + "            el.dataset.roomClickAttached = 'true';"
                + "            el.style.cursor = 'pointer';"
                + "            el.style.color = '#00f2fe';"
                + "            el.style.textDecoration = 'underline';"
                + "            el.title = 'คลิกเพื่อดู Real-time Voltage & Meter (ห้อง ' + roomNum + ')';"
                + "            el.addEventListener('click', function(e) {"
                + "              e.stopPropagation();"
                + "              window.location.href = '/live-dashboard?roomNumber=' + encodeURIComponent(roomNum);"
                + "            });"
                + "          }"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "  makeRoomsClickable();"
                + "  setInterval(makeRoomsClickable, 1000);"
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
                + "      '<button id=\"btnLiveDashboardNav\" style=\"background:linear-gradient(135deg,#8b5cf6,#6d28d9);color:#fff;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">⚡ Realtime Monitor</button>' +"
                + "      '<button id=\"btnAddRoomModal\" style=\"background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">➕ เพิ่มห้องใหม่</button>' +"
                + "      '<button id=\"btnDeleteRoomModal\" style=\"background:#f59e0b;color:#0f172a;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">🗑️ ลบห้องพัก</button>' +"
                + "      '<button id=\"btnLogoutAdmin\" style=\"background:#ef4444;color:#fff;border:none;padding:8px 12px;border-radius:12px;cursor:pointer;font-weight:600;\">ออกจากระบบ</button>';"
                + "    document.body.appendChild(bar);"
                + "    document.getElementById('btnGatewaySetup').onclick = function() { window.location.href = '/meter-gateway-setup'; };"
                + "    document.getElementById('btnLiveDashboardNav').onclick = function() { window.location.href = '/live-dashboard?roomNumber=101'; };"
                + "    document.getElementById('btnLogoutAdmin').onclick = function() { localStorage.clear(); window.location.reload(); };"
                + "    document.getElementById('btnAddRoomModal').onclick = function() {"
                + "      const roomNum = prompt('กรอกหมายเลขห้องใหม่ (เช่น 106):');"
                + "      if (!roomNum) return;"
                + "      const tenant = prompt('กรอกชื่อผู้เช่า (ถ้ามี):', 'ว่าง');"
                + "      const rate = prompt('ค่าไฟต่อหน่วย (บาท):', '8.0');"
                + "      fetch('/api/rooms', {"
                + "        method: 'POST',"
                + "        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },"
                + "        body: JSON.stringify({ roomNumber: roomNum.trim(), tenantName: tenant, ratePerUnit: parseFloat(rate) || 8.0 })"
                + "      }).then(res => {"
                + "        if (res.ok) { alert('เพิ่มห้อง ' + roomNum + ' เรียบร้อยแล้ว!'); window.location.reload(); }"
                + "        else { alert('เกิดข้อผิดพลาดในการเพิ่มห้อง (ติดสิทธิ์ Admin)'); }"
                + "      }).catch(err => alert('Error: ' + err));"
                + "    };"
                + "    document.getElementById('btnDeleteRoomModal').onclick = function() {"
                + "      const roomNum = prompt('กรอกหมายเลขห้องที่ต้องการลบ (เช่น 106):');"
                + "      if (!roomNum) return;"
                + "      const targetRoom = roomNum.trim();"
                + "      fetch('/api/rooms').then(res => res.json()).then(rooms => {"
                + "        const found = rooms.find(r => r.roomNumber === targetRoom);"
                + "        if (!found) { alert('ไม่พบห้องหมายเลข ' + targetRoom + ' ในระบบ'); return; }"
                + "        if (confirm('คุณแน่ใจหรือไม่ว่าต้องการลบห้อง ' + targetRoom + ' ออกจากระบบ?')) {"
                + "          fetch('/api/rooms/' + found.id, {"
                + "            method: 'DELETE',"
                + "            headers: { 'Authorization': 'Bearer ' + token }"
                + "          }).then(res => {"
                + "            if (res.ok) { alert('ลบห้อง ' + targetRoom + ' เรียบร้อยแล้ว!'); window.location.reload(); }"
                + "            else { alert('เกิดข้อผิดพลาดในการลบห้อง (ต้องเป็นสิทธิ์ Admin)'); }"
                + "          }).catch(err => alert('Error: ' + err));"
                + "        }"
                + "      }).catch(err => alert('Error: ' + err));"
                + "    };"
                + "  } else {"
                + "    bar.innerHTML = '<span>🔒 ยังไม่ได้เข้าสู่ระบบ Admin</span>' +"
                + "      '<button id=\"btnGatewaySetupGuest\" style=\"background:linear-gradient(135deg,#10b981,#059669);color:#fff;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:600;\">🛠️ Meter Gateway Setup</button>' +"
                + "      '<button id=\"btnLiveDashboardGuest\" style=\"background:linear-gradient(135deg,#8b5cf6,#6d28d9);color:#fff;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:600;\">⚡ Realtime Monitor</button>' +"
                + "      '<button id=\"btnLoginAdmin\" style=\"background:#0284c7;color:#fff;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:600;\">🔑 ไปหน้าล็อกอิน Admin</button>';"
                + "    document.body.appendChild(bar);"
                + "    document.getElementById('btnGatewaySetupGuest').onclick = function() { window.location.href = '/meter-gateway-setup'; };"
                + "    document.getElementById('btnLiveDashboardGuest').onclick = function() { window.location.href = '/live-dashboard?roomNumber=101'; };"
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
        return "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>🔑 เข้าสู่ระบบ Digital Meter</title>"
                + "<link href='https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap' rel='stylesheet'>"
                + "<style>"
                + ":root{--bg:#0b0f19;--card-bg:rgba(22,31,49,0.85);--card-border:rgba(255,255,255,0.1);--text:#fff;--text-muted:#94a3b8;}"
                + "*{box-sizing:border-box;margin:0;padding:0;}"
                + "body{font-family:'Outfit',sans-serif;background:var(--bg);background-image:radial-gradient(at 0% 0%,rgba(0,242,254,0.15) 0px,transparent 50%),radial-gradient(at 100% 100%,rgba(79,172,254,0.15) 0px,transparent 50%);min-height:100vh;color:var(--text);display:flex;justify-content:center;align-items:center;padding:20px;}"
                + ".login-card{background:var(--card-bg);backdrop-filter:blur(12px);border:1px solid var(--card-border);border-radius:24px;padding:40px;max-width:420px;width:100%;box-shadow:0 20px 50px rgba(0,0,0,0.5);}"
                + "h2{font-size:1.8rem;font-weight:700;margin-bottom:8px;background:linear-gradient(135deg,#00f2fe 0%,#4facfe 100%);-webkit-background-clip:text;-webkit-text-fill-color:transparent;text-align:center;}"
                + "p.subtitle{color:var(--text-muted);font-size:0.9rem;text-align:center;margin-bottom:24px;}"
                + ".form-group{margin-bottom:20px;}"
                + "label{display:block;font-size:0.85rem;color:var(--text-muted);margin-bottom:8px;font-weight:600;}"
                + "input{width:100%;background:rgba(15,23,42,0.6);border:1px solid var(--card-border);border-radius:12px;padding:12px 16px;color:#fff;font-size:1rem;outline:none;transition:border-color 0.2s;}"
                + "input:focus{border-color:#00f2fe;}"
                + ".btn-submit{width:100%;background:linear-gradient(135deg,#00f2fe 0%,#4facfe 100%);color:#0f172a;border:none;border-radius:12px;padding:14px;font-size:1rem;font-weight:700;cursor:pointer;transition:transform 0.2s;}"
                + ".btn-submit:hover{transform:translateY(-2px);}"
                + ".alert-box{display:none;background:rgba(239,68,68,0.15);border:1px solid rgba(239,68,68,0.3);color:#f87171;padding:10px;border-radius:10px;font-size:0.85rem;margin-bottom:15px;text-align:center;}"
                + "</style></head><body>"
                + "<div class='login-card'>"
                + "<h2>🔑 เข้าสู่ระบบ Meter Digital</h2>"
                + "<p class='subtitle'>กรอกบัญชีผู้ดูแลระบบ (Admin) หรือผู้เช่าเพื่อเข้าสู่ระบบ</p>"
                + "<div id='errorBox' class='alert-box'>ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง</div>"
                + "<form id='loginForm'>"
                + "<div class='form-group'><label>ชื่อผู้ใช้ (Username)</label><input type='text' id='usernameInput' placeholder='เช่น admin หรือ room101' required value='admin'></div>"
                + "<div class='form-group'><label>รหัสผ่าน (Password)</label><input type='password' id='passwordInput' placeholder='กรอกรหัสผ่าน' required value='admin123'></div>"
                + "<button type='submit' class='btn-submit'>เข้าสู่ระบบ (Login)</button>"
                + "</form>"
                + "</div>"
                + "<script>"
                + "document.getElementById('loginForm').addEventListener('submit',async function(e){e.preventDefault();"
                + "const u=document.getElementById('usernameInput').value.trim();"
                + "const p=document.getElementById('passwordInput').value.trim();"
                + "const err=document.getElementById('errorBox');err.style.display='none';"
                + "try{const res=await fetch('/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:u,password:p})});"
                + "if(!res.ok)throw new Error('Login failed');const data=await res.json();"
                + "if(data&&data.token){localStorage.setItem('token',data.token);localStorage.setItem('role',data.role||'ROLE_ADMIN');"
                + "alert('เข้าสู่ระบบสำเร็จ!');window.location.href='/';}"
                + "else{err.style.display='block';}}catch(e){err.style.display='block';}});"
                + "</script></body></html>";
    }

    private String getInlineDashboardHtml(String roomParam) {
        String defaultRoom = (roomParam != null && !roomParam.isBlank()) ? roomParam.trim() : "101";
        return "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>⚡ Real-time Meter Dashboard - ห้อง " + defaultRoom + "</title>"
                + "<link href='https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap' rel='stylesheet'>"
                + "<style>"
                + ":root{--bg:#0b0f19;--card-bg:rgba(22,31,49,0.85);--card-border:rgba(255,255,255,0.1);--text:#fff;--text-muted:#94a3b8;}"
                + "*{box-sizing:border-box;margin:0;padding:0;}"
                + "body{font-family:'Outfit',sans-serif;background:var(--bg);background-image:radial-gradient(at 0% 0%,rgba(0,242,254,0.15) 0px,transparent 50%),radial-gradient(at 100% 100%,rgba(79,172,254,0.15) 0px,transparent 50%);min-height:100vh;color:var(--text);padding:30px 20px;display:flex;flex-direction:column;align-items:center;}"
                + ".container{max-width:1000px;width:100%;}"
                + ".header{display:flex;justify-content:space-between;align-items:center;margin-bottom:25px;padding-bottom:18px;border-bottom:1px solid var(--card-border);flex-wrap:wrap;gap:15px;}"
                + ".header h1{font-size:1.8rem;font-weight:700;background:linear-gradient(135deg,#00f2fe 0%,#4facfe 100%);-webkit-background-clip:text;-webkit-text-fill-color:transparent;}"
                + ".status-badge{display:inline-flex;align-items:center;gap:8px;background:rgba(16,185,129,0.15);border:1px solid rgba(16,185,129,0.3);color:#10b981;padding:6px 14px;border-radius:20px;font-size:0.85rem;font-weight:600;}"
                + ".dot{width:8px;height:8px;background-color:#10b981;border-radius:50%;box-shadow:0 0 10px #10b981;animation:pulse 1.8s infinite;}"
                + "@keyframes pulse{0%{opacity:0.4;transform:scale(0.9);}50%{opacity:1;transform:scale(1.2);}100%{opacity:0.4;transform:scale(0.9);}}"
                + ".nav-bar{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;}"
                + ".back-btn{display:inline-flex;align-items:center;gap:8px;background:rgba(255,255,255,0.08);border:1px solid var(--card-border);color:#38bdf8;padding:8px 16px;border-radius:12px;text-decoration:none;font-weight:600;transition:all 0.2s;}"
                + ".back-btn:hover{background:rgba(56,189,248,0.15);color:#7dd3fc;}"
                + ".grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:20px;margin-bottom:30px;}"
                + ".card{background:var(--card-bg);backdrop-filter:blur(12px);border:1px solid var(--card-border);border-radius:20px;padding:24px;position:relative;overflow:hidden;transition:all 0.3s ease;}"
                + ".card:hover{transform:translateY(-4px);border-color:rgba(255,255,255,0.25);}"
                + ".card::before{content:'';position:absolute;top:0;left:0;width:100%;height:4px;}"
                + ".card.voltage::before{background:linear-gradient(90deg,#00f2fe,#4facfe);}"
                + ".card.energy::before{background:linear-gradient(90deg,#10b981,#059669);}"
                + ".card.current::before{background:linear-gradient(90deg,#f59e0b,#d97706);}"
                + ".card.power::before{background:linear-gradient(90deg,#ec4899,#8b5cf6);}"
                + ".card-title{font-size:0.85rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:1px;margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;}"
                + ".card-value{font-size:2.2rem;font-weight:700;color:#fff;}"
                + ".card-unit{font-size:1rem;color:var(--text-muted);margin-left:4px;font-weight:400;}"
                + ".card-subtext{margin-top:8px;font-size:0.78rem;color:var(--text-muted);}"
                + ".meta-card{background:var(--card-bg);border:1px solid var(--card-border);border-radius:20px;padding:20px 25px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:15px;}"
                + ".meta-item{display:flex;flex-direction:column;gap:4px;}"
                + ".meta-label{font-size:0.8rem;color:var(--text-muted);}"
                + ".meta-val{font-size:1rem;font-weight:600;color:#fff;}"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='nav-bar'><a href='/' class='back-btn'>⬅️ กลับหน้าหลักรายการห้อง (Main Dashboard)</a></div>"
                + "<div class='header'><div style='display:flex;align-items:center;gap:15px;flex-wrap:wrap;'>"
                + "<h1>⚡ Real-time Meter</h1>"
                + "<div style='display:flex;align-items:center;gap:8px;background:rgba(15,23,42,0.85);border:1px solid #00f2fe;padding:6px 14px;border-radius:14px;box-shadow:0 0 15px rgba(0,242,254,0.2);'>"
                + "<span style='color:#94a3b8;font-size:0.85rem;font-weight:600;'>🏢 เลือกห้องพัก:</span>"
                + "<select id='roomSelect' onchange='changeRoom(this.value)' style='background:#0f172a;color:#00f2fe;border:1px solid rgba(0,242,254,0.4);font-size:1rem;font-weight:700;outline:none;cursor:pointer;padding:4px 10px;border-radius:8px;'>"
                + "<option value='" + defaultRoom + "'>ห้อง " + defaultRoom + "</option></select>"
                + "</div></div>"
                + "<div class='status-badge'><div class='dot'></div><span id='statusText'>LIVE UPDATING</span></div></div>"
                + "<div class='grid'>"
                + "<div class='card voltage'><div class='card-title'><span>Voltage (แรงดันไฟ)</span><span>⚡</span></div>"
                + "<div class='card-value'><span id='valVoltage'>--</span><span class='card-unit'>V</span></div>"
                + "<div class='card-subtext'>แรงดันไฟฟ้า Real-time</div></div>"
                + "<div class='card energy'><div class='card-title'><span>Total Energy (หน่วยไฟ)</span><span>🔋</span></div>"
                + "<div class='card-value'><span id='valEnergy'>--</span><span class='card-unit'>kWh</span></div>"
                + "<div class='card-subtext'>หน่วยไฟฟ้าสะสมรวม</div></div>"
                + "<div class='card current'><div class='card-title'><span>Current (กระแสไฟ)</span><span>🔌</span></div>"
                + "<div class='card-value'><span id='valCurrent'>--</span><span class='card-unit'>A</span></div>"
                + "<div class='card-subtext'>กระแสไฟฟ้าขณะนี้</div></div>"
                + "<div class='card power'><div class='card-title'><span>Active Power (กำลังไฟ)</span><span>💡</span></div>"
                + "<div class='card-value'><span id='valPower'>--</span><span class='card-unit'>kW</span></div>"
                + "<div class='card-subtext'>กำลังไฟฟ้าที่ใช้งาน</div></div>"
                + "</div>"
                + "<div class='meta-card'>"
                + "<div class='meta-item'><span class='meta-label'>Meter ID / Room</span><span class='meta-val' id='valMeterId'>--</span></div>"
                + "<div class='meta-item'><span class='meta-label'>Last Updated Time</span><span class='meta-val' id='valTimestamp'>--</span></div>"
                + "<div class='meta-item'><span class='meta-label'>Refresh Rate</span><span class='meta-val'>3 วินาที (Auto)</span></div>"
                + "</div></div>"
                + "<script>"
                + "const urlParams = new URLSearchParams(window.location.search);"
                + "let currentRoomNum = urlParams.get('roomNumber') || '" + defaultRoom + "';"
                + "async function loadRooms(){try{const res=await fetch('/api/rooms');if(res.ok){const rooms=await res.json();if(rooms&&rooms.length>0){const sel=document.getElementById('roomSelect');sel.innerHTML=rooms.map(r=>'<option value=\"'+r.roomNumber+'\" '+(r.roomNumber===currentRoomNum?'selected':'')+'>ห้อง '+r.roomNumber+(r.tenantName?' ('+r.tenantName+')':'')+'</option>').join('');}}}catch(e){console.error(e);}}"
                + "function changeRoom(newRoom){currentRoomNum=newRoom;window.history.pushState({},'','/live-dashboard?roomNumber='+encodeURIComponent(newRoom));fetchLatestMeter();}"
                + "async function fetchLatestMeter(){try{const res=await fetch('/api/meter/latest?roomNumber='+encodeURIComponent(currentRoomNum));if(!res.ok)throw new Error('HTTP '+res.status);const data=await res.json();if(data){"
                + "document.getElementById('valVoltage').textContent=data.voltage!==undefined&&data.voltage!==null?data.voltage.toFixed(2):'0.00';"
                + "document.getElementById('valEnergy').textContent=data.energy!==undefined&&data.energy!==null?data.energy.toFixed(2):(data.unit!==undefined&&data.unit!==null?data.unit.toFixed(2):'0.00');"
                + "document.getElementById('valCurrent').textContent=data.current!==undefined&&data.current!==null?data.current.toFixed(3):'0.000';"
                + "document.getElementById('valPower').textContent=data.power!==undefined&&data.power!==null?data.power.toFixed(4):'0.000';"
                + "const rNum=data.roomNumber||(data.room?data.room.roomNumber:currentRoomNum);"
                + "document.getElementById('valMeterId').textContent=(data.meterId||'METER001')+' (ห้อง '+rNum+')';"
                + "if(data.timestamp){const dt=new Date(data.timestamp);document.getElementById('valTimestamp').textContent=dt.toLocaleTimeString('th-TH')+' ('+dt.toLocaleDateString('th-TH')+')';}"
                + "document.getElementById('statusText').textContent='LIVE UPDATING';"
                + "}}catch(err){console.error('Fetch error:',err);document.getElementById('statusText').textContent='OFFLINE / WAITING';}}"
                + "loadRooms();fetchLatestMeter();setInterval(fetchLatestMeter,3000);"
                + "</script></body></html>";
    }

    private String getInlineMaintenanceHtml() {
        return "<!DOCTYPE html><html lang='th'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>🛠️ Technician Maintenance Center - Opta / ESP32 Config</title>"
                + "<link href='https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap' rel='stylesheet'>"
                + "<style>"
                + ":root{--bg:#0b0f19;--card-bg:rgba(22,31,49,0.85);--card-border:rgba(255,255,255,0.1);--text:#fff;--text-muted:#94a3b8;--primary:#00f2fe;--accent:#4facfe;}"
                + "*{box-sizing:border-box;margin:0;padding:0;}"
                + "body{font-family:'Outfit',sans-serif;background:var(--bg);background-image:radial-gradient(at 0% 0%,rgba(0,242,254,0.12) 0px,transparent 50%),radial-gradient(at 100% 100%,rgba(79,172,254,0.12) 0px,transparent 50%);min-height:100vh;color:var(--text);padding:30px 20px;display:flex;flex-direction:column;align-items:center;}"
                + ".container{max-width:1100px;width:100%;}"
                + ".header{display:flex;justify-content:space-between;align-items:center;margin-bottom:25px;padding-bottom:18px;border-bottom:1px solid var(--card-border);flex-wrap:wrap;gap:15px;}"
                + ".header h1{font-size:1.8rem;font-weight:700;background:linear-gradient(135deg,#00f2fe 0%,#4facfe 100%);-webkit-background-clip:text;-webkit-text-fill-color:transparent;}"
                + ".btn-action{background:linear-gradient(135deg,#00f2fe 0%,#4facfe 100%);color:#0f172a;border:none;padding:10px 18px;border-radius:12px;font-weight:700;cursor:pointer;transition:all 0.2s;display:inline-flex;align-items:center;gap:6px;}"
                + ".btn-action:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(0,242,254,0.4);}"
                + ".btn-secondary{background:rgba(255,255,255,0.1);color:#fff;border:1px solid var(--card-border);padding:8px 14px;border-radius:10px;cursor:pointer;font-weight:600;}"
                + ".btn-danger{background:rgba(239,68,68,0.2);border:1px solid rgba(239,68,68,0.4);color:#f87171;padding:8px 12px;border-radius:10px;cursor:pointer;font-weight:600;}"
                + ".grid-stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:20px;margin-bottom:30px;}"
                + ".card-stat{background:var(--card-bg);backdrop-filter:blur(12px);border:1px solid var(--card-border);border-radius:18px;padding:20px;}"
                + ".stat-label{font-size:0.85rem;color:var(--text-muted);margin-bottom:6px;}"
                + ".stat-val{font-size:2rem;font-weight:700;}"
                + ".table-card{background:var(--card-bg);backdrop-filter:blur(12px);border:1px solid var(--card-border);border-radius:20px;padding:24px;overflow-x:auto;}"
                + "table{width:100%;border-collapse:collapse;text-align:left;}"
                + "th,td{padding:14px 16px;border-bottom:1px solid var(--card-border);font-size:0.92rem;}"
                + "th{color:var(--text-muted);font-weight:600;text-transform:uppercase;font-size:0.8rem;letter-spacing:1px;}"
                + ".badge{display:inline-block;padding:4px 10px;border-radius:12px;font-size:0.75rem;font-weight:700;}"
                + ".badge-online{background:rgba(16,185,129,0.2);color:#34d399;border:1px solid rgba(16,185,129,0.4);}"
                + ".badge-offline{background:rgba(239,68,68,0.2);color:#f87171;border:1px solid rgba(239,68,68,0.4);}"
                + ".modal-overlay{display:none;position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.7);backdrop-filter:blur(8px);z-index:9999;justify-content:center;align-items:center;padding:20px;}"
                + ".modal-content{background:#111827;border:1px solid var(--card-border);border-radius:24px;padding:30px;max-width:550px;width:100%;max-height:90vh;overflow-y:auto;}"
                + ".modal-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;}"
                + ".modal-title{font-size:1.3rem;font-weight:700;color:var(--primary);}"
                + ".form-group{margin-bottom:16px;}"
                + ".form-group label{display:block;font-size:0.85rem;color:var(--text-muted);margin-bottom:6px;font-weight:600;}"
                + ".form-group input, .form-group select, .form-group textarea{width:100%;background:rgba(15,23,42,0.8);border:1px solid var(--card-border);border-radius:10px;padding:10px 14px;color:#fff;font-size:0.95rem;outline:none;}"
                + ".form-group input:focus,.form-group select:focus{border-color:var(--primary);}"
                + ".nav-bar{margin-bottom:20px;display:flex;gap:15px;}"
                + ".back-link{color:var(--primary);text-decoration:none;font-weight:600;display:inline-flex;align-items:center;gap:6px;}"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='nav-bar'><a href='/' class='back-link'>⬅️ กลับหน้าหลัก Dashboard</a></div>"
                + "<div class='header'><div><h1>🛠️ Technician Maintenance Center</h1><p style='color:var(--text-muted);font-size:0.9rem;'>ระบบจัดการและส่ง Config ไปยัง Arduino Opta / ESP32 Gateway</p></div>"
                + "<button class='btn-action' onclick='openAddModal()'>➕ ลงทะเบียน Gateway ใหม่</button></div>"
                + "<div class='grid-stats'>"
                + "<div class='card-stat'><div class='stat-label'> Gateway ทั้งหมด</div><div class='stat-val' id='cntTotal'>0</div></div>"
                + "<div class='card-stat'><div class='stat-label'> สถานะ ONLINE</div><div class='stat-val' style='color:#34d399' id='cntOnline'>0</div></div>"
                + "<div class='card-stat'><div class='stat-label'> สถานะ WARNING / OFFLINE</div><div class='stat-val' style='color:#f87171' id='cntOffline'>0</div></div>"
                + "</div>"
                + "<div class='table-card'>"
                + "<table><thead><tr><th>Device ID</th><th>ชื่อบอร์ด / สถานที่</th><th>IP Address</th><th>รุ่นมิเตอร์ที่ต่อ</th><th>สถานะ</th><th>เครื่องมือช่าง (Gateway Tool)</th><th>การจัดการ</th></tr></thead>"
                + "<tbody id='deviceTableBody'><tr><td colspan='7' style='text-align:center;color:var(--text-muted)'>กำลังโหลดข้อมูล...</td></tr></tbody></table>"
                + "</div></div>"

                // Modal: Edit & Push Config
                + "<div id='configModal' class='modal-overlay'><div class='modal-content'>"
                + "<div class='modal-header'><h3 class='modal-title'>⚙️ ตั้งค่า & Push Config ไปยังบอร์ด</h3><span style='cursor:pointer;font-size:1.4rem' onclick='closeModal()'>✖</span></div>"
                + "<form id='configForm'>"
                + "<input type='hidden' id='cfgId'>"
                + "<div class='form-group'><label>Device ID</label><input type='text' id='cfgDeviceId' required readonly style='opacity:0.7'></div>"
                + "<div class='form-group'><label>ชื่ออุปกรณ์ / สถานที่ติดตั้ง</label><input type='text' id='cfgName' placeholder='เช่น Arduino Opta อาคาร A ชั้น 1' required></div>"
                + "<div class='form-group'><label>IP Address ของ Opta / ESP32</label><input type='text' id='cfgIp' placeholder='เช่น 10.221.143.118' required></div>"
                + "<div class='form-group'><label>Server IP (Spring Boot IP)</label><input type='text' id='cfgServerIp' placeholder='เช่น 10.221.143.108' value='10.221.143.108' required></div>"
                + "<div class='form-group'><label>Server Port</label><input type='number' id='cfgServerPort' value='8080' required></div>"
                + "<div class='form-group'><label>Wi-Fi SSID</label><input type='text' id='cfgSsid' placeholder='ชื่อ Wi-Fi'></div>"
                + "<div class='form-group'><label>Wi-Fi Password</label><input type='password' id='cfgPassword' placeholder='รหัสผ่าน Wi-Fi'></div>"
                + "<div class='form-group'><label>รุ่นมิเตอร์หลักที่ใช้งาน</label>"
                + "<select id='cfgModel'><option value='CIRCUTOR_CVMC5'>Circutor CVM-C5</option><option value='SCHNEIDER_PM2200'>Schneider PM2200</option></select></div>"
                + "<div class='form-group'><label>ความถี่ในการอ่านค่า (วินาที)</label><input type='number' id='cfgInterval' value='10' required></div>"
                + "<div style='display:flex;gap:10px;margin-top:20px;'>"
                + "<button type='button' class='btn-secondary' style='flex:1' onclick='saveDeviceOnly()'>💾 บันทึกในระบบ</button>"
                + "<button type='submit' class='btn-action' style='flex:1.5'>🚀 Push Config ไปยังบอร์ด</button>"
                + "</div></form></div></div>"

                + "<script>"
                + "async function loadDevices(){try{const res=await fetch('/api/maintenance/devices');const devices=await res.json();"
                + "document.getElementById('cntTotal').textContent=devices.length;"
                + "const online=devices.filter(d=>d.status==='ONLINE').length;"
                + "document.getElementById('cntOnline').textContent=online;"
                + "document.getElementById('cntOffline').textContent=devices.length-online;"
                + "const tbody=document.getElementById('deviceTableBody');"
                + "if(devices.length===0){tbody.innerHTML='<tr><td colspan=\"7\" style=\"text-align:center;color:var(--text-muted)\">ยังไม่มีบอร์ดในระบบ กดปุ่มเพิ่ม Gateway ใหม่ด้านบน</td></tr>';return;}"
                + "window.deviceMap=new Map(devices.map(d=>[d.id,d]));"
                + "tbody.innerHTML=devices.map(d=>{"
                + "  const ip=(d.ipAddress||'').trim();"
                + "  const tools=ip?("
                + "    '<a href=\"http://' + ip + '/\" target=\"_blank\" style=\"background:#0284c7;color:#fff;padding:3px 8px;border-radius:6px;text-decoration:none;font-size:0.75rem;font-weight:600;display:inline-block;margin:2px;\" title=\"ดูหน้าสถานะทั่วไป\">🌐 สถานะ</a>'"
                + "    + '<a href=\"http://' + ip + '/log\" target=\"_blank\" style=\"background:#10b981;color:#fff;padding:3px 8px;border-radius:6px;text-decoration:none;font-size:0.75rem;font-weight:600;display:inline-block;margin:2px;\" title=\"ดู Live Log การอ่านค่ามิเตอร์\">📋 Live Log</a>'"
                + "    + '<a href=\"http://' + ip + '/update\" target=\"_blank\" style=\"background:#8b5cf6;color:#fff;padding:3px 8px;border-radius:6px;text-decoration:none;font-size:0.75rem;font-weight:600;display:inline-block;margin:2px;\" title=\"อัปเดต Firmware ผ่านเว็บ (OTA)\">⚡ OTA Update</a>'"
                + "    + '<a href=\"http://' + ip + '/reset-ap\" target=\"_blank\" onclick=\"return confirm(\\'ต้องการรีเซ็ตบอร์ดเข้าโหมด AP Setup ใช่หรือไม่?\\')\" style=\"background:#f59e0b;color:#0f172a;padding:3px 8px;border-radius:6px;text-decoration:none;font-size:0.75rem;font-weight:700;display:inline-block;margin:2px;\" title=\"รีเซ็ตเข้าโหมด AP\">🔄 Reset AP</a>'"
                + "  ):'<span style=\"color:var(--text-muted);font-size:0.8rem;\">ยังไม่ได้ระบุ IP</span>';"
                + "  return '<tr>'"
                + "    + '<td><b>' + (d.deviceId||'') + '</b></td>'"
                + "    + '<td>' + (d.deviceName||'-') + '</td>'"
                + "    + '<td><code>' + (d.ipAddress||'-') + '</code></td>'"
                + "    + '<td><span style=\"color:#00f2fe\">' + (d.activeMeterModel||'CIRCUTOR_CVMC5') + '</span></td>'"
                + "    + '<td><span class=\"badge ' + (d.status==='ONLINE'?'badge-online':'badge-offline') + '\">' + (d.status||'UNKNOWN') + '</span></td>'"
                + "    + '<td>' + tools + '</td>'"
                + "    + '<td><button class=\"btn-action\" style=\"padding:4px 10px;font-size:0.8rem\" onclick=\"openEditById(' + d.id + ')\">⚙️ ตั้งค่า & Push</button> '"
                + "    + '<button class=\"btn-danger\" style=\"padding:4px 8px;font-size:0.8rem\" onclick=\"deleteDev(' + d.id + ')\">🗑️</button></td>'"
                + "    + '</tr>';"
                + "}).join('');"
                + "}catch(e){console.error(e);}}"
                + "function openEditById(id){const d=window.deviceMap?window.deviceMap.get(id):null;if(d)openEditModal(d);}"
                + "function openAddModal(){document.getElementById('configForm').reset();document.getElementById('cfgId').value='';document.getElementById('cfgDeviceId').readOnly=false;document.getElementById('cfgDeviceId').value='OPTA-DORM-01';document.getElementById('configModal').style.display='flex';}"
                + "function openEditModal(d){document.getElementById('cfgId').value=d.id||'';document.getElementById('cfgDeviceId').value=d.deviceId||'';document.getElementById('cfgDeviceId').readOnly=true;"
                + "document.getElementById('cfgName').value=d.deviceName||'';document.getElementById('cfgIp').value=d.ipAddress||'';"
                + "document.getElementById('cfgServerIp').value=d.serverIp||'10.221.143.108';document.getElementById('cfgServerPort').value=d.serverPort||8080;"
                + "document.getElementById('cfgSsid').value=d.wifiSsid||'';document.getElementById('cfgPassword').value=d.wifiPassword||'';"
                + "document.getElementById('cfgModel').value=d.activeMeterModel||'CIRCUTOR_CVMC5';"
                + "document.getElementById('configModal').style.display='flex';}"
                + "function closeModal(){document.getElementById('configModal').style.display='none';}"
                + "async function saveDeviceOnly(){"
                + "const dev={deviceId:document.getElementById('cfgDeviceId').value,deviceName:document.getElementById('cfgName').value,ipAddress:document.getElementById('cfgIp').value,serverIp:document.getElementById('cfgServerIp').value,serverPort:parseInt(document.getElementById('cfgServerPort').value),wifiSsid:document.getElementById('cfgSsid').value,wifiPassword:document.getElementById('cfgPassword').value,activeMeterModel:document.getElementById('cfgModel').value};"
                + "try{const res=await fetch('/api/maintenance/devices',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(dev)});if(res.ok){alert('บันทึกข้อมูลสำเร็จ!');closeModal();loadDevices();}}catch(e){alert('Error: '+e);}}"
                + "document.getElementById('configForm').addEventListener('submit',async function(e){e.preventDefault();"
                + "await saveDeviceOnly();"
                + "const devId=document.getElementById('cfgDeviceId').value;"
                + "const allRes=await fetch('/api/maintenance/devices');const list=await allRes.json();const found=list.find(d=>d.deviceId===devId);"
                + "if(found&&found.id){try{const pRes=await fetch('/api/maintenance/devices/'+found.id+'/push-config',{method:'POST'});"
                + "const resData=await pRes.json();if(resData.success){alert('🚀 Push Config ไปยังบอร์ด '+found.ipAddress+' เรียบร้อยแล้ว!');}"
                + "else{alert('⚠️ บันทึกข้อมูลแล้ว แต่ยิง Push Config ไม่สำเร็จ (บอร์ดอาจยังไม่ได้เปิดรับ /api/config): '+resData.message);}}"
                + "catch(err){alert('Push error: '+err);}}loadDevices();});"
                + "async function deleteDev(id){if(confirm('แน่ใจหรือไม่ว่าต้องการลบ Gateway นี้?')){await fetch('/api/maintenance/devices/'+id,{method:'DELETE'});loadDevices();}}"
                + "loadDevices();setInterval(loadDevices,5000);"
                + "</script></body></html>";
    }
}
