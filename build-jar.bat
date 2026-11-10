@echo off
rem ============================================================
rem Build the executable Spring Boot JAR (default packaging).
rem Output: application\target\halo-<version>.jar
rem Usage : run from any shell; no arguments required.
rem ============================================================
setlocal
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
    echo [build-jar] ERROR: Maven ^(mvn^) not found in PATH.
    exit /b 1
)

echo [build-jar] Packaging executable JAR, skipping tests...
call mvn -B -DskipTests package
if errorlevel 1 (
    echo [build-jar] BUILD FAILED.
    exit /b 1
)

echo.
echo [build-jar] BUILD SUCCESS. Artifact:
for %%f in ("application\target\halo-*.jar") do echo   %%f
echo.
echo [build-jar] Run it with:
echo   java -jar application\target\halo-2.28.0-SNAPSHOT.jar --spring.profiles.active=mariadb --halo.work-dir=H:\halo2-maven-test
endlocal
