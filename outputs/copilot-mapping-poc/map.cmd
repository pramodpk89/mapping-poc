@echo off
setlocal DisableDelayedExpansion
rem Read a plain UTF-8 key=value path, not a Java-escaped properties string.
for /f "tokens=2 delims=:" %%C in ('chcp') do set "MAPPING_CODEPAGE=%%C"
chcp 65001 >nul
set "MAPPING_JAVA_HOME="
if not exist "%~dp0java-home.properties" goto missing
for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0java-home.properties") do if "%%A"=="java.home" set "MAPPING_JAVA_HOME=%%B"
if not defined MAPPING_JAVA_HOME goto missing
if not exist "%MAPPING_JAVA_HOME%\bin\java.exe" goto invalid
"%MAPPING_JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -jar "%~dp0.framework\mapping.jar" %*
set "MAPPING_RESULT=%ERRORLEVEL%"
chcp %MAPPING_CODEPAGE% >nul
exit /b %MAPPING_RESULT%
:missing
echo Set java.home in java-home.properties to your approved Java 1.8 folder, without quotes.
goto failed
:invalid
echo The configured Java folder has no bin\java.exe. Correct java.home in java-home.properties.
:failed
chcp %MAPPING_CODEPAGE% >nul
exit /b 2
