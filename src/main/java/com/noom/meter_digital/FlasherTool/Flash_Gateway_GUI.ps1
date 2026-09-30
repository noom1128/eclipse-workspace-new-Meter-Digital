Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$form = New-Object System.Windows.Forms.Form
$form.Text = "1-Click Meter Gateway Flasher Tool"
$form.Size = New-Object System.Drawing.Size(520, 380)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = "FixedDialog"
$form.MaximizeBox = $false
$form.BackColor = [System.Drawing.Color]::FromArgb(15, 23, 42)

# Title
$lblTitle = New-Object System.Windows.Forms.Label
$lblTitle.Text = "1-Click Firmware Flasher Tool"
$lblTitle.Font = New-Object System.Drawing.Font("Segoe UI", 14, [System.Drawing.FontStyle]::Bold)
$lblTitle.ForeColor = [System.Drawing.Color]::FromArgb(0, 242, 254)
$lblTitle.Size = New-Object System.Drawing.Size(460, 35)
$lblTitle.Location = New-Object System.Drawing.Point(20, 20)
$form.Controls.Add($lblTitle)

# Subtitle
$lblSub = New-Object System.Windows.Forms.Label
$lblSub.Text = "Select USB-C COM Port and click Flash Firmware button"
$lblSub.Font = New-Object System.Drawing.Font("Segoe UI", 9)
$lblSub.ForeColor = [System.Drawing.Color]::FromArgb(148, 163, 184)
$lblSub.Size = New-Object System.Drawing.Size(460, 25)
$lblSub.Location = New-Object System.Drawing.Point(20, 55)
$form.Controls.Add($lblSub)

