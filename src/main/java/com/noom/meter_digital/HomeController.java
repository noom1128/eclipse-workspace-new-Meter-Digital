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
        String[] paths = {"/dashboard.html", "/static/dashboard.html", "/templates/dashboard.html", "/public/dashboard.html"};
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

    @GetMapping(value = "/live-dashboard", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String liveDashboard(@RequestParam(required = false, defaultValue = "101") String roomNumber) {
        return getInlineDashboardHtml(roomNumber);
    }

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String login() {
        String[] paths = {"/login.html", "/static/login.html", "/templates/login.html", "/public/login.html"};
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
                + "      '<button id=\"btnAddRoomModal\" style=\"background:linear-gradient(135deg,#00f2fe,#4facfe);color:#0f172a;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">➕ เพิ่มห้องใหม่</button>' +"
                + "      '<button id=\"btnDeleteRoomModal\" style=\"background:#f59e0b;color:#0f172a;border:none;padding:8px 14px;border-radius:12px;cursor:pointer;font-weight:700;\">🗑️ ลบห้องพัก</button>' +"
                + "      '<button id=\"btnLogoutAdmin\" style=\"background:#ef4444;color:#fff;border:none;padding:8px 12px;border-radius:12px;cursor:pointer;font-weight:600;\">ออกจากระบบ</button>';"
                + "    document.body.appendChild(bar);"
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
                + "      '<button id=\"btnLoginAdmin\" style=\"background:#0284c7;color:#fff;border:none;padding:10px 16px;border-radius:12px;cursor:pointer;font-weight:600;\">🔑 ไปหน้าล็อกอิน Admin</button>';"
                + "    document.body.appendChild(bar);"
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
                + "<div class='header'><h1>⚡ Real-time Meter: ห้อง <span id='roomTitle'>" + defaultRoom + "</span></h1>"
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
                + "const roomNum = urlParams.get('roomNumber') || '" + defaultRoom + "';"
                + "document.getElementById('roomTitle').textContent = roomNum;"
                + "async function fetchLatestMeter(){try{const res=await fetch('/api/meter/latest?roomNumber='+encodeURIComponent(roomNum));if(!res.ok)throw new Error('HTTP '+res.status);const data=await res.json();if(data){"
                + "document.getElementById('valVoltage').textContent=data.voltage!==undefined&&data.voltage!==null?data.voltage.toFixed(2):'0.00';"
                + "document.getElementById('valEnergy').textContent=data.energy!==undefined&&data.energy!==null?data.energy.toFixed(2):(data.unit!==undefined&&data.unit!==null?data.unit.toFixed(2):'0.00');"
                + "document.getElementById('valCurrent').textContent=data.current!==undefined&&data.current!==null?data.current.toFixed(3):'0.000';"
                + "document.getElementById('valPower').textContent=data.power!==undefined&&data.power!==null?data.power.toFixed(4):'0.000';"
                + "const rNum=data.roomNumber||(data.room?data.room.roomNumber:roomNum);"
                + "document.getElementById('valMeterId').textContent=(data.meterId||'METER001')+' (ห้อง '+rNum+')';"
                + "if(data.timestamp){const dt=new Date(data.timestamp);document.getElementById('valTimestamp').textContent=dt.toLocaleTimeString('th-TH')+' ('+dt.toLocaleDateString('th-TH')+')';}"
                + "document.getElementById('statusText').textContent='LIVE UPDATING';"
                + "}}catch(err){console.error('Fetch error:',err);document.getElementById('statusText').textContent='OFFLINE / WAITING';}}"
                + "fetchLatestMeter();setInterval(fetchLatestMeter,3000);"
                + "</script></body></html>";
    }
}
