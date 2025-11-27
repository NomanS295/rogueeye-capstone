#!/bin/bash
set -e

ADAPTER="wlxcc641aeb88bf"
SCANNER_DIR="/home/shahid/Capstone/scanner"
WEB_DIR="/home/shahid/Capstone/web"
PYTHON="/usr/bin/python3"
LOG_FILE="/tmp/scan.log"
FLASK_LOG="/tmp/flask.log"

echo "[*] Setting Wi-Fi adapter to monitor mode..."
sudo ip link set "$ADAPTER" down || true
sudo iw dev "$ADAPTER" set type monitor || true
sleep 2
sudo ip link set "$ADAPTER" up || true
sleep 2

echo "[*] Killing any old scanner or API processes..."
sudo pkill -f "scan.py" || true
sudo pkill -f "scannerAPI.py" || true

echo "[*] Starting Wi-Fi scanner (with sudo)..."
sudo nohup "$PYTHON" "$SCANNER_DIR/scan.py" "$ADAPTER" -d 9999999 > "$LOG_FILE" 2>&1 &

sleep 5
echo "[*] Starting Flask API (normal user)..."
nohup "$PYTHON" "$WEB_DIR/scannerAPI.py" > "$FLASK_LOG" 2>&1 &
sleep 5

echo "[*] Checking Flask API status..."
if curl -s http://127.0.0.1:5000/scans > /dev/null; then
  echo "[✓] Flask API is running on port 5000."
else
  echo "[❌] Flask API did not start – check $FLASK_LOG."
fi

while true; do
  curl -s http://127.0.0.1:5000/scans  > /dev/null || echo "[WARN] /scans not responding"
  curl -s http://127.0.0.1:5000/alerts > /dev/null || echo "[WARN] /alerts not responding"
  sleep 60
done &