# Label COM Port
$lblPort = New-Object System.Windows.Forms.Label
$lblPort.Text = "Select COM Port (USB-C):"
$lblPort.Font = New-Object System.Drawing.Font("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
$lblPort.ForeColor = [System.Drawing.Color]::White
$lblPort.Size = New-Object System.Drawing.Size(460, 25)
$lblPort.Location = New-Object System.Drawing.Point(20, 95)
$form.Controls.Add($lblPort)

# Combo Box COM Port
$comboPort = New-Object System.Windows.Forms.ComboBox
$comboPort.Size = New-Object System.Drawing.Size(460, 30)
$comboPort.Location = New-Object System.Drawing.Point(20, 125)
$comboPort.Font = New-Object System.Drawing.Font("Segoe UI", 10)
$comboPort.DropDownStyle = "DropDownList"
$form.Controls.Add($comboPort)

function Refresh-ComPorts {
    $comboPort.Items.Clear()
    try {
        $ports = [System.IO.Ports.SerialPort]::GetPortNames()
    } catch {
        $ports = @()
    }
    if ($ports.Count -eq 0) {
        try {
            $ports = (Get-CimInstance -ClassName Win32_SerialPort).DeviceID
        } catch {}
    }
    if ($ports.Count -eq 0) {
        $comboPort.Items.Add("COM1")
        $comboPort.Items.Add("COM2")
        $comboPort.Items.Add("COM3")
        $comboPort.Items.Add("COM4")
        $comboPort.Items.Add("COM5")
        $comboPort.Items.Add("COM6")
        $comboPort.Items.Add("COM7")
        $comboPort.Items.Add("COM8")
    } else {
        foreach ($p in $ports) {
            $comboPort.Items.Add($p)
        }
    }
    $comboPort.SelectedIndex = 0
}

# Button Refresh
$btnRefresh = New-Object System.Windows.Forms.Button
$btnRefresh.Text = "Refresh Ports"
$btnRefresh.Size = New-Object System.Drawing.Size(140, 32)
$btnRefresh.Location = New-Object System.Drawing.Point(20, 165)
$btnRefresh.BackColor = [System.Drawing.Color]::FromArgb(51, 65, 85)
$btnRefresh.ForeColor = [System.Drawing.Color]::White
$btnRefresh.FlatStyle = "Flat"
$btnRefresh.Add_Click({ Refresh-ComPorts })
$form.Controls.Add($btnRefresh)

Refresh-ComPorts

# Status Label
$lblStatus = New-Object System.Windows.Forms.Label
$lblStatus.Text = "Status: Ready to Flash"
$lblStatus.Font = New-Object System.Drawing.Font("Segoe UI", 9.5)
$lblStatus.ForeColor = [System.Drawing.Color]::FromArgb(52, 211, 153)
$lblStatus.Size = New-Object System.Drawing.Size(460, 25)
$lblStatus.Location = New-Object System.Drawing.Point(20, 210)
$form.Controls.Add($lblStatus)

# Button Flash
$btnFlash = New-Object System.Windows.Forms.Button
$btnFlash.Text = "Flash Firmware to Board (1-Click)"
$btnFlash.Size = New-Object System.Drawing.Size(460, 45)
$btnFlash.Location = New-Object System.Drawing.Point(20, 245)
$btnFlash.Font = New-Object System.Drawing.Font("Segoe UI", 11, [System.Drawing.FontStyle]::Bold)
$btnFlash.BackColor = [System.Drawing.Color]::FromArgb(0, 242, 254)
$btnFlash.ForeColor = [System.Drawing.Color]::FromArgb(15, 23, 42)
$btnFlash.FlatStyle = "Flat"

$btnFlash.Add_Click({
    if ($comboPort.SelectedItem -eq $null) {
        [System.Windows.Forms.MessageBox]::Show("Please select a COM port first", "Warning", 0, 48)
        return
    }
    $selectedPort = $comboPort.SelectedItem.ToString()
    $lblStatus.Text = "Flashing Firmware to $selectedPort... (Please wait 5-10 seconds)"
    $lblStatus.ForeColor = [System.Drawing.Color]::Yellow
    $form.Refresh()

    $esptool = "C:\Users\Nopporn.C\AppData\Local\Arduino15\packages\esp32\tools\esptool_py\5.3.1\esptool.exe"
    
    $fwDir = Join-Path $PSScriptRoot "..\MeterDigital_Upload_Wifi_API"
    $fw = Join-Path $fwDir "MeterDigital_Upload_Wifi_API.ino.bin"
    if (-not (Test-Path $fw)) {
        $found = Get-ChildItem -Path $fwDir -Filter "*.ino.bin" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $fw = $found.FullName
        }
    }

    if (-not (Test-Path $fw)) {
        [System.Windows.Forms.MessageBox]::Show("Firmware .bin file not found!`nPath: $fw", "Error", 0, 16)
        $lblStatus.Text = "Status: Firmware file not found"
        $lblStatus.ForeColor = [System.Drawing.Color]::Red
        return
    }

    $process = Start-Process -FilePath $esptool -ArgumentList "--chip esp32 --port $selectedPort --baud 921600 --before default-reset --after hard-reset write-flash -z 0x10000 `"$fw`"" -Wait -NoNewWindow -PassThru

    if ($process.ExitCode -eq 0) {
        $lblStatus.Text = "Status: SUCCESS! Firmware Flashed Successfully"
        $lblStatus.ForeColor = [System.Drawing.Color]::FromArgb(52, 211, 153)
        [System.Windows.Forms.MessageBox]::Show("Firmware Flashed Successfully! You can now disconnect the USB-C cable.", "Success", 0, 64)
    } else {
        $lblStatus.Text = "Status: Flash Failed (COM port busy or disconnected)"
        $lblStatus.ForeColor = [System.Drawing.Color]::Red
        [System.Windows.Forms.MessageBox]::Show("Could not open $selectedPort (Access Denied).`n`nPlease CLOSE Serial Monitor in Arduino IDE or any terminal app holding COM port, then try again!", "COM Port Busy", 0, 48)
    }
})

$form.Controls.Add($btnFlash)
[void]$form.ShowDialog()
