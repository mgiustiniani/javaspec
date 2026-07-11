@echo off
rem javaspec launcher for Windows
setlocal

set "VERSION=0.1.0-SNAPSHOT"
set "BIN_DIR=%~dp0"
set "REPO_ROOT=%BIN_DIR%.."

set "LOCAL_REPO=%USERPROFILE%\.m2\repository\io\github\jvmspec\javaspec\%VERSION%\javaspec-%VERSION%.jar"
set "TARGET_JAR=%REPO_ROOT%\target\javaspec-%VERSION%.jar"

if exist "%LOCAL_REPO%" (
  set "JAR=%LOCAL_REPO%"
) else if exist "%TARGET_JAR%" (
  set "JAR=%TARGET_JAR%"
) else (
  echo javaspec: jar not found. >&2
  echo Run: mvn -q package  (or: mvn -q -DskipTests install) >&2
  exit /b 1
)

java -jar "%JAR%" %*
endlocal
