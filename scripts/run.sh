#!/bin/bash

JAR_FILE=$(find . -maxdepth 1 -name "kleber*.jar" | head -n 1)

if [[ -z "$JAR_FILE" ]]; then
  echo "No .jar file starting with 'kleber' found in the current directory."
  exit 1
fi

echo "Running $JAR_FILE..."
java -jar "$JAR_FILE"
