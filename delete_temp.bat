@echo off
setlocal enabledelayedexpansion

REM Delete the temporary _new.java file
set "FILE=C:\Users\Jitu\AndroidStudioProjects\Aapraksha\app\src\main\java\com\example\aapraksha\SosTriggeredActivity_new.java"

if exist "!FILE!" (
    del "!FILE!"
    echo ✓ Deleted: SosTriggeredActivity_new.java
    timeout /t 2
) else (
    echo File not found: !FILE!
    timeout /t 2
)
