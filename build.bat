@echo off
call "%~dp0..\module.bat" RichStuff build %*
exit /b %ERRORLEVEL%
