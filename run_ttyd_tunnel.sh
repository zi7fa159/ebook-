#!/bin/bash

# Start ttyd in the background
ttyd bash > ttyd.log 2>&1 &
TTYD_PID=$!

# Start cloudflared quick tunnel in the background
cloudflared tunnel --url http://localhost:7681 > cloudflared.log 2>&1 &
CLOUDFLARED_PID=$!

echo "ttyd started with PID $TTYD_PID"
echo "cloudflared started with PID $CLOUDFLARED_PID"

# Wait for the tunnel URL to appear
echo "Waiting for Cloudflare Tunnel URL..."
for i in {1..20}; do
    URL=$(grep -ao "https://[-a-z0-9.]*\.trycloudflare\.com" cloudflared.log | head -n 1)
    if [ -n "$URL" ]; then
        echo "Tunnel URL: $URL"
        break
    fi
    sleep 1
done

if [ -z "$URL" ]; then
    echo "Failed to find tunnel URL in cloudflared.log"
    exit 1
fi
