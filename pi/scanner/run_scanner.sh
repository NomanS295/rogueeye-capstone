#!/usr/bin/env bash
# Wrapper for scan.py: ensures monitor mode, runs scan, uploads to S3
# Author: Noman & Shahid - Capstone 2025

# ===== ENVIRONMENT =====
# Load environment and AWS credentials for non-interactive sudo runs
source /home/shahid/.zshrc 2>/dev/null || source /home/shahid/.bashrc 2>/dev/null
export AWS_ACCESS_KEY_ID="AKIA2K3COU3P7FFRKUW2"
export AWS_SECRET_ACCESS_KEY="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"
export AWS_DEFAULT_REGION="us-east-2"

# ===== VARIABLES =====
ADAPTER="wlxcc641aeb88bf"
SCANNER="/home/shahid/Capstone/scanner/scan.py"
LOG="/home/shahid/Capstone/logs/scan.log"
PYTHON="/usr/bin/python3"
S3_BUCKET="capstone-2025-wifi-scanner-data-noman"
SCAN_JSON="/home/shahid/Capstone/scanner/scan_results.json"
ALERT_JSON="/home/shahid/Capstone/scanner/alert.json"

mkdir -p "$(dirname "$LOG")"

# ===== ENABLE MONITOR MODE =====
echo "[*] $(date) - Setting Wi-Fi adapter ($ADAPTER) to monitor mode..." | tee -a "$LOG"
sudo ip link set "$ADAPTER" down
sudo iw dev "$ADAPTER" set type monitor
sleep 1
sudo ip link set "$ADAPTER" up
sleep 1

# ===== VERIFY MODE =====
MODE=$(sudo iw dev "$ADAPTER" info | grep type | awk '{print $2}')
if [[ "$MODE" != "monitor" ]]; then
    echo "[❌] Adapter $ADAPTER failed to enter monitor mode (current: $MODE)" | tee -a "$LOG"
    exit 1
fi

# ===== RUN SCANNER =====
echo "[*] $(date) - Starting scan.py as root..." | tee -a "$LOG"
sudo "$PYTHON" "$SCANNER" >> "$LOG" 2>&1 &

# ===== MONITOR PROCESS =====
PID=$!
sleep 5

if ps -p $PID > /dev/null; then
    echo "[✓] scan.py started successfully with PID $PID" | tee -a "$LOG"
else
    echo "[❌] scan.py failed to start" | tee -a "$LOG"
fi

# ===== UPLOAD TO S3 (after short delay) =====
sleep 10
if [[ -f "$SCAN_JSON" ]]; then
    echo "[*] Uploading scan_results.json to S3..." | tee -a "$LOG"
    aws s3 cp "$SCAN_JSON" "s3://$S3_BUCKET/latest/scan_results.json" >> "$LOG" 2>&1
    if [[ $? -eq 0 ]]; then
        echo "[UPLOAD SUCCESS] $(date)" | tee -a "$LOG"
    else
        echo "[UPLOAD FAILED] $(date)" | tee -a "$LOG"
    fi
else
    echo "[!] scan_results.json not found, skipping upload" | tee -a "$LOG"
fi

# ===== SAFE ALERT UPLOAD =====
sleep 3
if [[ ! -s "$ALERT_JSON" ]]; then
    echo "[⚠️] alert.json empty or incomplete; skipping upload" | tee -a "$LOG"
else
    echo "[*] Uploading alert.json to S3..." | tee -a "$LOG"
    aws s3 cp "$ALERT_JSON" "s3://$S3_BUCKET/latest/alert.json" >> "$LOG" 2>&1
    if [[ $? -eq 0 ]]; then
        echo "[ALERT UPLOAD SUCCESS] $(date)" | tee -a "$LOG"
    else
        echo "[ALERT UPLOAD FAILED] $(date)" | tee -a "$LOG"
    fi
fi

echo "[*] Scan cycle complete $(date)" | tee -a "$LOG"
exit 0

