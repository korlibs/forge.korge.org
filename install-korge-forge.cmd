@echo off

set KORGE_FORGE_VERSION=

echo KorGE Forge Installer %KORGE_FORGE_VERSION%

REM https://github.com/adoptium/temurin21-binaries/releases

set JRE_URL=https://github.com/korlibs/universal-jre/releases/download/0.0.1/OpenJDK21U-jre_x64_windows_hotspot_21.0.3_9.zip
REM set JRE_URL=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jre_x64_windows_hotspot_21.0.3_9.zip
set JRE_SHA1=0a36d67c443387bab59e335b8a073d7d0f3a9575
set JRE_LOCAL=%LOCALAPPDATA%\KorgeForgeInstaller
set JRE_ZIP=%JRE_LOCAL%\jre.zip
set JRE_JAVA_BIN=%JRE_LOCAL%\jdk-21.0.3+9-jre\bin

set INSTALLER_URL=https://github.com/korlibs/korge-forge-installer/releases/download/v0.0.1/korge-forge-installer.jar
set INSTALLER_SHA1=
set INSTALLER_LOCAL_FILE=korge-forge-installer-%KORGE_FORGE_VERSION%.jar
set INSTALLER_LOCAL=%JRE_LOCAL%\%INSTALLER_LOCAL_FILE%

echo Temporary files at: %JRE_LOCAL%

mkdir "%JRE_LOCAL%" 2> NUL

SET DOWNLOAD_URL=%JRE_URL%
SET DOWNLOAD_LOCAL=%JRE_ZIP%
SET DOWNLOAD_SHA1=%JRE_SHA1%
CALL :DOWNLOAD_FILE

SET DOWNLOAD_URL=%INSTALLER_URL%
SET DOWNLOAD_LOCAL=%INSTALLER_LOCAL%
SET DOWNLOAD_SHA1=%INSTALLER_SHA1%
CALL :DOWNLOAD_FILE

if not exist "%JRE_JAVA_BIN%" (
    echo Unzipping %JRE_ZIP%...
    tar -xf "%JRE_ZIP%" -C "%JRE_LOCAL%" 2> NUL > NUL
    echo Ok
)

"%JRE_JAVA_BIN%\java" -jar %INSTALLER_LOCAL%

EXIT /B

:DOWNLOAD_FILE
    REM DOWNLOAD_URL
    REM DOWNLOAD_LOCAL
    REM DOWNLOAD_SHA1

    SET DOWNLOAD_LOCAL_TMP=%DOWNLOAD_LOCAL%.tmp

    if not exist "%DOWNLOAD_LOCAL%" (
        SET /A SHA1_RETRIES=3
        echo Downloading %DOWNLOAD_URL% into: %DOWNLOAD_LOCAL_TMP%
        curl -s -L "%DOWNLOAD_URL%" -o "%DOWNLOAD_LOCAL_TMP%" && timeout /T 1 /NOBREAK > NUL
        powershell -NoProfile -ExecutionPolicy Bypass -Command "(New-Object Net.WebClient).DownloadFile('%DOWNLOAD_URL%', '%DOWNLOAD_LOCAL_TMP:\=\\%')"
        REM powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-BitsTransfer -Source '%DOWNLOAD_URL%' -Destination '%DOWNLOAD_LOCAL_TMP:\=\\%'"

        IF NOT "%DOWNLOAD_SHA1%"=="" (
            :REPEAT_SHA1
            echo Computing hash...
            FOR /f %%i IN (
                'powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Filehash -Path '%DOWNLOAD_LOCAL_TMP:\=\\%' -Algorithm SHA1).Hash"'
            ) DO SET SHA1=%%i

            IF %SHA1_RETRIES% GEQ 0 (
                if /i NOT "%SHA1%"=="%DOWNLOAD_SHA1%" (
                    ECHO "File not ready, retrying in 2 seconds (retries %SHA1_RETRIES%) %DOWNLOAD_LOCAL_TMP:\=\\%"
                    TIMEOUT /T 2 /NOBREAK > NUL
                    SET /A SHA1_RETRIES-=1
                    GOTO REPEAT_SHA1
                )
            )

            if /i NOT "%SHA1%"=="%DOWNLOAD_SHA1%" (
                ECHO "Error downloading file expected '%DOWNLOAD_SHA1%' but found '%SHA1%' from url %DOWNLOAD_URL%"
                EXIT /B 1
            )
        )

        MOVE "%DOWNLOAD_LOCAL_TMP%" "%DOWNLOAD_LOCAL%" 2> NUL > NUL
        echo Ok
    )
EXIT /B
