@echo off
REM ============================================================================
REM Delete problematic layout files causing compilation error
REM ============================================================================

setlocal enabledelayedexpansion

cls
echo.
echo ╔════════════════════════════════════════════════════════════════╗
echo ║  Fixing Android Resource Linking Error                        ║
echo ║  Deleting: activity_sos_triggered_new.xml                     ║
echo ╚════════════════════════════════════════════════════════════════╝
echo.

set "FILE=C:\Users\Jitu\AndroidStudioProjects\Aapraksha\app\src\main\res\layout\activity_sos_triggered_new.xml"

if exist "!FILE!" (
    echo [1/3] Found problematic file: activity_sos_triggered_new.xml
    echo.
    
    REM Close Android Studio (optional - helps with file locks)
    echo [2/3] Attempting to delete...
    del "!FILE!" /F /Q 2>nul
    
    if not exist "!FILE!" (
        echo.
        echo ╔════════════════════════════════════════════════════════════════╗
        echo ║ ✓ SUCCESS!                                                     ║
        echo ║ File deleted: activity_sos_triggered_new.xml                   ║
        echo ╚════════════════════════════════════════════════════════════════╝
        echo.
        echo [3/3] Next steps:
        echo.
        echo 1. Go back to Android Studio
        echo 2. Click: Build menu
        echo 3. Click: Clean Project (wait for completion)
        echo 4. Click: Rebuild Project (wait 1-2 minutes)
        echo 5. Look for: "BUILD SUCCESSFUL" message
        echo.
        echo If BUILD SUCCESSFUL appears, the error is fixed!
        echo.
    ) else (
        echo.
        echo ╔════════════════════════════════════════════════════════════════╗
        echo ║ ✗ ERROR: Could not delete file                                ║
        echo ╚════════════════════════════════════════════════════════════════╝
        echo.
        echo This usually happens when Android Studio is locking the file.
        echo.
        echo Solution:
        echo 1. Close Android Studio COMPLETELY
        echo 2. Run this script again
        echo 3. Then reopen Android Studio
        echo.
        echo OR manually delete using File Explorer:
        echo C:\Users\Jitu\AndroidStudioProjects\Aapraksha\
        echo   app\src\main\res\layout\
        echo   activity_sos_triggered_new.xml
        echo.
    )
) else (
    echo [✓] File not found - already deleted!
    echo [✓] You can proceed with building.
    echo.
)

pause
