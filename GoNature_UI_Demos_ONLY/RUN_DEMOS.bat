@echo off
setlocal enabledelayedexpansion
title GoNature UI/UX Demos Launcher

rem --- JavaFX SDK bundled with your project ---
set "FX=%~dp0..\GoNatureClient\javafx-sdk-26.0.1\lib"

if not exist "%FX%" (
  echo.
  echo Could not find JavaFX at:
  echo    %FX%
  echo Edit this file and set FX to your javafx-sdk\lib folder.
  echo.
  pause
  exit /b 1
)

:menu
cls
echo ==================================================
echo            GoNature  -  UI / UX  Demos
echo ==================================================
echo    1   Dark Nature Command Center
echo    2   Glass Eco Explorer
echo    3   Premium Booking Wizard
echo    4   Map-First Park Operations
echo    5   Clean Government Service
echo    6   Luxury Traveler App
echo    7   Admin Analytics Studio
echo    8   Role-Based Control Hub
echo    9   Modern Kiosk Check-In
echo   10   Future Web App Desktop
echo    0   Quit
echo ==================================================
set /p choice="Pick a demo number then press Enter: "

if "%choice%"=="0" exit /b 0
set "name="
if "%choice%"=="1"  set "name=Demo_01_Dark_Nature_Command_Center"
if "%choice%"=="2"  set "name=Demo_02_Glass_Eco_Explorer"
if "%choice%"=="3"  set "name=Demo_03_Premium_Booking_Wizard"
if "%choice%"=="4"  set "name=Demo_04_Map_First_Park_Operations"
if "%choice%"=="5"  set "name=Demo_05_Clean_Government_Service"
if "%choice%"=="6"  set "name=Demo_06_Luxury_Traveler_App"
if "%choice%"=="7"  set "name=Demo_07_Admin_Analytics_Studio"
if "%choice%"=="8"  set "name=Demo_08_Role_Based_Control_Hub"
if "%choice%"=="9"  set "name=Demo_09_Modern_Kiosk_CheckIn"
if "%choice%"=="10" set "name=Demo_10_Future_Web_App_Desktop"

if not defined name (
  echo Invalid choice, try again.
  timeout /t 1 >nul
  goto menu
)

echo.
echo Launching %name% ...  (close the window to come back to this menu)
java --module-path "%FX%" --add-modules javafx.controls,javafx.graphics,javafx.base "%~dp0%name%\%name%.java"
if errorlevel 1 (
  echo.
  echo Something went wrong launching the demo. Make sure Java is installed and on PATH.
  pause
)
goto menu
