!define JRE_VERSION "1.8"
!define JAVA_URL "http://www.java.com/download"
!define PRODUCT_NAME "JHelioviewer"
!define JAVAEXE "java.exe"

!include "FileFunc.nsh"
!include "x64.nsh"
!insertmacro GetFileVersion
!insertmacro GetParameters
!include "WordFunc.nsh"
!insertmacro VersionCompare

VIAddVersionKey "ProductName" "${PRODUCT_NAME}"
VIAddVersionKey "Comments" "Solar image viewer based on JPEG2000"
VIAddVersionKey "FileDescription" "${PRODUCT_NAME} Setup"
VIAddVersionKey "LegalCopyright" "This product is released under the Mozilla Public License Version 2.0"
VIAddVersionKey "FileVersion" "${JHV_VERSION}.${JHV_REVISION}"
VIProductVersion "${JHV_VERSION}.${JHV_REVISION}"

Name "ESA ${PRODUCT_NAME}"
Caption "ESA ${PRODUCT_NAME}"

Icon "${RESOURCE_PATH}\${PRODUCT_NAME}.ico"
InstallDir "$PROGRAMFILES64\${PRODUCT_NAME}"
OutFile "${BUILD_PATH}\${FILE_NAME}.exe"

RequestExecutionLevel admin

;--------------------------------
; Pages

Page components
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

;--------------------------------

Section "${PRODUCT_NAME} (required)" core_section_id
  SectionIn RO

  Call GetJRE
  Pop $R0

  SetOutPath "$INSTDIR"

  File "${BUILD_PATH}\${PRODUCT_NAME}.jar"
  File "${RESOURCE_PATH}\${PRODUCT_NAME}.bat"
  File "${RESOURCE_PATH}\*.url"
  File /oname=README.txt "${README_FILE}"
  File /oname=COPYING.txt "${COPYING_FILE}"
  File /oname=VERSION.txt "${VERSION_FILE}"

  WriteUninstaller "uninstall.exe"
  ${If} ${RunningX64}
    SetRegView 64
  ${EndIf}

  ${GetSize} "$INSTDIR" "/S=0K /G=1" $0 $1 $2
  IntFmt $0 "0x%08X" $0

  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayName" "${PRODUCT_NAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "InstallLocation" "$\"$INSTDIR\$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayIcon" "$\"$INSTDIR\${PRODUCT_NAME}.ico$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "Publisher" "European Space Agency"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "HelpLink" "http://www.helioviewer.org/wiki/index.php?title=${PRODUCT_NAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "URLUpdateInfo" "http://${PRODUCT_NAME}.org/"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "URLInfoAbout" "http://${PRODUCT_NAME}.org/"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayVersion" "${JHV_VERSION}.${JHV_REVISION}"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "NoModify" "1"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "NoRepair" "1"
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "EstimatedSize" "$0"
SectionEnd

Section "Start Menu Shortcuts" start_menu_section_id
  SetOutPath "$INSTDIR"
  File "${RESOURCE_PATH}\${PRODUCT_NAME}.ico"

  CreateDirectory "$SMPROGRAMS\${PRODUCT_NAME}"
  CreateShortCut "$SMPROGRAMS\${PRODUCT_NAME}\Uninstall.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_NAME}\${PRODUCT_NAME}.lnk" "$INSTDIR\${PRODUCT_NAME}.bat" "" "$INSTDIR\${PRODUCT_NAME}.ico"

  CreateShortCut "$SMPROGRAMS\${PRODUCT_NAME}\Readme.lnk" "$INSTDIR\README.txt"

  SetOutPath "$SMPROGRAMS\${PRODUCT_NAME}\Websites"
  File "${RESOURCE_PATH}\*.url"
SectionEnd

Section "Desktop Shortcuts" desktop_section_id
  SetOutPath "$INSTDIR"
  File "${RESOURCE_PATH}\${PRODUCT_NAME}.ico"

  CreateShortCut "$DESKTOP\${PRODUCT_NAME}.lnk" "$INSTDIR\${PRODUCT_NAME}.bat" "" "$INSTDIR\${PRODUCT_NAME}.ico"
SectionEnd

;--------------------------------
; Uninstaller

Section "Uninstall"

  ; Remove files and uninstaller
  Delete "$INSTDIR\${PRODUCT_NAME}.jar"
  Delete "$INSTDIR\${PRODUCT_NAME}.bat"
  Delete "$INSTDIR\${PRODUCT_NAME}.ico"
  Delete "$INSTDIR\*.url"
  Delete "$INSTDIR\README.txt"
  Delete "$INSTDIR\COPYING.txt"
  Delete "$INSTDIR\VERSION.txt"
  Delete "$INSTDIR\uninstall.exe"

  ; Remove shortcuts, if any
  Delete "$SMPROGRAMS\${PRODUCT_NAME}\Websites\*.*"
  Delete "$SMPROGRAMS\${PRODUCT_NAME}\*.*"
  Delete "$DESKTOP\${PRODUCT_NAME}.lnk"

  ; Remove directories used
  RMDir "$SMPROGRAMS\${PRODUCT_NAME}\Websites"
  RMDir "$SMPROGRAMS\${PRODUCT_NAME}"
  RMDir "$INSTDIR"

  ; Remove keys
  ${If} ${RunningX64}
    SetRegView 64
  ${EndIf}
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"

SectionEnd

Function .onInit
  ${If} ${RunningX64}
    SetRegView 64
  ${EndIf}
  ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "QuietUninstallString"
  StrCmp $R0 "" 0 ask
  ReadRegStr $R0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "UninstallString"
  StrCmp $R0 "" done ask

ask:
  ReadRegStr $R1 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}" "DisplayVersion"
  StrCmp $R1 "" defaultversion dialog

