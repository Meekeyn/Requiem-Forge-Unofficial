cd /d I:
cd /d "%~dp0"
@REM cd /d "I:\***"
call gradlew build
@REM explorer "I:\***\build\libs"
explorer "%~dp0build\libs"
gradlew build
pause