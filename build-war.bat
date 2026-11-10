@echo off
rem ============================================================
rem Build the Spring Boot WAR (executable, run with java -jar).
rem Output: application\target\halo-<version>.war
rem
rem NOTE: Halo is a reactive (WebFlux) application. External
rem servlet containers (standalone Tomcat) are NOT supported by
rem Spring Boot for WebFlux apps; run the WAR directly with
rem `java -jar` instead. The WAR form exists mainly for
rem operation processes that require a .war artifact.
rem
rem It temporarily switches application\pom.xml packaging from
rem jar to war, builds, then ALWAYS restores the original pom,
rem so the default JAR build is never affected.
rem ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
    echo [build-war] ERROR: Maven ^(mvn^) not found in PATH.
    exit /b 1
)

set POM=application\pom.xml
set BAK=application\pom.xml.build-war.bak

if not exist "%POM%" (
    echo [build-war] ERROR: %POM% not found. Run this script from the repository root.
    exit /b 1
)

rem ---- switch packaging to war ----
copy /y "%POM%" "%BAK%" >nul
powershell -NoProfile -Command "(Get-Content '%POM%' -Raw) -replace '<packaging>jar</packaging>', '<packaging>war</packaging>' | Set-Content '%POM%' -NoNewline -Encoding ascii"
findstr /c:"<packaging>war</packaging>" "%POM%" >nul
if errorlevel 1 (
    echo [build-war] ERROR: failed to switch pom packaging to war. Restoring...
    copy /y "%BAK%" "%POM%" >nul
    del "%BAK%"
    exit /b 1
)
echo [build-war] pom packaging switched to war.

rem ---- build ----
echo [build-war] Packaging WAR, skipping tests...
call mvn -B -DskipTests package
set BUILD_RC=%errorlevel%

rem ---- always restore the original pom ----
copy /y "%BAK%" "%POM%" >nul
del "%BAK%"
echo [build-war] pom packaging restored to jar.

if not "%BUILD_RC%"=="0" (
    echo [build-war] BUILD FAILED.
    exit /b 1
)

echo.
echo [build-war] BUILD SUCCESS. Artifact:
for %%f in ("application\target\halo-*.war") do echo   %%f
echo.
echo [build-war] Run it directly ^(WebFlux apps cannot run inside
echo   external Tomcat; that is a Spring Boot limitation^):
echo   java -jar application\target\halo-2.28.0-SNAPSHOT.war --spring.profiles.active=mariadb --halo.work-dir=H:\halo2-maven-test
endlocal
