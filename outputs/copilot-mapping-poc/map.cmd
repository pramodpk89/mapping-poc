@echo off
setlocal DisableDelayedExpansion
rem All text parameters cross the Java 8 Windows boundary in Unicode environment values.
for /f "tokens=2 delims=:" %%C in ('chcp') do set "MAPPING_CODEPAGE=%%C"
chcp 65001 >nul
set "MAPPING_JAVA_HOME="
set "MAPPING_ACTION=%~1"
set "MAPPING_PACK=%~dp0"
set "MAPPING_ROOT=%~dp0"
set "MAPPING_EDITS="
set "MAPPING_IMPORT="
set "MAPPING_OUTPUT="
if not exist "%~dp0java-home.properties" goto missing
for /f "usebackq tokens=1,* delims==" %%A in ("%~dp0java-home.properties") do if "%%A"=="java.home" set "MAPPING_JAVA_HOME=%%B"
if not defined MAPPING_JAVA_HOME goto missing
if not exist "%MAPPING_JAVA_HOME%\bin\java.exe" goto invalid
rem Prefer the existing short path for Java 8's native DLL lookup; never enable short names.
for %%J in ("%MAPPING_JAVA_HOME%") do set "MAPPING_JAVA_EXE=%%~sJ\bin\java.exe"
shift
:options
if "%~1"=="" goto launch
if "%~2"=="" goto badoption
if "%~1"=="--edits" goto edits
if "%~1"=="--import-answers" goto import
if "%~1"=="--output" goto output
if "%~1"=="--root" goto root
goto badoption
:edits
if defined MAPPING_EDITS goto badoption
set "MAPPING_EDITS=%~f2"
goto next
:import
if defined MAPPING_IMPORT goto badoption
set "MAPPING_IMPORT=%~f2"
goto next
:output
if defined MAPPING_OUTPUT goto badoption
set "MAPPING_OUTPUT=%~f2"
goto next
:root
set "MAPPING_ROOT=%~f2"
:next
shift
shift
goto options
:launch
rem A relative JAR name avoids Java 8's ANSI -jar path conversion on Windows.
pushd "%MAPPING_PACK%"
if errorlevel 1 goto failed
"%MAPPING_JAVA_EXE%" -Dfile.encoding=UTF-8 -jar ".framework\mapping.jar" --launcher
set "MAPPING_RESULT=%ERRORLEVEL%"
popd
chcp %MAPPING_CODEPAGE% >nul
exit /b %MAPPING_RESULT%
:missing
echo Set java.home in java-home.properties to your approved Java 1.8 folder, without quotes.
goto failed
:invalid
echo The configured Java folder has no bin\java.exe. Correct java.home in java-home.properties.
goto failed
:badoption
echo Invalid, repeated or missing option. See RUN.md for supported commands.
:failed
chcp %MAPPING_CODEPAGE% >nul
exit /b 2
