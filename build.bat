@echo off
cd /d "C:\Users\Jitu\AndroidStudioProjects\Aapraksha"
echo Building Aapraksha project...
echo.
call gradlew.bat clean build
echo.
if %ERRORLEVEL% == 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo The lambda expression error has been fixed.
    echo The project compiled without errors.
    echo.
) else (
    echo.
    echo ========================================
    echo BUILD FAILED
    echo ========================================
    echo.
)
pause
