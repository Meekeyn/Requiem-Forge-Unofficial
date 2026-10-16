cd /d I:
cd /d "%~dp0"
@REM cd /d "I:\***"
@REM call gradlew build
call gradlew.bat --offline runClient

@REM explorer "I:\***\build\libs"
explorer "%~dp0build\libs"
gradlew build
pause