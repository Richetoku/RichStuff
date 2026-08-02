@echo off
call "%~dp0..\module.bat" RichStuff runClient %*
exit /b %ERRORLEVEL%
