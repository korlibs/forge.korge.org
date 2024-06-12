#!/bin/sh

export KORGE_FORGE_VERSION=v0.1.1
echo KorGE Forge Installer $KORGE_FORGE_VERSION

export INSTALLER_URL=https://github.com/korlibs/korge-forge-installer/releases/download/$KORGE_FORGE_VERSION/korge-forge-installer.jar
export INSTALLER_SHA1=8b29794bbc14e50f7c4d9c4b673aabbbdcf6cfd1
export INSTALLER_LOCAL=$HOME/.local/korge-forge-installer-$KORGE_FORGE_VERSION.jar

if [ "$(uname -s)" = 'Darwin' ]; then
  export sha1sum=shasum
else
  export sha1sum=sha1sum
fi

mkdir -p ~/.local > /dev/null

download_file()
{
  FILE_URL=$1
  FILE_NAME=$2
  EXPECTED_SHA1=$3
  shift; shift; shift;

  # Download the file if it doesn't exist
  if [ ! -f "$FILE_NAME" ]; then
    echo "File not found. Downloading... $FILE_URL"
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

    echo "File verification succeeded."
  fi
}

download_file "$INSTALLER_URL" "$INSTALLER_LOCAL" "$INSTALLER_SHA1"
if [ "$(uname -s)" = 'Darwin' ]; then
  export java=$HOME/.local/jdk-21+35-jre/Contents/jre/bin/java
  export LOCAL_JRE_ZIP=$HOME/.local/macos-universal-jdk-21+35-jre.tar.gz
  download_file "https://github.com/korlibs/universal-jre/releases/download/0.0.1/macos-universal-jdk-21+35-jre.tar.gz" "$LOCAL_JRE_ZIP" "5bfa5a0ba39852ce5151012d9e1cc3c6ce96f317"
  if [ ! -f "$java" ]; then
    echo Extracting JRE...
    tar -xzf "$LOCAL_JRE_ZIP" -C "$HOME/.local"
  fi
else
  export java=java
fi

"$java" -jar "$INSTALLER_LOCAL" $*
