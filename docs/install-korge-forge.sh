#!/bin/sh

export KORGE_FORGE_VERSION=v0.1.6
echo KorGE Forge Installer $KORGE_FORGE_VERSION

export SCRIPT_DIR="$(dirname "$(realpath "$0")")"

#export INSTALLER_PATH=$HOME/.local
export INSTALLER_PATH=$SCRIPT_DIR/korge-forge-installer

export INSTALLER_URL=https://github.com/korlibs/korge-forge-installer/releases/download/v0.1.6/korge-forge-installer.jar
export INSTALLER_SHA1=630e573212085e7c3e4ffad6aaa04ee180cd9672
export INSTALLER_LOCAL=$INSTALLER_PATH/korge-forge-installer-$KORGE_FORGE_VERSION.jar

echo Working directory... "$INSTALLER_PATH"

if [ "$(uname -s)" = 'Darwin' ]; then
  export sha1sum=shasum
else
  export sha1sum=sha1sum
fi

mkdir -p "$INSTALLER_PATH" > /dev/null

download_file()
{
  FILE_URL=$1
  FILE_NAME=$2
  EXPECTED_SHA1=$3
  shift; shift; shift;

  # Download the file if it doesn't exist
  if [ ! -f "$FILE_NAME" ]; then
    echo "Downloading... $FILE_URL"
    curl -s -L "$FILE_URL" -o "$FILE_NAME.tmp"
    if [ $? -ne 0 ]; then
      echo "Failed to download the file."
      exit 1
    fi

    # Calculate the SHA-1 checksum of the downloaded file
    ACTUAL_SHA1=$($sha1sum "$FILE_NAME.tmp" | awk '{ print $1 }')

    # Compare the actual SHA-1 checksum with the expected one
    if [ "${ACTUAL_SHA1}" != "${EXPECTED_SHA1}" ]; then
      echo "SHA-1 checksum does not match for $FILE_URL in $FILE_NAME.tmp"
      echo "Expected: $EXPECTED_SHA1"
      echo "Actual:   $ACTUAL_SHA1"
      exit 1
    fi

    mv "$FILE_NAME.tmp" "$FILE_NAME"

    # echo "File verification succeeded."
  fi
}

download_file "$INSTALLER_URL" "$INSTALLER_LOCAL" "$INSTALLER_SHA1"
if [ "$(uname -s)" = 'Darwin' ]; then
  export java=$INSTALLER_PATH/jdk-21+35-jre/Contents/jre/bin/java
  if [ ! -f "$java" ]; then
    export LOCAL_JRE_ZIP=$INSTALLER_PATH/macos-universal-jdk-21+35-jre.tar.gz
    download_file "https://github.com/korlibs/universal-jre/releases/download/0.0.1/macos-universal-jdk-21+35-jre.tar.gz" "$LOCAL_JRE_ZIP" "5bfa5a0ba39852ce5151012d9e1cc3c6ce96f317"
    echo Extracting JRE...
    tar -xzf "$LOCAL_JRE_ZIP" -C "$INSTALLER_PATH"
    rm $LOCAL_JRE_ZIP
  fi
else
  export java=java
fi

cd $INSTALLER_PATH && "$java" -jar "$INSTALLER_LOCAL" "$@"; cd ..
