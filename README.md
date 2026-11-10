<div align="center">

# RideNow

**Android Ride Booking · FastAPI · Supabase · OpenStreetMap**

An educational full-stack ride-booking application connecting **passengers and drivers**, with trip requests, fare negotiation, location-based workflows, and account management.

![Android](https://img.shields.io/badge/Android-Java-3DDC84?logo=android&logoColor=black)
![FastAPI](https://img.shields.io/badge/Backend-FastAPI-009688?logo=fastapi&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?logo=python&logoColor=white)
![Supabase](https://img.shields.io/badge/Database-Supabase-3FCF8E?logo=supabase&logoColor=white)
![OpenStreetMap](https://img.shields.io/badge/Maps-OpenStreetMap-7EBC6F?logo=openstreetmap&logoColor=white)

[Features](#features) · [Architecture](#architecture) · [Getting started](#getting-started) · [Security notes](#security-and-project-limitations)

</div>

## Overview

RideNow demonstrates the development of a client-server mobile product using an Android application and a Python REST API. The repository contains separate client and driver experiences, authentication, ride-request workflows, and mapping features.

> **Project status:** Academic / portfolio project. The features below are documented in the repository and associated code; a complete live end-to-end verification has not been performed for this README update.

## Features

| Passengers | Drivers |
| :--- | :--- |
| Registration and login | Registration and login |
| Destination and map interaction | Availability and ride-request handling |
| Ride requests with proposed fares | Accept/reject and negotiate fares |
| Ride-status and tracking screens | Trip management and navigation workflows |
| History and profile screens | Driver dashboard and profile screens |

**Planned, not claimed as completed:** in-app payments, chat, Firebase push notifications, AI fare prediction, multilingual support, and offline operation.

## Architecture

```text
                 Android Application (Java / XML)
                   Passenger and Driver UI
                              |
                       OkHttp + JSON
                              |
                              v
                       FastAPI Backend
                 Authentication + REST APIs
                              |
                              v
                      Supabase / PostgreSQL
```

**Mobile:** Android Java, XML layouts, Gradle, Material components, OSMdroid / OpenStreetMap, Google Play Services Location, OkHttp, and Gson.

**Backend:** Python, FastAPI, Supabase client, JWT authentication, and bcrypt password hashing.

## Repository structure

```text
RideNow/
├── Backend/
│   ├── main.py                 # FastAPI routes
│   ├── auth.py                 # JWT and password helpers
│   ├── models.py               # Request/response models
│   ├── supabase_client.py      # Database client
│   └── database.py
├── RideNow/                    # Android Studio / Gradle project
│   ├── app/
│   │   ├── src/main/
│   │   └── build.gradle.kts
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle.kts
├── .gitignore
└── README.md
```

The repository currently also contains a tracked Python `venv/` directory and `__pycache__/` files under `Backend/`. These should be removed from version control in a separate cleanup commit; they are not needed in source control.

## Getting started

### Prerequisites

- Android Studio and an Android SDK compatible with the project's Gradle configuration
- Python 3 and pip
- A Supabase project with the database schema expected by the backend
- Android emulator or a physical Android device
- Network access for the Supabase backend and map services

### 1. Clone the repository

```bash
git clone https://github.com/mohamed-yawina/RideNow.git
cd RideNow
```

### 2. Set up the FastAPI backend

Create a fresh virtual environment instead of relying on the tracked `Backend/venv` folder.

```bash
cd Backend
python -m venv .venv
```

Activate it:

```powershell
# Windows PowerShell
.\.venv\Scripts\Activate.ps1
```

```bash
# macOS / Linux
source .venv/bin/activate
```

Install the dependencies used by the source code:

```bash
pip install fastapi uvicorn supabase python-dotenv python-jose passlib bcrypt
```

> The repository does not currently provide a root backend `requirements.txt`. The command above is a source-based starting point, **not a verified lockfile**.

Create `Backend/.env` locally:

```dotenv
SUPABASE_URL=your_supabase_project_url
SUPABASE_KEY=your_supabase_key
SECRET_KEY=replace_with_a_long_random_secret
```

Do not commit `.env` or share private keys. The database tables and policies must match the backend code; schema setup is not automated by these instructions.

Start the API:

```bash
uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

API documentation, when running successfully: **http://127.0.0.1:8000/docs**

### 3. Open the Android application

1. Open the **nested `RideNow/` directory** in Android Studio (the folder containing `settings.gradle.kts`).
2. Allow Gradle to sync and install the configured Android SDK.
3. Inspect the application's backend base URL and adapt it to your environment. An Android emulator cannot reach a host-machine backend through `localhost` in the same way as a desktop browser; network configuration may be required.
4. Run the app on an emulator or Android device and verify login, maps, and trip workflows.

## Security and project limitations

The inspected source contains several points that should be addressed before any real deployment:

- **JWT signing secret:** `Backend/auth.py` uses a hard-coded fallback value if `SECRET_KEY` is absent. Always configure a strong environment-specific secret; ideally, make startup fail when it is missing.
- **CORS:** `Backend/main.py` allows all origins. Restrict origins and review credential handling for a production deployment.
- **Dependency hygiene:** `Backend/venv/` and `Backend/__pycache__/` appear in the repository. Add appropriate `.gitignore` rules and remove already tracked generated files in a separate commit.
- **Backend dependencies:** Add a maintained `requirements.txt` or lockfile for reproducible setup.
- **Database and permissions:** Review Supabase Row Level Security, access policies, and role checks before using real accounts.
- **Connectivity:** Confirm the API URL, device networking, and HTTPS configuration for the intended Android environment.
- **Verification:** The README update did not execute the Android app, integration tests, or a Supabase deployment.

This is a learning and demonstration project, **not a production-ready ride-hailing service**.

## Roadmap

- [ ] Add reproducible backend dependency management
- [ ] Remove tracked Python environments and generated cache files
- [ ] Harden JWT secret handling and backend access controls
- [ ] Add automated API and Android tests
- [ ] Document the Supabase schema and configuration
- [ ] Add genuine screenshots and a short demo
- [ ] Explore payments, push notifications, and real-time messaging

## Author

**Mohamed Yawina** · Software Engineering Student, EMSI

[GitHub](https://github.com/mohamed-yawina) · [LinkedIn](https://www.linkedin.com/in/mohamed-yawina/)

---

<div align="center"><sub>RideNow — an educational exploration of mobile, backend, and location-based software.</sub></div>
