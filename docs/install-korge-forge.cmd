@ECHO OFF

SETLOCAL enabledelayedexpansion

SET KORGE_FORGE_VERSION=v2025.2.1
SET INSTALLER_URL=https://github.com/korlibs/korge-forge-installer/releases/download/v2025.2.1/korge-forge-installer.jar
SET INSTALLER_SHA256=fb8e7176f990efe1ec3cb71505ab5d595fd9d506bf7d3766b9dc4ea0370cc708

ECHO KorGE Forge Installer %KORGE_FORGE_VERSION%

CALL :NORMALIZE_PATH .\korge-forge-installer

SET INSTALLER_PATH=%RETVAL%
MKDIR "%INSTALLER_PATH%"
SET INSTALLER_LOCAL_FILE=%INSTALLER_PATH%\korge-forge-installer-%KORGE_FORGE_VERSION%.jar

SET KORGEDIR=%USERPROFILE%\.korge
SET JAVA=%KORGEDIR%\jre-21\bin\java.exe
MKDIR "%KORGEDIR%" 2> NUL

SET XZ=%KORGEDIR%\xzdec.exe

IF NOT EXIST "%XZ%" (
    CALL :DOWNLOAD_FILE "https://github.com/korlibs/universal-jre/releases/download/0.0.1/xzdec.exe" "%XZ%" "D55CF3EE369E407AD2D9EF1098C3DDE112139593226A68E42396D1EA392C8037"
)

IF NOT EXIST "%INSTALLER_LOCAL_FILE%" (
    CALL :DOWNLOAD_FILE "%INSTALLER_URL%" "%INSTALLER_LOCAL_FILE%" "%INSTALLER_SHA256%"
)

IF NOT EXIST "%JAVA%" (

    IF "%PROCESSOR_ARCHITECTURE%" == "ARM64" (
        REM echo ARM
        CALL :DOWNLOAD_FILE "https://github.com/korlibs/universal-jre/releases/download/0.0.1/microsoft-jre-21.0.3-windows-aarch64.tar.xz" "%KORGEDIR%\jre-21.tar.xz" "8F18060960FD7935D76C79BBB643B1779440B73AE9715153A3BA332B1B8A2348"
    ) ELSE (
        REM echo X64
        CALL :DOWNLOAD_FILE "https://github.com/korlibs/universal-jre/releases/download/0.0.1/microsoft-jre-21.0.3-windows-x64.tar.xz" "%KORGEDIR%\jre-21.tar.xz" "6D16528A2201DCBE0ADDB0622F5CBE0CD6FA84AE937D3830FC1F74B32132C37B"
    )

    CALL :EXTRACT_TAR "%KORGEDIR%\jre-21.tar.xz" "%KORGEDIR%\jre-21" 1 "%JAVA%"

)

cd "%INSTALLER_PATH%"
"%JAVA%" -jar "%INSTALLER_LOCAL_FILE%" %*

EXIT /B

:DOWNLOAD_FILE

    SET URL=%~1
    SET LOCAL_PATH=%~2
    SET EXPECTED_SHA256=%~3

    IF EXIST "%LOCAL_PATH%" (
        EXIT /B
    )

    IF NOT EXIST "%LOCAL_PATH%.tmp" (
        echo Downloading %URL% into %LOCAL_PATH%
        curl -sL "%URL%" -o "%LOCAL_PATH%.tmp"
    )
    powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Filehash -Path '%LOCAL_PATH:\=\\%.tmp' -Algorithm SHA256).Hash" > "%LOCAL_PATH%.sha256"
    SET /p DOWNLOAD_SHA256=<"%LOCAL_PATH%.sha256"

    IF /i "%DOWNLOAD_SHA256%" == "%EXPECTED_SHA256%" (
        MOVE "%LOCAL_PATH%.tmp" "%LOCAL_PATH%" > NUL 2> NUL
    ) ELSE (
        ECHO ERROR downloading %URL%, SHA256=%DOWNLOAD_SHA256%, but expected SHA256=%EXPECTED_SHA256%
        EXIT /B -1
    )

EXIT /b

:EXTRACT_TAR
    SET INPUT_FILE=%~1
    SET OUT=%~2
    SET STRIP_COMPONENTS=%~3
    SET CHECK_EXISTS=%~4

    IF EXIST %CHECK_EXISTS% (
        EXIT /B
    )

    MKDIR "%OUT%" > NUL 2> NUL
    echo Extracting %INPUT_FILE%...
    REM tar --strip-components %STRIP_COMPONENTS% -C "%OUT%" -xf "%INPUT_FILE%"
    "%XZ%" -d < "%INPUT_FILE%" | tar --strip-components %STRIP_COMPONENTS% -C "%OUT%" -xf -
EXIT /b

:NORMALIZE_PATH
  SET RETVAL=%~f1
EXIT /B
