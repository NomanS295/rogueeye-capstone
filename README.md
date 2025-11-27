# RogueEye – Cloud-Based Rogue Wi-Fi Detector
RogueEye is a cloud-based system that detects rogue / unauthorized Wi-Fi access points using a Raspberry Pi scanner, AWS S3, a Spring Boot backend, and an Angular admin dashboard. It was built as a Sheridan SDNE Capstone project.

## What’s in this repo?
This repository contains:
- Spring Boot backend (Java/Maven)
- Angular frontend (login, dashboard, scans, alerts, remote admin)
- Raspberry Pi scanning + MQTT scripts
- Configuration for AWS S3 + MQTT communication

```
Project structure:
rogueeye-capstone/
├─ pom.xml
├─ src/main/java/... (backend logic)
├─ src/main/resources/application.properties
├─ src/main/capstone-frontend/ (Angular UI)
├─ raspberry-pi/
│  ├─ scan.py
│  ├─ mqtt_listener.py
│  ├─ runscanner.sh
│  └─ requirements.txt
└─ README.md
```


This repo contains source code only, not the deployed `/capstone_bundle` folders.

## High-Level Architecture
Raspberry Pi Scanner:
- External Wi-Fi adapter in monitor mode
- scan.py sniffs ESSID/BSSID/channel
- Stores locally + generates scan_results.json and alert.json
- Uploads JSON files to AWS S3 every few seconds

AWS S3:
- Acts as the data source for the backend
- Holds latest scan_results.json and alert.json

Spring Boot Backend (EC2):
- Pulls JSON data from S3
- Exposes REST endpoints under /api/scans and /api/remote
- Publishes MQTT commands to Pi
- Streams updates to Angular using WebSockets (/topic/scans, /topic/alerts)

Angular Frontend (EC2):
- JWT login + change password
- Dashboard with live scan + alert updates
- Scans page (detailed list)
- Alerts page (suspicious networks)
- Remote Admin (start/stop scan, update allowlist, custom command)

## Prerequisites
EC2:
- Ubuntu
- Java 17+
- Node.js + npm
- git
- mosquitto MQTT broker
- Open ports: 22, 80, 8080, 1883

Raspberry Pi:
- Pi 4/5
- External Wi-Fi adapter supporting monitor mode
- Python 3 + pip
- AWS credentials configured

## Clone the Project
EC2:
```
cd ~
git clone https://github.com/NomanS295/rogueeye-capstone.git
cd rogueeye-capstone
```

Pi:
```
cd ~
git clone https://github.com/NomanS295/rogueeye-capstone.git
cd rogueeye-capstone/raspberry-pi
```

## Backend Setup (EC2)
Configure application.properties:
```
nano src/main/resources/application.properties
```

Set:
```
rapd.s3.bucketName=YOUR_BUCKET_NAME
rapd.s3.region=us-east-1
rapd.mqtt.host=YOUR_EC2_PUBLIC_IP
rapd.mqtt.port=1883
rapd.mqtt.username=piagent
rapd.mqtt.password=YOUR_STRONG_PASS
rapd.mqtt.topicPrefix=rapd/commands
flask.api.baseUrl=http://127.0.0.1:15000
```

Build backend:
```
./mvnw -q -DskipTests package
```

Run backend:
```
nohup java -jar target/*.jar > backend.log 2>&1 &
```

Verify:
```
curl http://localhost:8080/actuator/health
```

## Frontend Setup (EC2)
Install dependencies:
```
cd src/main/capstone-frontend
npm install
```

Configure backend URL:
```
nano src/environments/environment.ts
```

Set:
```
apiBaseUrl: 'http://YOUR_EC2_PUBLIC_IP:8080'
```

Build frontend:
```
npm run build -- --configuration production
```

Serve frontend:
```
sudo npm install -g http-server
cd dist/capstone-frontend/browser
nohup http-server . -p 80 -a 0.0.0.0 -c-1 > ~/frontend.log 2>&1 &
```

Access UI:
http://YOUR_EC2_PUBLIC_IP

## Raspberry Pi Setup
Install dependencies:
```
cd ~/rogueeye-capstone/raspberry-pi
pip3 install -r requirements.txt
```

Configure AWS credentials:
```
mkdir -p ~/.aws
nano ~/.aws/credentials
```

Add:
```
[default]
aws_access_key_id=YOUR_KEY
aws_secret_access_key=YOUR_SECRET
```

Configure region:
```
nano ~/.aws/config
```

```
[default]
region=us-east-1
```

Configure scan.py:
```
nano scan.py
```

Set:
```
BUCKET_NAME = "YOUR_BUCKET_NAME"
ADAPTER = "YOUR_ADAPTER"
SCAN_DURATION = 5
SCAN_INTERVAL = 5
```

Configure mqtt_listener.py:
```
nano mqtt_listener.py
```

Set:
```
BROKER = "YOUR_EC2_PUBLIC_IP"
PORT = 1883
USERNAME = "piagent"
PASSWORD = "YOUR_STRONG_PASS"
TOPIC = "rapd/commands"
```

Start scanner:
```
sudo bash runscanner.sh
```

Start MQTT listener:
```
sudo python3 mqtt_listener.py
```

Expected:
Connected to broker
Subscribed to topic rapd/commands

## Using the System
Go to:
http://YOUR_EC2_PUBLIC_IP

Login → Dashboard

You can:
- Start/Stop Scan
- Update Allowlist
- Send Custom Commands

Data will appear automatically if Pi uploads to S3 and backend is running.

## Teammate Setup Checklist
EC2:
- application.properties (S3 + MQTT settings)
- environment.ts (apiBaseUrl)
- Build backend
- Build and serve frontend

Pi:
- AWS credentials
- scan.py (BUCKET_NAME + ADAPTER)
- mqtt_listener.py (BROKER + credentials)

Once configured, the system will work end-to-end.

## Security Notes
Do NOT commit:
- AWS keys
- PEM files
- Passwords

This system is intended for defensive use only.

## Troubleshooting
Check:
backend.log
frontend.log
Pi terminal output

Confirm:
- Correct bucket name
- Correct MQTT settings
- Correct API URLs
- Ports open on EC2

If all match, RogueEye will display live scan data and remote commands will reach the Pi successfully.

