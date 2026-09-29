@echo off
setlocal
cd /d "%~dp0"
where py >nul 2>nul
if not errorlevel 1 (
  py -3 render.py
) else (
  python render.py
)
if errorlevel 1 (
  echo Report generation failed. Check the message above. Python 3.9 or newer is required.
  pause
  exit /b 1
)
start "" "report.html"
pause
