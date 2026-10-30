# BankEase Deployment Guide 🚀

This document provides step-by-step instructions for deploying **BankEase: NextGen Digital Banking Suite** across different production environments: **Docker Compose**, **Cloud Platforms (Render, Railway)**, and **Linux VPS (AWS / DigitalOcean / Ubuntu)**.

---

## 📋 Pre-Deployment Checklist

Before deploying to any production environment:

1. **Generate a 256-bit JWT Secret Key:**
   - **Linux / macOS:**
     ```bash
     openssl rand -base64 48
     ```
   - **Windows PowerShell:**
     ```powershell
     [Convert]::ToBase64String((1..48 | % { Get-Random -Maximum 256 }))
     ```
2. **Review Environment Settings:**
   - `JWT_SECRET`: Must be set to your generated 32+ character key.
   - `SEED_ENABLED`: Set to `false` in live production to disable demo test accounts.
   - `OTP_DEMO_MODE`: Set to `false` so OTPs are sent through SMS/Email gateways instead of returned in API responses.
   - `DB_PASSWORD`: Set a strong, unique MySQL password.
   - `CORS_ALLOWED_ORIGINS`: Set to your production domains (e.g. `https://yourbankease.com`).

---

## 🐳 Option 1: Docker Compose (Recommended for Containerized Servers)

The easiest and most reliable way to run BankEase with its MySQL database in an isolated container environment.

### 1. Configure Environment File
Copy the provided template:
```bash
cp .env.example .env
```
Edit `.env` and set your passwords:
```ini
JWT_SECRET=YourGenerated32CharacterKeyHere!
DB_PASSWORD=YourStrongDatabasePassword123!
SEED_ENABLED=true
OTP_DEMO_MODE=false
```

### 2. Build and Start Containers
```bash
docker compose up -d --build
```

### 3. Check Service Status & Health
```bash
docker compose ps
docker compose logs -f app
```

Once healthy, BankEase is running at:
👉 **`http://localhost:8080/`** (or your server's public IP).

### Stopping the Services
```bash
docker compose down
```
*(Database data persists securely in the Docker volume `bankease_mysql_data`).*

---

## ☁️ Option 2: Cloud Deployment (Render.com)

BankEase includes a ready-to-use `render.yaml` blueprint.

1. Push your repository to **GitHub**.
2. Log in to [Render Dashboard](https://dashboard.render.com/).
3. Click **Blueprints** $\rightarrow$ **New Blueprint Instance**.
4. Connect your `Anilsoni27/BankEase` repository.
5. Render automatically provisions:
   - A managed **MySQL Database**.
   - A Docker **Web Service** building from `Dockerfile`.
   - Automatic environment variables wiring (`DB_URL`, `JWT_SECRET`, etc.).
6. Click **Apply**. Once deployment completes, your live banking portal is accessible on your Render URL!

---

## 🚂 Option 3: Cloud Deployment (Railway.app)

1. Push your repository to **GitHub**.
2. Go to [Railway.app](https://railway.app/) and create a **New Project**.
3. Add a **MySQL** database service to the project.
4. Add a service from your **GitHub Repo (`BankEase`)**.
5. In the Web Service settings, set the following environment variables:
   - `DB_URL`: `${{MySQL.MYSQL_URL}}`
   - `DB_PASSWORD`: `${{MySQL.MYSQLPASSWORD}}`
   - `JWT_SECRET`: *(Your 32+ character key)*
   - `PORT`: `8080`
   - `SEED_ENABLED`: `true`
6. Railway automatically detects `Dockerfile` and `railway.json`, builds the application, and deploys it.

---

## 🖥️ Option 4: Linux VPS Deployment (Ubuntu / AWS EC2 / DigitalOcean)

### Step 1: Install Dependencies
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-25-jdk mysql-server nginx
```

### Step 2: Configure MySQL Database
```sql
sudo mysql -u root
```
```sql
CREATE DATABASE bankease_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'bankease_user'@'localhost' IDENTIFIED BY 'StrongPassword123!';
GRANT ALL PRIVILEGES ON bankease_db.* TO 'bankease_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Step 3: Build Standalone Application JAR
On your build machine or server:
```bash
# Build frontend
cd frontend
npm ci
npm run build
cd ..

# Copy frontend to static resources & build JAR
cp -r frontend/dist/frontend/browser/* src/main/resources/static/
./mvnw clean package -DskipTests
```
The production artifact is generated at: `target/bankease-backend-0.0.1-SNAPSHOT.jar`.

### Step 4: Setup Linux `systemd` Service
Create a dedicated service configuration:
```bash
sudo nano /etc/systemd/system/bankease.service
```

Paste the following configuration:
```ini
[Unit]
Description=BankEase Digital Banking Suite
After=syslog.target network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=/opt/bankease
ExecStart=/usr/bin/java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /opt/bankease/bankease-backend-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143

# Environment Variables
Environment=PORT=8080
Environment=DB_URL=jdbc:mysql://localhost:3306/bankease_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
Environment=DB_USERNAME=bankease_user
Environment=DB_PASSWORD=StrongPassword123!
Environment=JWT_SECRET=YourGenerated32CharacterKeyHere!
Environment=SEED_ENABLED=true
Environment=OTP_DEMO_MODE=false

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable bankease
sudo systemctl start bankease
sudo systemctl status bankease
```

### Step 5: Configure Nginx as Reverse Proxy & SSL
```bash
sudo nano /etc/nginx/sites-available/bankease
```
Configuration:
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
Enable the site and obtain a free Let's Encrypt SSL certificate:
```bash
sudo ln -s /etc/nginx/sites-available/bankease /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
sudo certbot --nginx -d yourdomain.com
```

---

## 🔍 Verification & Health Monitoring

To verify your deployment at any time:

1. **Health Check Probe:**
   ```bash
   curl http://localhost:8080/api/health
   ```
   **Response:**
   ```json
   {
     "status": "UP",
     "service": "BankEase NextGen Digital Banking Suite",
     "timestamp": "2026-10-30T12:00:00Z"
   }
   ```

2. **Frontend UI:**
   Open `http://localhost:8080/` in a browser. The single-page Angular interface should render seamlessly with instant tab navigation.

