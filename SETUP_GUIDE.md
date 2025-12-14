# Quick Setup Guide

Follow these steps to get the Smart Email Cleanup Assistant running on your local machine.

## 1. Install Prerequisites

### Java 17 or Higher

**Windows:**
1. Download from https://adoptium.net/
2. Run installer and follow instructions
3. Verify: Open Command Prompt and run `java -version`

**macOS:**
```bash
brew install openjdk@17
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### Maven 3.6 or Higher

**Windows:**
1. Download from https://maven.apache.org/download.cgi
2. Extract to C:\Program Files\Apache\maven
3. Add to PATH environment variable
4. Verify: `mvn -version`

**macOS:**
```bash
brew install maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install maven
```

## 2. Gmail API Setup (5 minutes)

### Create Google Cloud Project

1. Visit https://console.cloud.google.com/
2. Click "New Project"
3. Name it "Email Cleanup App"
4. Click "Create"

### Enable Gmail API

1. In your project, go to "APIs & Services" > "Library"
2. Search for "Gmail API"
3. Click it and press "Enable"

### Create OAuth Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth client ID"
3. Configure OAuth consent screen:
   - User Type: External
   - App name: Email Cleanup Assistant
   - Support email: your-email@gmail.com
   - Add your email as test user
   - Save and Continue through all steps
4. Create OAuth Client ID:
   - Application type: **Desktop app**
   - Name: Email Cleanup Desktop
   - Click "Create"
5. **Download JSON** file
6. Rename to `credentials.json`
7. Place in project root folder

## 3. Run the Application

### Using Startup Scripts (Easiest)

**Windows:**
```cmd
run.bat
```

**macOS/Linux:**
```bash
chmod +x run.sh
./run.sh
```

### Using Maven Directly

```bash
mvn clean javafx:run
```

### Build Executable JAR

```bash
mvn clean package
java -jar target/smart-email-cleanup-1.0.0-jar-with-dependencies.jar
```

## 4. First Use

1. Application opens
2. Click "Account" > "Login"
3. Browser opens for Google sign-in
4. Grant permissions
5. Click "File" > "Scan Emails"
6. Enter 500 (start small)
7. Wait for scan to complete
8. Browse and cleanup!

## Troubleshooting

### "Credentials file not found"
- Ensure `credentials.json` is in the same folder as `pom.xml`

### "Authentication failed"
- Check internet connection
- Verify Gmail API is enabled
- Add your email as test user in OAuth consent screen

### Port 8888 error
- Close any app using port 8888
- Or change port in `GmailAuthService.java`

### JavaFX not found (Linux)
```bash
sudo apt install openjfx
```

## Need Help?

Check the full README.md for detailed documentation and troubleshooting.

---

Enjoy your clean inbox! 📧
