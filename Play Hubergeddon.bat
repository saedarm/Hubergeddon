@echo off
rem Double-click to build (first time only) and play Hubergeddon.
cd /d "%~dp0"
if not defined JAVA_HOME (
  if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
if not exist "lwjgl3\build\libs\Hubergeddon.jar" (
  echo Building Hubergeddon. The first build takes a minute or two...
  call gradlew.bat lwjgl3:dist
  if errorlevel 1 ( echo Build failed. & pause & exit /b 1 )
)
start "" "%JAVA%" -jar "lwjgl3\build\libs\Hubergeddon.jar"
