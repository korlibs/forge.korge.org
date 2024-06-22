@echo off
SETLOCAL
SETLOCAL EnableDelayedExpansion

SET KORGE_FORGE_VERSION=v0.1.7
ECHO KorGE Forge Installer %KORGE_FORGE_VERSION%

CALL :NORMALIZEPATH .\korge-forge-installer

REM SET INSTALLER_PATH=%LOCALAPPDATA%\KorgeForgeInstaller
SET INSTALLER_PATH=%RETVAL%

SET INSTALLER_URL=https://github.com/korlibs/korge-forge-installer/releases/download/v0.1.7/korge-forge-installer.jar
SET INSTALLER_SHA1=271b49d02e67e21730fc9fdb4e0688e40d3cec7c
SET INSTALLER_LOCAL_FILE=korge-forge-installer-%KORGE_FORGE_VERSION%.jar
SET INSTALLER_LOCAL=%INSTALLER_PATH%\%INSTALLER_LOCAL_FILE%

SET JRE_URL=https://github.com/korlibs/universal-jre/releases/download/0.0.1/OpenJDK21U-jre_x64_windows_hotspot_21.0.3_9.zip
REM SET JRE_URL=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jre_x64_windows_hotspot_21.0.3_9.zip
SET JRE_SHA1=0a36d67c443387bab59e335b8a073d7d0f3a9575
SET JRE_ZIP=%INSTALLER_PATH%\jre.zip
SET JRE_JAVA_BIN=%INSTALLER_PATH%\jdk-21.0.3+9-jre\bin

REM https://github.com/adoptium/temurin21-binaries/releases

ECHO Temporary files at: %INSTALLER_PATH%

MKDIR "%INSTALLER_PATH%" 2> NUL

SET DOWNLOAD_URL=%INSTALLER_URL%
SET DOWNLOAD_LOCAL=%INSTALLER_LOCAL%
SET DOWNLOAD_SHA1=%INSTALLER_SHA1%
CALL :DOWNLOAD_FILE

IF NOT EXIST "%JRE_JAVA_BIN%" (
    SET DOWNLOAD_URL=%JRE_URL%
    SET DOWNLOAD_LOCAL=%JRE_ZIP%
    SET DOWNLOAD_SHA1=%JRE_SHA1%
    CALL :DOWNLOAD_FILE

    ECHO Unzipping %JRE_ZIP%...
    tar -xf "%JRE_ZIP%" -C "%INSTALLER_PATH%" 2> NUL > NUL
    DEL %JRE_ZIP%
    ECHO Ok
)

CD %INSTALLER_PATH%
"%JRE_JAVA_BIN%\java" -jar "%INSTALLER_LOCAL%" %*
CD ..

EXIT /B

:DOWNLOAD_FILE
    REM DOWNLOAD_URL
    REM DOWNLOAD_LOCAL
    REM DOWNLOAD_SHA1

    SET DOWNLOAD_LOCAL_TMP=%DOWNLOAD_LOCAL%.tmp
    SET /A SHA1_RETRIES=3

    IF NOT EXIST "%DOWNLOAD_LOCAL%" (
        ECHO Downloading %DOWNLOAD_URL% into: %DOWNLOAD_LOCAL_TMP%
        REM curl -s -L "%DOWNLOAD_URL%" -o "%DOWNLOAD_LOCAL_TMP%" && timeout /T 1 /NOBREAK > NUL
        powershell -NoProfile -ExecutionPolicy Bypass -Command "(New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%DOWNLOAD_LOCAL_TMP:\=\\%')"
        REM powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-BitsTransfer -Source '%DOWNLOAD_URL%' -Destination '%DOWNLOAD_LOCAL_TMP:\=\\%'"

        IF NOT "%DOWNLOAD_SHA1%"=="" (
            :REPEAT_SHA1
            REM ECHO Computing hash...
            FOR /f %%i IN (
                'powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Filehash -Path '%DOWNLOAD_LOCAL_TMP:\=\\%' -Algorithm SHA1).Hash"'
            ) DO SET SHA1=%%i

            IF NOT "%SHA1_RETRIES%"=="0" (
                IF /i NOT "%SHA1%"=="%DOWNLOAD_SHA1%" (
                    REM ECHO "File not ready, retrying in 2 seconds (retries %SHA1_RETRIES%) %DOWNLOAD_LOCAL_TMP:\=\\%"
                    TIMEOUT /T 2 /NOBREAK > NUL
                    SET /A SHA1_RETRIES-=1
                    GOTO :REPEAT_SHA1
                )
            )

            IF /i NOT "%SHA1%"=="%DOWNLOAD_SHA1%" (
                ECHO "Error downloading file expected '%DOWNLOAD_SHA1%' but found '%SHA1%' from url %DOWNLOAD_URL%"
                ECHO "URL: %DOWNLOAD_URL%"
                ECHO "SHA1: %DOWNLOAD_SHA1%"
                ECHO "LOCAL: %DOWNLOAD_LOCAL_TMP%"
                EXIT /B -1
            )
        )

        MOVE "%DOWNLOAD_LOCAL_TMP%" "%DOWNLOAD_LOCAL%" 2> NUL > NUL
        REM ECHO Ok
    )
EXIT /B

:NORMALIZEPATH
  SET RETVAL=%~f1
EXIT /B
