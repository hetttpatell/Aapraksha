@echo off
REM Delete temporary SOS file and fix compilation error
REM This script removes the duplicate SosTriggeredActivity_new.java file

setlocal enabledelayedexpansion

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║   Deleting SosTriggeredActivity_new.java - Compilation Fix    ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

set "FILE=C:\Users\Jitu\AndroidStudioProjects\Aapraksha\app\src\main\java\com\example\aapraksha\SosTriggeredActivity_new.java"

if exist "!FILE!" (
    echo [*] Found file: !FILE!
    echo [*] Attempting to delete...
    
    REM Try to delete
    del "!FILE!" /F /Q 2>nul
    
    REM Check if deletion was successful
    if not exist "!FILE!" (
        echo.
        echo [✓] SUCCESS! File deleted.
        echo [✓] Temporary file removed.
        echo.
        echo Next steps:
        echo 1. Open Android Studio
        echo 2. Click: Build → Clean Project
        echo 3. Click: Build → Rebuild Project
        echo 4. Wait for "BUILD SUCCESSFUL"
        echo.
    ) else (
        echo.
        echo [✗] ERROR: Could not delete file
        echo [!] File is likely locked by Android Studio
        echo.
        echo Solution:
        echo 1. Close Android Studio completely
        echo 2. Run this script again
        echo 3. OR manually delete:
        echo    !FILE!
        echo.
    )
) else (
    echo [✓] File not found - already deleted!
    echo [✓] You can proceed with building the project.
    echo.
)

pause
