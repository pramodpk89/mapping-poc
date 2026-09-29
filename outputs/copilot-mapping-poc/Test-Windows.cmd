@echo off
rem Uses the configured Java 8 runtime; no downloads or execution-policy changes.
call "%~dp0map.cmd" test %*
exit /b %ERRORLEVEL%
