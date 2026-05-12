#!/bin/bash
# Run once to generate the Gradle wrapper files.
# Requires Gradle: brew install gradle
set -e
echo "Generating Gradle wrapper (requires Gradle installed)..."
gradle wrapper --gradle-version 8.6
chmod +x gradlew
echo "Done. Run './gradlew tasks' to verify."
