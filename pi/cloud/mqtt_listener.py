#!/usr/bin/env python3
import paho.mqtt.client as mqtt
import json
import os

# --- MQTT Configuration ---
BROKER = "3.20.218.205"
PORT = 1883
TOPIC = "rapd/commands"
USERNAME = "piagent"
PASSWORD = "shahid295"

# --- MQTT Handlers ---
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print(f"[✓] Connected to broker {BROKER}:{PORT}")
        client.subscribe(TOPIC)
        print(f"[📡] Subscribed to topic: {TOPIC}")
    else:
        print(f"[❌] Connection failed with code {rc}")


def on_message(client, userdata, msg):
    try:
        payload = msg.payload.decode().strip()
        print(f"[📩] Message received on {msg.topic}: {payload}")

        # Handle both plain and JSON-encoded messages
        if payload.startswith("{"):
            data = json.loads(payload)
            action = data.get("action", "").strip()
        else:
            action = payload
            data = {}

        # Execute based on received action
        if action == "start_scan":
            print("[⚙️] Starting Wi-Fi scan as root ...")
            os.system("sudo bash -c '/home/shahid/Capstone/scanner/run_scanner.sh &'")

        elif action == "stop_scan":
            print("[🛑] Stopping scan process ...")
            os.system("sudo pkill -f scan.py")

        elif action == "update_allowlist":
            allowlist = data.get("args", {}).get("allowlist", [])
            print(f"[✅] Updating allowlist: {allowlist}")
            os.makedirs("/home/shahid/Capstone/scanner", exist_ok=True)
            with open("/home/shahid/Capstone/scanner/allowlist.json", "w") as f:
                json.dump(allowlist, f, indent=4)
            print("[💾] Allowlist saved successfully.")

        elif action == "custom_command":
            command = data.get("args", {}).get("command", "")
            if command:
                print(f"[⚙️] Executing custom command: {command}")
                os.system(command)
            else:
                print("[ℹ️] No command provided in custom_command payload.")

        else:
            print(f"[ℹ️] Unknown action received: {action}")

    except json.JSONDecodeError:
        print(f"[❌] Error: Received malformed JSON payload — could not parse '{payload}'")

    except Exception as e:
        print(f"[❌] Error processing message: {e}")


# --- Main Execution ---
if __name__ == "__main__":
    print("[🚀] MQTT listener started; waiting for commands...")

    client = mqtt.Client()
    client.username_pw_set(USERNAME, PASSWORD)
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(BROKER, PORT, 60)
    client.loop_forever()

