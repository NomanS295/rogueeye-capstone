#!/usr/bin/env python3
import os
import json
import sqlite3
import time
import signal
from datetime import datetime
from pathlib import Path
from scapy.all import sniff, Dot11, Dot11Beacon, Dot11Elt
import boto3
import botocore.exceptions

# === Paths ===
BASE_DIR = Path("/home/shahid/Capstone")
DB_PATH = BASE_DIR / "db" / "wifi_scanner.db"
SCAN_JSON = BASE_DIR / "scanner" / "scan_results.json"
ALERT_JSON = BASE_DIR / "scanner" / "alert.json"

# === Wi-Fi Adapter & Timing ===
ADAPTER = "wlxcc641aeb88bf"
SCAN_DURATION = 5
SCAN_INTERVAL = 5

# === AWS S3 Config ===
BUCKET_NAME = "capstone-2025-wifi-scanner-data-noman"
S3_PREFIX = "latest/"
os.environ["AWS_SHARED_CREDENTIALS_FILE"] = "/home/shahid/.aws/credentials"
os.environ["AWS_CONFIG_FILE"] = "/home/shahid/.aws/config"

s3 = boto3.client("s3", region_name="us-east-1")

# === Globals ===
ap_data = {}
alerts = []
_running = True


# === Graceful shutdown ===
def _signal_handler(signum, frame):
    global _running
    print(f"[{datetime.now().strftime('%H:%M:%S')}] ⚠️ Received signal {signum}, shutting down scanner...")
    _running = False


signal.signal(signal.SIGINT, _signal_handler)
signal.signal(signal.SIGTERM, _signal_handler)


# === Ensure DB exists ===
def ensure_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS scan_results (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        essid TEXT, bssid TEXT, channel TEXT, avg_power TEXT,
        auth TEXT, enc TEXT, scanned_at TEXT, whitelist_id TEXT)''')
    c.execute('''CREATE TABLE IF NOT EXISTS alerts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        essid TEXT, bssid TEXT, channel TEXT, avg_power TEXT,
        auth TEXT, enc TEXT, alert_type TEXT, detected_at TEXT, whitelist_id TEXT)''')
    conn.commit()
    conn.close()


# === Insert scan data ===
def insert_scan(essid, bssid, channel, avg_power, auth, enc):
    try:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute(
            "INSERT INTO scan_results (essid,bssid,channel,avg_power,auth,enc,scanned_at,whitelist_id) VALUES (?,?,?,?,?,?,?,?)",
            (essid, bssid, channel, avg_power, auth, enc, datetime.now().isoformat(), None)
        )
        conn.commit()
    except Exception as e:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] [DB ERROR] {e}")
    finally:
        conn.close()


# === Packet handler ===
def packet_handler(pkt):
    try:
        if pkt.haslayer(Dot11Beacon):
            bssid = pkt[Dot11].addr2
            essid = "<hidden>"

            if pkt.haslayer(Dot11Elt):
                elt = pkt.getlayer(Dot11Elt, 0)
                if elt and hasattr(elt, "info"):
                    try:
                        essid = elt.info.decode(errors="ignore") or "<hidden>"
                    except Exception:
                        pass

            ch_elt = pkt.getlayer(Dot11Elt, ID=3)
            channel = None
            if ch_elt and getattr(ch_elt, "info", None):
                try:
                    channel = int(ch_elt.info[0])
                except Exception:
                    channel = -1

            ap_data[bssid] = {
                "essid": essid,
                "bssid": bssid,
                "channel": channel,
                "auth": "WPA2",
                "enc": "CCMP",
                "avg_power": -40,
                "scanned_at": datetime.now().isoformat(),
            }

            insert_scan(essid, bssid, channel, -40, "WPA2", "CCMP")

            # === Simple Rogue Alert Logic ===
            if essid == "<hidden>":
                alerts.append({
                    "essid": essid,
                    "bssid": bssid,
                    "channel": channel,
                    "avg_power": -40,
                    "auth": "WPA2",
                    "enc": "CCMP",
                    "alert_type": "Hidden SSID Detected",
                    "detected_at": datetime.now().isoformat(),
                    "whitelist_id": None
                })
    except Exception as e:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] [PACKET ERROR] {e}")


# === Save & Upload ===
def save_and_upload():
    try:
        # ✅ Proper JSON arrays for both scan results and alerts
        SCAN_JSON.write_text(json.dumps(list(ap_data.values()), indent=4))
        ALERT_JSON.write_text(json.dumps(alerts, indent=4))

        # ✅ Upload to S3
        s3.upload_file(str(SCAN_JSON), BUCKET_NAME, S3_PREFIX + "scan_results.json")
        s3.upload_file(str(ALERT_JSON), BUCKET_NAME, S3_PREFIX + "alert.json")

        print(f"[{datetime.now().strftime('%H:%M:%S')}] ✅ Uploaded scan_results.json ({len(ap_data)} networks)")
        print(f"[{datetime.now().strftime('%H:%M:%S')}] ✅ Uploaded alert.json ({len(alerts)} alerts)")
    except botocore.exceptions.NoCredentialsError:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] ❌ AWS credentials not found.")
    except Exception as e:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] ❌ Upload failed: {e}")


# === Main loop ===
def main():
    ensure_db()

    # 🧹 Clear old scan data for a fresh start
    global ap_data, alerts
    ap_data = {}
    alerts = []

    # Remove old JSON files (to avoid showing stale results)
    for f in [SCAN_JSON, ALERT_JSON]:
        if f.exists():
            f.unlink()

    # Clear previous DB entries
    try:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute("DELETE FROM scan_results")
        c.execute("DELETE FROM alerts")
        conn.commit()
        conn.close()
        print(f"[{datetime.now().strftime('%H:%M:%S')}] 🧼 Old scans cleared from database.")
    except Exception as e:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] ⚠️ Failed to clear old scans: {e}")

    print(f"[*] Starting timed scanning on {ADAPTER} every {SCAN_INTERVAL}s...")

    while _running:
        try:
            print(f"[{datetime.now().strftime('%H:%M:%S')}] 🔍 Scanning for nearby networks...")
            sniff(iface=ADAPTER, prn=packet_handler, timeout=SCAN_DURATION)
            save_and_upload()
            print(f"[{datetime.now().strftime('%H:%M:%S')}] ⏳ Waiting {SCAN_INTERVAL}s before next scan...\n")
            time.sleep(SCAN_INTERVAL)
        except Exception as e:
            print(f"[{datetime.now().strftime('%H:%M:%S')}] [MAIN LOOP ERROR] {e}")
            time.sleep(2)

    print(f"[{datetime.now().strftime('%H:%M:%S')}] 🛑 Scanner stopped gracefully.")


if __name__ == "__main__":
    main()

