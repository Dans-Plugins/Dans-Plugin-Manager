#!/bin/bash

# Script to rebuild and hot-reload the Dans Plugin Manager plugin during development
# This script requires the test server to be running with ServerUtils plugin

# Build the plugin
echo "Building Dans Plugin Manager plugin..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed. Exiting."
    exit 1
fi

# Get the container ID
CONTAINER_ID=$(docker ps -qf "name=dpm-test-mc-server")

if [ -z "$CONTAINER_ID" ]; then
    echo "Error: Test server container not found. Make sure to start it with './up.sh'"
    exit 1
fi

# Copy the new plugin jar to the server
echo "Copying new plugin jar to test server..."
LATEST_JAR=$(find target -maxdepth 1 -name "DansPluginManager-*.jar" -not -name "original-*" -type f -print -quit)

if [ -z "$LATEST_JAR" ]; then
    echo "Error: No plugin jar found in target/"
    exit 1
fi

docker cp "$LATEST_JAR" "$CONTAINER_ID":/testmcserver/plugins/

# Execute the reload command via ServerUtils
echo "Reloading Dans Plugin Manager plugin..."
docker exec "$CONTAINER_ID" /bin/bash -c "echo 'serverutils reload DansPluginManager' >> /tmp/reload_command"
echo "Plugin reload initiated. Check server console for confirmation."

echo ""
echo "To manually reload the plugin in-game or via console, use:"
echo "  /serverutils reload DansPluginManager"
echo ""
echo "Other useful ServerUtils commands:"
echo "  /serverutils list - List all plugins"
echo "  /serverutils unload DansPluginManager - Unload the plugin"
echo "  /serverutils load DansPluginManager - Load the plugin"
