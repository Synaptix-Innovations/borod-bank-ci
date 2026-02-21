#!/bin/bash

# Borod Bank - Development Server Startup Script
# This script starts the application with H2 database and hot reload enabled

PORT=8080

# Check if port 8080 is in use
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1 || ss -tlnp 2>/dev/null | grep -q ":8080 "; then
    echo "⚠️  Port 8080 is already in use. Using port 8081 instead."
    PORT=8081
fi

echo "🏦 Starting Borod Bank Development Server..."
echo ""
echo "Features:"
echo "  ✓ H2 in-memory database (no MySQL required)"
echo "  ✓ Hot reload for CSS/HTML changes"
echo "  ✓ Auto-restart on file changes"
echo ""
echo "Open in browser: http://localhost:$PORT"
echo ""
echo "To stop: Press Ctrl+C"
echo "================================================"
echo ""

if [ "$PORT" = "8081" ]; then
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev -Dspring-boot.run.arguments="--server.port=8081"
else
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev
fi
