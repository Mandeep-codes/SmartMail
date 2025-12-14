# Smart Email Cleanup Assistant

A powerful desktop application for analyzing, organizing, and cleaning up your Gmail inbox. Built with Java and JavaFX, this application runs completely locally on your computer while securely connecting to Gmail via OAuth 2.0.

## Features

### 📧 Email Analysis
- **Smart Scanning**: Scan your Gmail inbox and analyze emails efficiently
- **Sender Grouping**: View all emails grouped by sender with detailed statistics
- **Storage Analytics**: See exactly how much storage each sender is using
- **Email Categorization**: Automatically categorize emails (Promotional, Newsletter, Social, Important, etc.)

### 🗑️ Cleanup Operations
- **Bulk Delete**: Delete all emails from specific senders with one click
- **Selective Deletion**: Choose individual emails to delete
- **Safe Deletion**: Emails are moved to trash (not permanently deleted)
- **Unsubscribe Helper**: One-click access to unsubscribe links

### 🔍 Search & Filter
- **Quick Search**: Search by sender name or email address
- **Advanced Filtering**: Filter by category, date, or size
- **Real-time Updates**: See changes reflected immediately

### 📊 Dashboard & Stats
- **Total Email Count**: Track total number of scanned emails
- **Storage Usage**: Monitor total storage used by emails
- **Visual Tables**: Easy-to-read tables with sorting capabilities
- **Progress Tracking**: Real-time progress bars during operations

## Prerequisites

Before running the application, ensure you have:

1. **Java 17 or higher** installed on your system
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
   - Verify installation: `java -version`

2. **Maven 3.6 or higher** (for building from source)
   - Download from: https://maven.apache.org/download.cgi
   - Verify installation: `mvn -version`

3. **Gmail Account** with API access enabled

## Gmail API Setup

This is a **one-time setup** required before using the application:

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click "Create Project" or select an existing project
3. Give your project a name (e.g., "Email Cleanup App")
4. Click "Create"

### Step 2: Enable Gmail API

1. In your project, go to **"APIs & Services" > "Library"**
2. Search for **"Gmail API"**
3. Click on it and press **"Enable"**

### Step 3: Create OAuth 2.0 Credentials

1. Go to **"APIs & Services" > "Credentials"**
2. Click **"Create Credentials" > "OAuth client ID"**
3. If prompted, configure the OAuth consent screen:
   - Choose **"External"** user type
   - Fill in required fields (App name, support email)
   - Add your email as a test user
   - Click "Save and Continue" through all steps
4. Back in "Create OAuth client ID":
   - Choose **"Desktop app"** as application type
   - Give it a name (e.g., "Email Cleanup Desktop")
   - Click **"Create"**
5. Click **"Download JSON"** to download your credentials

### Step 4: Configure Application

1. Rename the downloaded file to **`credentials.json`**
2. Place it in the **root directory** of the application (same folder as `pom.xml`)

The file should look like this:
```json
{
  "installed": {
    "client_id": "YOUR_CLIENT_ID.apps.googleusercontent.com",
    "project_id": "your-project-id",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "client_secret": "YOUR_CLIENT_SECRET",
    "redirect_uris": ["http://localhost"]
  }
}
```

## Installation & Running

### Option 1: Run with Maven (Recommended for Development)

```bash
# Clone or download the project
cd smart-email-cleanup

# Run the application
mvn clean javafx:run
```

### Option 2: Build Executable JAR

```bash
# Build the project
mvn clean package

# Run the JAR file
java -jar target/smart-email-cleanup-1.0.0-jar-with-dependencies.jar
```

### Option 3: Create Platform-Specific Installer (Advanced)

```bash
# Build with JPackage (requires JDK 17+)
jpackage --input target \
  --name "Email Cleanup Assistant" \
  --main-jar smart-email-cleanup-1.0.0-jar-with-dependencies.jar \
  --main-class com.emailcleanup.EmailCleanupApp \
  --type dmg  # Use 'exe' for Windows, 'deb' for Linux
```

## First Time Usage

1. **Launch the application**
2. **Authenticate with Gmail**:
   - Click **"Account" > "Login"**
   - Your browser will open automatically
   - Sign in to your Google account
   - Grant permissions when prompted
   - A success message will appear
3. **Scan your emails**:
   - Click **"File" > "Scan Emails"**
   - Enter number of emails to scan (start with 500 for testing)
   - Wait for the scan to complete
4. **Analyze and cleanup**:
   - Browse senders by storage usage
   - Click on a sender to view all their emails
   - Use "Delete All from Selected Sender" or select individual emails
   - Click "Unsubscribe" to open unsubscribe links

## Project Structure

