# RogueEye – Cloud-Based Rogue Wi-Fi Detector

RogueEye is a cloud-based system that detects **rogue / unauthorized Wi-Fi access points** using a **Raspberry Pi scanner**, **AWS S3**, a **Spring Boot backend**, and an **Angular admin dashboard**.

It was built as a Sheridan SDNE Capstone project.

---

## 1. What’s in this repo?

At a high level:

- **Spring Boot backend** (Java, Maven)
- **Angular frontend** (login, dashboard, scans, alerts, remote admin)
- **Raspberry Pi node scripts** (scanner + MQTT listener)
- Configs to glue everything through **AWS S3** and **MQTT**.

A typical structure looks like:

```text
rogueeye-capstone/
├─ pom.xml                            # Spring Boot project (backend + API)
├─ src/
│  ├─ main/
│  │  ├─ java/ca/...                 # Controllers, services, MQTT publisher, S3 service
│  │  ├─ resources/
│  │  │   └─ application.properties  # Backend config (S3, MQTT, etc.)
│  │  └─ capstone-frontend/          # Angular app
│  │      ├─ src/app/...
│  │      ├─ angular.json
│  │      └─ package.json
├─ raspberry-pi/                      # Pi side (folder name may vary)
│  ├─ scan.py                         # Wi-Fi scanning + S3 upload
│  ├─ runscanner.sh                   # Puts adapter in monitor mode + runs scan loop
│  ├─ mqtt_listener.py               # Listens for MQTT commands from EC2
│  ├─ requirements.txt
│  └─ (optional) allowlist example files
└─ README.md
Important: This repo is the source code, not the built /capstone_bundle deploy folders.
Each teammate will build and deploy their own backend + frontend from this.

2. High-Level Architecture (How it Works)
Raspberry Pi Scanner

External Wi-Fi adapter in monitor mode

scan.py uses Scapy to sniff Wi-Fi beacons (ESSID/BSSID/channel/etc.)

Saves results + alerts into local SQLite and/or JSON files

Uploads latest results to an AWS S3 bucket every few seconds

AWS S3

Acts as a simple “data lake” for:

scan_results.json / alert.json or

the latest SQLite DB file

Backend reads from here, not directly from the Pi

Spring Boot Backend (EC2)

Exposes REST endpoints under /api/scans and /api/remote

/api/scans/results → latest scan data

/api/scans/alerts → latest alerts (sorted, limited to ~100)

/api/remote/start-scan, /stop-scan, /allowlist, /custom → remote commands

Periodically pulls data from S3 via S3Service

Uses WebSockets (/topic/scans, /topic/alerts) to push live updates to the Angular UI

Uses MQTT (via MqttPublisher) to send commands to the Pi on topic like rapd/commands

Angular Frontend (EC2, port 80)

Login / Change Password (JWT-based auth)

Dashboard – Recent Scans & Recent Alerts

Scans Page – full list of recent Wi-Fi networks

Alerts Page – highlighted suspicious networks

Remote Administration Page – buttons to:

Start Scan

Stop Scan

Update Allowlist

Send Custom Command (to Pi via MQTT)

3. Prerequisites
Everyone who wants to run their own copy needs:

Cloud side (EC2)
AWS account

1 × Ubuntu EC2 instance

1 × S3 bucket (e.g. your-rogueeye-bucket)

Security group rules that allow:

80 (HTTP) → from your browser

8080 (optional, if you hit backend directly)

1883 (MQTT) → from your Pi IP

22 (SSH) → from your own IP

Software on EC2:

Java 17+

Node.js + npm

git

Maven wrapper (./mvnw) is already in repo

mosquitto (MQTT broker), e.g.:

bash
Copy code
sudo apt update
sudo apt install -y mosquitto mosquitto-clients
sudo systemctl enable mosquitto
sudo systemctl start mosquitto
Raspberry Pi side
Raspberry Pi (4 or 5 recommended)

External Wi-Fi adapter that supports monitor mode

Python 3 + pip

git

AWS credentials (via ~/.aws/credentials or env vars)

4. Clone the Project
On EC2:

bash
Copy code
cd ~
git clone https://github.com/NomanS295/rogueeye-capstone.git
cd rogueeye-capstone
On Raspberry Pi:

bash
Copy code
cd ~
git clone https://github.com/NomanS295/rogueeye-capstone.git
cd rogueeye-capstone/raspberry-pi   # or wherever the Pi scripts folder is
5. Backend Setup (Spring Boot on EC2)
5.1. Configure application.properties
Open:

bash
Copy code
nano src/main/resources/application.properties
Update values like:

properties
Copy code
# === S3 Settings ===
rapd.s3.bucketName=YOUR_BUCKET_NAME
rapd.s3.region=us-east-1

# === MQTT Settings (to talk to Raspberry Pi) ===
rapd.mqtt.host=YOUR_EC2_PUBLIC_IP     # or internal hostname if VPN
rapd.mqtt.port=1883
rapd.mqtt.username=piagent            # or your own
rapd.mqtt.password=YOUR_STRONG_PASS
rapd.mqtt.topicPrefix=rapd/commands

# (Optional) Flask API base URL if still used
flask.api.baseUrl=http://127.0.0.1:15000
Make sure:

bucketName matches whatever your Pi uploads to.

MQTT host/port/creds match the Pi’s mqtt_listener.py config.

5.2. Build backend
From repo root:

bash
Copy code
./mvnw -q -DskipTests package
If successful, you should see something like:

text
Copy code
target/*.jar
5.3. Run backend
bash
Copy code
nohup java -jar target/*.jar > backend.log 2>&1 &
Verify:

bash
Copy code
ss -tulnp | grep 8080
curl http://localhost:8080/actuator/health
6. Frontend Setup (Angular on EC2)
6.1. Install dependencies
bash
Copy code
cd ~/rogueeye-capstone/src/main/capstone-frontend
npm install
6.2. Configure backend URL
Edit environment files:

bash
Copy code
nano src/environments/environment.ts
Set:

ts
Copy code
export const environment = {
  production: false,
  apiBaseUrl: 'http://YOUR_EC2_PUBLIC_IP:8080'
};
If you use environment.prod.ts, mirror the same value there.

6.3. Build Angular app
bash
Copy code
npm run build -- --configuration production
Build output will be at:

text
Copy code
dist/capstone-frontend/browser/
6.4. Serve frontend (simple http-server)
bash
Copy code
sudo npm install -g http-server
cd dist/capstone-frontend/browser
nohup http-server . -p 80 -a 0.0.0.0 -c-1 > ~/frontend.log 2>&1 &
Test in browser:

text
Copy code
http://YOUR_EC2_PUBLIC_IP
You should see the login page, then be able to reach:

Dashboard

Scans

Alerts

Remote Administration

(As long as the backend is running and configured.)

7. Raspberry Pi Setup
On the Pi:

7.1. Python dependencies
bash
Copy code
cd ~/rogueeye-capstone/raspberry-pi
pip3 install -r requirements.txt
(If requirements.txt doesn’t exist, install at least: scapy, boto3, paho-mqtt, sqlite3 is built-in.)

7.2. AWS credentials
Set up:

bash
Copy code
mkdir -p ~/.aws
nano ~/.aws/credentials
Example:

ini
Copy code
[default]
aws_access_key_id=YOUR_KEY
aws_secret_access_key=YOUR_SECRET
~/.aws/config:

ini
Copy code
[default]
region=us-east-1
7.3. Configure scan.py
Open:

bash
Copy code
nano scan.py
Confirm key constants:

python
Copy code
BUCKET_NAME = "YOUR_BUCKET_NAME"
ADAPTER = "YOUR_MONITOR_INTERFACE"   # e.g., wlxcc641aeb88bf or wlan0mon
SCAN_DURATION = 5
SCAN_INTERVAL = 5
BUCKET_NAME must match rapd.s3.bucketName from application.properties.

7.4. Configure mqtt_listener.py
Open:

bash
Copy code
nano mqtt_listener.py
Set:

python
Copy code
BROKER = "YOUR_EC2_PUBLIC_IP"
PORT = 1883
USERNAME = "piagent"             # must match backend config
PASSWORD = "YOUR_STRONG_PASS"
TOPIC = "rapd/commands"
This script:

Subscribes to rapd/commands

Accepts JSON messages like:

{"action": "start_scan"}

{"action": "stop_scan"}

{"action": "update_allowlist", "args": {"allowlist": ["ESSID/BSSID", ...]}}

{"action": "custom_command", "args": {"command": "your_command"}}

Executes runscanner.sh / kills scan process accordingly.

7.5. Enable monitor mode + scanning
Use your existing script (example):

bash
Copy code
sudo bash runscanner.sh
This usually:

Puts the Wi-Fi interface into monitor mode.

Launches scan.py in a timed loop (scan, save, upload every X seconds).

7.6. Start MQTT listener
bash
Copy code
cd ~/rogueeye-capstone/raspberry-pi
sudo python3 mqtt_listener.py
You should see logs like:

text
Copy code
[🚀] MQTT listener started; waiting for commands...
[✓] Connected to broker ...
[📡] Subscribed to topic: rapd/commands
8. Using the System
Once everything is running:

Go to: http://YOUR_EC2_PUBLIC_IP

Log in with a valid user (admin or test user).

Navigate through:

Dashboard → see recent scans + alerts

Scans → full table of recent Wi-Fi networks

Alerts → hidden SSIDs / suspicious networks

Remote Administration → core control panel

From the Remote Administration page you can:

Start Scan → sends MQTT start_scan to Pi → Pi starts scan.py

Stop Scan → sends stop_scan → Pi kills scan process

Update Allowlist → pushes allowlist array via MQTT

Send Custom Command → sends a raw command field (if you’ve wired it up in mqtt_listener.py)

New scans should appear on Dashboard / Scans / Alerts as the Pi continues to upload.

9. Things Each Teammate MUST Customize
If someone else clones this repo and wants to run it end-to-end, they must:

Create their own:

S3 bucket

EC2 instance

Update on EC2:

src/main/resources/application.properties

rapd.s3.bucketName

rapd.s3.region

rapd.mqtt.*

Update in Angular:

src/main/capstone-frontend/src/environments/*.ts

apiBaseUrl → http://<their-EC2-IP>:8080

Update on Pi:

scan.py → correct BUCKET_NAME + ADAPTER

mqtt_listener.py → correct BROKER, USERNAME, PASSWORD

Once those 3 places are aligned, they can have their own fully working RogueEye deployment.

10. Security Notes
Do NOT commit:

Real AWS keys

PEM files

Production passwords

Use environment variables or secrets manager in a real deployment.

This project is for educational / defensive purposes (detecting rogue APs), not for offensive use.

If something doesn’t work:

Check backend.log on EC2.

Check frontend.log on EC2.

Check scan.py / mqtt_listener.py output on the Pi.

Verify S3 uploads are happening and the bucket name matches on both sides.

yaml
Copy code

---

If you want, next step I can also write a tiny **“Quickstart for teammates”** (like 10 commands only) that you can paste into a separate `DEPLOYMENT.md` so they don’t even have to read the whole thing.






