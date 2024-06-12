#!/bin/sh

export KORGE_FORGE_VERSION=v0.0.2
export FILE_URL=https://github.com/korlibs/korge-forge-installer/releases/download/v0.0.2/korge-forge-installer.jar
export EXPECTED_SHA1=2bfcc019ccfde3eb432626e4e6cf34e95239e787
export FILE_NAME=~/.local/korge-forge-installer-$KORGE_FORGE_VERSION.jar

if [ "$(uname -s)" = 'Darwin' ]; then
  export sha1sum=shasum
else
  export sha1sum=sha1sum
fi

# Download the file if it doesn't exist
if [ ! -f "$FILE_NAME" ]; then
  mkdir -p ~/.local > /dev/null
  echo "File not found. Downloading..."
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

java -jar "$FILE_NAME" $*
