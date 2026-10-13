; Inno Setup Script for Meter Digital Commercial Installer
; Produces MeterDigitalSetup.exe installer for Windows

[Setup]
AppName=Meter Digital Pro (ระบบบริหารจัดการอ่านมิเตอร์ไฟฟ้าดิจิทัล)
AppVersion=1.0.0
AppPublisher=Meter Digital Solution
DefaultDirName={autopf}\MeterDigital
DefaultGroupName=Meter Digital Pro
UninstallDisplayIcon={app}\MeterDigital.exe
Compression=lzma2/max
SolidCompression=yes
OutputDir=dist
OutputBaseFilename=MeterDigitalSetup
SetupIconFile=src\main\resources\static\favicon.ico
WizardStyle=modern

[Languages]
Name: "thailand"; MessagesFile: "compiler:Languages\Thai.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "target\meter-digital-0.0.1-SNAPSHOT.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "dorm.db"; DestDir: "{app}"; Flags: onlyifdestfilemissing
Source: "src\main\resources\application.properties"; DestDir: "{app}\config"; Flags: ignoreversion

[Icons]
Name: "{group}\Meter Digital Pro"; Filename: "{app}\MeterDigital.exe"
Name: "{autodesktop}\Meter Digital Pro"; Filename: "{app}\MeterDigital.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\MeterDigital.exe"; Description: "เปิดใช้งาน Meter Digital Pro ทันที"; Flags: postinstall nowait skipifsilent
