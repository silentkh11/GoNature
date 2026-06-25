# GoNature – National Park Visitor Management System
**Group 7 | Course 61756 | Assignment 3**

## Project Overview
GoNature is a client-server application for managing national park visits.  
It handles online booking, walk-in entry, waitlists, automated reminders, and management reports across 8 Israeli national parks.

**Stack:** Java 26 · JavaFX 26 · MySQL 8 · OCSF · HikariCP · Twilio SMS · Gmail SMTP

---

## Prerequisites
| Requirement | Version |
|---|---|
| Java (JDK) | 26 or later |
| JavaFX SDK | 26 or later |
| MySQL Server | 8.x |
| Eclipse IDE (for build) | 2024-09 or later |

---

## Database Setup (run once)

1. Start MySQL Server.
2. Open a MySQL client (Workbench, DBeaver, or CLI).
3. Run the provided script:
   ```sql
   source DB_3Assignment_7.sql
   ```
   This creates the `gonature_db` database, all tables, and seed data.

---

## Credentials Setup

The server needs Twilio and Gmail credentials for SMS/email notifications.

1. Copy the template:
   ```
   GoNatureServer/credentials.properties.template
       → GoNatureServer/credentials.properties
   ```
2. Fill in your real values (Twilio account SID/auth token, Gmail app password).
3. `credentials.properties` is listed in `.gitignore` and will **not** be committed.

> **Note for demo/grading:** The credentials file with working values will be  
> submitted separately (inside the ZIP archive). Do not commit it to GitHub.

---

## Running the Server

### From Eclipse
1. Open the `GoNatureServer` project.
2. Run `server.ServerUI` as a Java Application.
3. In the startup dialog, enter the MySQL root password (default: `root`) and click **Start Server**.
4. The server listens on port **5555** by default.

### From JAR (submission folder)
```
cd JAR/
RunServer.bat
```
Enter the MySQL password when prompted.

---

## Running the Client

### From Eclipse
1. Open the `GoNatureClient` project.
2. Run `client.ClientUI` as a Java Application.
3. In the connection dialog, enter the server's IP and port (default: `127.0.0.1:5555`).

### From JAR (submission folder)
```
cd JAR/
RunClient.bat
```

---

## Default Employee Accounts

| Role | Username | Password |
|---|---|---|
| Department Manager | `deptmgr` | `deptpass` |
| Park Manager (Park 1) | `pm1` | `pm1pass` |
| Park Manager (Park 2) | `pm2` | `pm2pass` |
| Gate Worker (Park 1) | `gate1` | `gate1pass` |
| Service Rep (Park 1) | `service1` | `s1pass` |

See `DB_3Assignment_7.sql` for the full list.

---

## Project Structure
```
GoNature/
├── GoNatureClient/        Client Eclipse project (JavaFX GUI)
│   └── src/
│       ├── client/        ChatClient, ClientUI, ClientConfig
│       ├── entities/      Shared data model (Serializable POJOs)
│       ├── gui/           FXML controllers and helper classes
│       └── ocsf/          OCSF client framework
├── GoNatureServer/        Server Eclipse project
│   └── src/
│       ├── database/      DBController (all SQL logic)
│       ├── entities/      Server-side entity copies
│       ├── gui/           Server UI controller
│       ├── ocsf/          OCSF server framework
│       └── server/        EchoServer, EmailSender, SmsSender, ConfirmationHttpServer
├── DB_3Assignment_7.sql          Database schema + seed data
├── DB_Content_Description.txt    Table-by-table schema description
├── G7_Assignment3.vpp            Visual Paradigm diagrams
└── README.md
```

---

## Architecture

```
JavaFX Client  ──TCP/5555──►  EchoServer  ──►  DBController  ──►  MySQL
                              (OCSF)             (HikariCP)
                                │
                                ├──►  EmailSender  (Gmail SMTP)
                                ├──►  SmsSender    (Twilio)
                                └──►  ConfirmationHttpServer (HTTP :8080)
```

All client-server messages use the `Message(command, data)` pattern over Java object serialization.

---

## Notification Flow
- **24 h before visit:** status changes to `Pending Confirm`; email + SMS with Confirm/Cancel links are sent.
- **If not confirmed within 2 h:** order is auto-cancelled; next waitlisted visitor is promoted.
- **Waitlist slot:** visitor has 1 h to confirm before the slot is offered to the next person.
