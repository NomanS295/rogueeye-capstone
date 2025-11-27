 

# RogueEye – Cloud-Based Rogue Wi-Fi Detector

RogueEye is a cloud-based system that detects **rogue / unauthorized Wi-Fi access points** using a **Raspberry Pi scanner**, **AWS S3**, a **Spring Boot backend**, and an **Angular admin dashboard**.

It was built as a Sheridan SDNE Capstone project.

---

## 1. What’s in this repo?

At a high level:

- **Spring Boot backend** (Java, Maven)
- **Angular frontend** (login, dashboard, scans, alerts, remote admin)
- **Raspberry Pi node scripts** (scanner + MQTT listener)
- Configs to glue everything through **AWS S3** and **MQTT**

Project structure:

```
rogueeye-capstone/
├─ pom.xml
├─ src/
│  ├─ main/java/ca/...           # Backend logic
│  ├─ resources/application.properties
│  └─ capstone-frontend/         # Angular UI
├─ raspberry-pi/
│  ├─ scan.py
│  ├─ runscanner.sh
│  ├─ mqtt_listener.py
│  └─ requirements.txt
└─ README.md
```

> ⚠️ Note: This repo holds the **source code**, not the built `/capstone_bundle` deploy folders.

---

## 2. High-Level Architecture

### 🔹 Raspberry Pi Scanner
- Uses external Wi-Fi adapter in monitor mode
- `scan.py` sniffs beacon frames (ESSID/BSSID/channel)
- Stores data in JSON/SQLite
- Uploads to S3 every few seconds

### 🔹 AWS S3
- Used as a simple **data lake**
  - `scan_results.json`
  - `alert.json`

### 🔹 Spring Boot Backend (EC2)
- REST API under `/api/scans` and `/api/remote`
- Pulls from S3 using `S3Service`
- Sends MQTT messages to Pi via `MqttPublisher`
- Uses WebSockets (`/topic/scans`) to push live updates to Angular UI

### 🔹 Angular Frontend (EC2)
- JWT-based Login
- Dashboard: Recent scans and alerts
- Remote Admin: Start/Stop scan, update allowlist, send custom command

---

## 3. Prerequisites

### ☁️ EC2 Instance
- Ubuntu instance with:
  - Java 17+
  - Node.js + npm
  - git
  - MQTT Broker (`mosquitto`)
  - Open ports: 22, 80, 8080, 1883

### 🍓 Raspberry Pi
- Pi 4/5 with:
  - External Wi-Fi adapter (monitor mode)
  - Python 3
  - pip
  - `~/.aws/credentials` configured

---

## 4. Clone the Project

### On EC2:
```bash
cd ~
git clone https://github.com/NomanS295/rogueeye-capstone.git
cd rogueeye-capstone
```

### On Raspberry Pi:
```bash
cd ~
git clone https://github.com/NomanS295/rogueeye-capstone.git
cd rogueeye-capstone/raspberry-pi
```

---

## 5. Backend Setup (Spring Boot)

### 5.1 Configure `application.properties`
```bash
nano src/main/resources/application.properties
```

Edit values:
```properties
rapd.s3.bucketName=YOUR_BUCKET_NAME
rapd.s3.region=us-east-1

rapd.mqtt.host=YOUR_EC2_PUBLIC_IP
rapd.mqtt.port=1883
rapd.mqtt.username=piagent
rapd.mqtt.password=YOUR_STRONG_PASS
rapd.mqtt.topicPrefix=rapd/commands

flask.api.baseUrl=http://127.0.0.1:15000
```

---

### 5.2 Build backend
```bash
./mvnw -q -DskipTests package
```

You should get:
```
target/*.jar
```

---

### 5.3 Run backend
```bash
nohup java -jar target/*.jar > backend.log 2>&1 &
```

Verify:
```bash
ss -tulnp | grep 8080
curl http://localhost:8080/actuator/health
```

---

## 6. Frontend Setup (Angular)

### 6.1 Install dependencies
```bash
cd ~/rogueeye-capstone/src/main/capstone-frontend
npm install
```

### 6.2 Configure backend URL
```bash
nano src/environments/environment.ts
```

Set:
```ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://YOUR_EC2_PUBLIC_IP:8080'
};
```

### 6.3 Build Angular app
```bash
npm run build -- --configuration production
```

Output:
```
dist/capstone-frontend/browser/
```

### 6.4 Serve frontend
```bash
sudo npm install -g http-server
cd dist/capstone-frontend/browser
nohup http-server . -p 80 -a 0.0.0.0 -c-1 > ~/frontend.log 2>&1 &
```

Test in browser:  
`http://YOUR_EC2_PUBLIC_IP`

---

## 7. Raspberry Pi Setup

### 7.1 Install dependencies
```bash
cd ~/rogueeye-capstone/raspberry-pi
pip3 install -r requirements.txt
```

### 7.2 AWS credentials
```bash
mkdir -p ~/.aws
nano ~/.aws/credentials
```

```ini
[default]
aws_access_key_id=YOUR_KEY
aws_secret_access_key=YOUR_SECRET
```

```ini
# ~/.aws/config
[default]
region=us-east-1
```

---

### 7.3 Configure `scan.py`
```python
BUCKET_NAME = "YOUR_BUCKET_NAME"
ADAPTER = "wlxcc641aeb88bf"
SCAN_DURATION = 5
SCAN_INTERVAL = 5
```

---

### 7.4 Configure `mqtt_listener.py`
```python
BROKER = "YOUR_EC2_PUBLIC_IP"
PORT = 1883
USERNAME = "piagent"
PASSWORD = "YOUR_STRONG_PASS"
TOPIC = "rapd/commands"
```

Accepts messages like:
```json
{"action": "start_scan"}
{"action": "stop_scan"}
{"action": "update_allowlist", "args": {"allowlist": ["BSSID/ESSID"]}}
{"action": "custom_command", "args": {"command": "ls"}}
```

---

### 7.5 Start Scanner
```bash
sudo bash runscanner.sh
```

---

### 7.6 Start MQTT Listener
```bash
cd ~/rogueeye-capstone/raspberry-pi
sudo python3 mqtt_listener.py
```

Output:
```
[✓] Connected to broker
[📡] Subscribed to topic: rapd/commands
```

---

## 8. Using the System

Visit in browser:  
`http://YOUR_EC2_PUBLIC_IP`

Login and navigate:
- Dashboard
- Scans
- Alerts
- Remote Admin

You can:
- ✅ Start / Stop Scan
- ✅ Update Allowlist
- ✅ Send Custom Commands

Scans will begin appearing automatically if Pi is uploading to S3.

---

## 9. Teammate Customization Checklist

Each teammate must configure:

### EC2:
- `application.properties`
  - `rapd.s3.bucketName`, `rapd.mqtt.*`

### Angular:
- `environment.ts`
  - `apiBaseUrl`

### Raspberry Pi:
- `scan.py` → `BUCKET_NAME`, `ADAPTER`
- `mqtt_listener.py` → `BROKER`, `USERNAME`, `PASSWORD`

---

## 10. Security Notes

- ❌ Do NOT commit:
  - AWS keys
  - PEM files
  - Passwords

Use `.env`, Secrets Manager, or IAM roles instead.  
This project is intended for **defensive use only**.

---

## ✅ Troubleshooting

- Check logs on EC2:
  - `backend.log`, `frontend.log`
- Watch terminal output on Pi
- Make sure:
  - Bucket names match
  - MQTT credentials match
  - Backend is running