defaultversion:
  StrCpy $R1 "unknown"

dialog:
  MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION "${PRODUCT_NAME} is already installed. $\n$\nCurrent version: $R1$\nNew version: ${JHV_VERSION}.${JHV_REVISION}$\n$\nClick `OK` to remove the previous version or `Cancel` to exit setup." IDOK uninst

  Abort

uninst:
  ClearErrors
  ExecWait "$R0"

done:
FunctionEnd

Function GetJRE
    Push $R0
    Push $R1
    Push $2

  ; 1) Check local JRE
  CheckLocal:
    ClearErrors
    StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
    IfFileExists $R0 JreFound

  ; 2) Check for JAVA_HOME
  CheckJavaHome:
    ClearErrors
    ReadEnvStr $R0 "JAVA_HOME"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfErrors CheckRegistry64
    IfFileExists $R0 0 CheckRegistry64
    Call CheckJREVersion
    IfErrors CheckRegistry64 JreFound

  ; 3) Check for registry 64 bit
  CheckRegistry64:
    ${If} ${RunningX64}
        SetRegView 64
        ClearErrors
        ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
        ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
        StrCpy $R0 "$R0\bin\${JAVAEXE}"
        IfErrors CheckRegistry32
        IfFileExists $R0 0 CheckRegistry32
        Call CheckJREVersion
        IfErrors CheckRegistry32 JreFound
    ${EndIf}

  ; 4) Check for registry 32 bit
  CheckRegistry32:
    SetRegView 32
    ClearErrors
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfErrors DownloadJRE
    IfFileExists $R0 0 DownloadJRE
    Call CheckJREVersion
    IfErrors DownloadJRE JreFound

  DownloadJRE:
    MessageBox MB_ICONINFORMATION "No compatible Java version could be found. ${PRODUCT_NAME} needs the Java Runtime Environment with version ${JRE_VERSION} or higher. You can get the newest Java version from ${JAVA_URL}"
    StrCpy $R0 "${JAVAEXE}"

  JreFound:
    Pop $2
    Pop $R1
    Exch $R0
FunctionEnd

; Pass the "java.exe" path by $R0
Function CheckJREVersion
    Push $R1

    ; Get the file version of javaw.exe
    ${GetFileVersion} $R0 $R1
    ${VersionCompare} ${JRE_VERSION} $R1 $R1

    ; Check whether $R1 != "1"
    ClearErrors
    StrCmp $R1 "1" 0 CheckDone
    SetErrors

  CheckDone:
    Pop $R1
FunctionEnd
