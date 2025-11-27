import sqlite3
import boto3
from flask import Flask, jsonify
from flask_cors import CORS
from datetime import datetime
import json
import tempfile
import os
import paho.mqtt.publish as publish

# --- Flask setup ---
app = Flask(__name__)
CORS(app)

# --- Database path ---
DB_PATH = "/home/shahid/Capstone/db/wifi_scanner.db"

# --- AWS S3 configuration ---
S3_BUCKET_NAME = "capstone-2025-wifi-scanner-data-noman"
S3_REGION = "us-east-2"
s3 = boto3.client("s3", region_name=S3_REGION)

# --- MQTT configuration (to EC2) ---
MQTT_BROKER = "3.20.218.205"
MQTT_PORT = 1883
MQTT_USER = "piagent"
MQTT_PASS = "shahid295"
MQTT_TOPIC = "rapd/commands"

# --- Helper to query SQLite ---
def query_db(query, args=()):
    if not os.path.exists(DB_PATH):
        print(f"[ERROR] Database file not found: {DB_PATH}")
        return []
    try:
        conn = sqlite3.connect(DB_PATH)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()
        cur.execute(query, args)
        rows = cur.fetchall()
        conn.close()
        print(f"[INFO] Query returned {len(rows)} rows.")
        return [dict(row) for row in rows]
    except sqlite3.Error as e:
        print(f"[DB ERROR] {e}")
        return []

# --- Helper to upload JSON results to S3 ---
def upload_to_s3(data, key):
    try:
        tmpfile = tempfile.NamedTemporaryFile(delete=False)
        with open(tmpfile.name, "w") as f:
            json.dump(data, f, indent=4)
        s3.upload_file(tmpfile.name, S3_BUCKET_NAME, key, ExtraArgs={"ContentType": "application/json"})
        print(f"[✓] Uploaded {key} to S3 bucket {S3_BUCKET_NAME}")
        os.unlink(tmpfile.name)
    except Exception as e:
        print(f"[S3 ERROR] {e}")

# --- Flask routes ---
@app.route("/scans", methods=["GET"])
def get_scans():
    print("[INFO] /scans endpoint hit")
    results = query_db("SELECT * FROM scan_results ORDER BY scanned_at DESC LIMIT 100")
    if results:
        upload_to_s3(results, "latest/scan_results.json")
    return jsonify(results)

@app.route("/alerts", methods=["GET"])
def get_alerts():
    print("[INFO] /alerts endpoint hit")
    results = query_db("SELECT * FROM alerts ORDER BY detected_at DESC LIMIT 50")
    if results:
        upload_to_s3(results, "latest/alert.json")
    return jsonify(results)

@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({"status": "ok", "message": "Flask API running"}), 200

# --- 🔁 NEW: Start scan route (MQTT + fallback local) ---
@app.route("/start_scan", methods=["GET"])
def start_scan():
    try:
        payload = '{"action":"start_scan"}'
        publish.single(
            MQTT_TOPIC,
            payload,
            hostname=MQTT_BROKER,
            port=MQTT_PORT,
            auth={'username': MQTT_USER, 'password': MQTT_PASS}
        )
        print("[📡] MQTT publish: start_scan sent to EC2 broker.")
        return jsonify({"status": "Scan triggered via MQTT"}), 200
    except Exception as e:
        print(f"[⚠️ MQTT ERROR] {e} — running scan locally.")
        os.system("sudo python3 /home/shahid/Capstone/scanner/scan.py &")
        return jsonify({"status": "Scan triggered locally (MQTT failed)"}), 200

# --- Run server ---
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

