@echo off
%COMSPEC% /c
if not exist ".\jre\bin\java.exe" goto trypath
".\jre\bin\java.exe" -jar JHelioviewer.jar
pause
exit

:trypath
%COMSPEC% /c
for %%A in ("%PATH:;=" "%") do (dir %%A | find "java.exe" >NUL & if not errorLevel 1 goto inpath )
goto tryhome
:inpath
"java.exe" -jar JHelioviewer.jar
pause
exit

:tryhome
%COMSPEC% /c
if not exist "%JAVA_HOME%\bin\java.exe" goto tryregistry
"%JAVA_HOME%\bin\java.exe" -jar JHelioviewer.jar
pause 
exit

:tryregistry
%COMSPEC% /c
FOR /F "skip=2 tokens=2* delims=	 " %%A IN ('"reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment" /v CurrentVersion 2>NUL"') DO SET Version=%%B
if not defined Version goto tryregistrywow6432
FOR /F "skip=2 tokens=2* delims=	 " %%A IN ('"reg query "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment\%Version%" /v JavaHome 2>NUL"') DO SET JavaHome=%%B
if not defined JavaHome goto tryregistrywow6432
"%JavaHome%\bin\java.exe" -jar JHelioviewer.jar
pause
exit


:tryregistrywow6432
%COMSPEC% /c
FOR /F "skip=2 tokens=2* delims=	 " %%A IN ('"reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment" /v CurrentVersion 2>NUL"') DO SET Version=%%B
if not defined Version goto ERROR
FOR /F "skip=2 tokens=2* delims=	 " %%A IN ('"reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment\%Version%" /v JavaHome 2>NUL"') DO SET JavaHome=%%B
if not defined JavaHome goto ERROR
"%JavaHome%\bin\java.exe" -jar JHelioviewer.jar
pause
exit

:ERROR
echo "ERROR: Could not find Java Runtime Environment! JHelioviewer needs Java 1.6 or higher. You can get the latest version from http://www.java.com/download/"
echo "Try to start JHelioviewer without specifying java.exe using default associated program."
echo "Console log output wont be available. Log files are stored in %HOME%\JHelioviewer\Logs"
start JHelioviewer.jar
pause
exit

