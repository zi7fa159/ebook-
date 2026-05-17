#!/bin/bash

# Configuration
PORT=7681
USER="admin"
PASS="password123" # Change this in production!

# Kill existing processes
kill $(pgrep ttyd) $(pgrep lt) 2>/dev/null || true

# Start ttyd in writable mode with authentication
echo "Starting ttyd on port $PORT..."
ttyd -W -p $PORT -c $USER:$PASS bash > ttyd.log 2>&1 &

# Wait for ttyd to start
sleep 2

# Start localtunnel
echo "Starting localtunnel..."
lt --port $PORT > lt.log 2>&1 &

# Wait for localtunnel to get a URL
sleep 5

# Display the URL
URL=$(cat lt.log | grep -o 'https://[^ ]*')
echo "Your public URL is: $URL"
echo "Credentials: $USER / $PASS"