```
smart-email-cleanup/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── emailcleanup/
│       │           ├── EmailCleanupApp.java          # Main application
│       │           ├── model/
│       │           │   ├── Email.java                # Email data model
│       │           │   └── SenderStats.java          # Sender statistics model
│       │           ├── service/
│       │           │   ├── GmailAuthService.java     # Gmail OAuth authentication
│       │           │   ├── EmailScannerService.java  # Email scanning logic
│       │           │   ├── EmailCategorizerService.java  # Email categorization
│       │           │   ├── EmailActionService.java   # Delete/unsubscribe actions
│       │           │   └── DatabaseService.java      # H2 database operations
│       │           └── ui/
│       │               └── MainWindow.java           # JavaFX UI
│       └── resources/
│           └── logback.xml                           # Logging configuration
├── pom.xml                                           # Maven configuration
├── credentials.json                                  # Gmail API credentials (you create this)
└── README.md                                         # This file

Generated folders:
├── data/                                             # H2 database files
├── logs/                                             # Application logs
├── tokens/                                           # OAuth tokens (auto-generated)
└── target/                                           # Compiled files
```

## Architecture Overview

### Technology Stack

- **Language**: Java 17
- **UI Framework**: JavaFX 19
- **Build Tool**: Maven
- **Database**: H2 (embedded)
- **Gmail Integration**: Google Gmail API
- **Authentication**: OAuth 2.0
- **HTML Parsing**: JSoup
- **Logging**: SLF4J + Logback

### Key Components

1. **GmailAuthService**: Handles OAuth 2.0 authentication flow
2. **EmailScannerService**: Fetches emails from Gmail API
3. **EmailCategorizerService**: Categorizes emails using pattern matching
4. **DatabaseService**: Manages local H2 database for email metadata
5. **EmailActionService**: Performs delete and unsubscribe operations
6. **MainWindow**: JavaFX user interface

### Data Flow

```
Gmail API → EmailScannerService → EmailCategorizerService → DatabaseService → UI
                                                                    ↓
                                                            EmailActionService → Gmail API
```

## Configuration

### Scanning Limits

By default, you can scan any number of emails. However, Gmail API has rate limits:
- 250 quota units per user per second
- 1 billion quota units per day

The application automatically handles rate limiting.

### Database Location

Email metadata is stored in: `./data/emailcleanup.mv.db`

To reset the database, simply delete the `data/` folder.

### Logs

Application logs are stored in: `./logs/email-cleanup.log`

## Troubleshooting

### Issue: "Credentials file not found"

**Solution**: Make sure `credentials.json` is in the root directory of the project.

### Issue: "Authentication failed"

**Solutions**:
1. Check your internet connection
2. Verify Gmail API is enabled in Google Cloud Console
3. Ensure you added your email as a test user
4. Delete the `tokens/` folder and try authenticating again

### Issue: "Port 8888 already in use"

**Solution**: The OAuth callback uses port 8888. Close any application using this port or change it in `GmailAuthService.java` (line with `setPort(8888)`).

### Issue: JavaFX errors on Linux

**Solution**: Install OpenJFX:
```bash
# Ubuntu/Debian
sudo apt-get install openjfx

# Fedora
sudo dnf install java-openjfx
```

### Issue: Application won't start

**Solutions**:
1. Verify Java 17+ is installed: `java -version`
2. Check JavaFX is available: `mvn javafx:run`
3. Review logs in `logs/email-cleanup.log`

## Security & Privacy

- **Local First**: All email metadata is stored locally on your computer
- **Secure Authentication**: Uses OAuth 2.0 (no password storage)
- **Read-Only by Default**: Application doesn't modify emails without your explicit action
- **No Telemetry**: No data is sent to third parties
- **Open Source**: Review the code yourself

## Permissions Required

The application requests these Gmail permissions:

- **gmail.modify**: Required to read emails and move them to trash
  - View your email messages and settings
  - Delete emails

The application **does not**:
- Send emails
- Access your password
- Share your data with anyone

## Performance Tips

1. **Start Small**: Begin with 500-1000 emails to test
2. **Batch Processing**: The app processes in batches of 50 for efficiency
3. **Database Cleanup**: Periodically delete the `data/` folder to start fresh
4. **Selective Scanning**: Focus on specific date ranges if needed

## Known Limitations

- **Gmail Only**: Currently supports Gmail only (not Outlook, Yahoo, etc.)
- **Desktop Only**: No mobile version available
- **Single Account**: One Gmail account at a time
- **API Limits**: Subject to Gmail API rate limits

## Future Enhancements

Potential features for future versions:

- Multi-account support
- Email templates and auto-replies
- Advanced filtering and rules
- Export reports to PDF/CSV
- Schedule automatic cleanups
- Email attachment manager
- Support for other email providers

## Contributing

This is a local-first application. To contribute:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is open source and available for personal and commercial use.

## Support

For issues, questions, or suggestions:

1. Check the **Troubleshooting** section above
2. Review application logs in `logs/`
3. Verify Gmail API setup is correct
4. Check internet connectivity

## Version History

### Version 1.0.0 (Current)
- Initial release
- Gmail integration with OAuth 2.0
- Email scanning and analysis
- Sender grouping and statistics
- Bulk delete functionality
- Unsubscribe link detection
- Email categorization
- Local H2 database storage
- JavaFX desktop UI

---

**Built with ❤️ for productivity and privacy**

Enjoy your clean inbox! 📧✨
