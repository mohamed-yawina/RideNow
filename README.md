# 🚖 RideNow - Smart Ride Booking Mobile App

## 📱 Overview

RideNow is an intelligent Android mobile application that connects passengers and drivers in real time.  
It allows users to request rides, negotiate prices, track trips live, and manage transportation efficiently through a modern and secure platform.

This project was developed as a full-stack application combining mobile development (Android) and backend APIs (FastAPI).

---

## 🎯 Features

### 👤 Client Features
- Secure registration and login
- Real-time geolocation
- Destination search
- Ride request with price proposal
- Driver tracking on map
- Ride status tracking (pending, accepted, in progress, completed)
- Ride history
- Profile management

### 🚗 Driver Features
- Secure registration and login
- Online / offline status management
- View available ride requests
- Accept / reject rides
- Price negotiation with clients
- GPS navigation to client and destination
- Ride management (start / complete)
- Statistics dashboard

---

## 🛠️ Technologies Used

### 📱 Frontend (Android)
- Java
- XML (UI Design)
- Android Studio
- Material Design
- OSMdroid (OpenStreetMap)
- OkHttp (API calls)
- Gson (JSON parsing)

### ⚙️ Backend
- FastAPI (Python)
- Supabase (PostgreSQL Database)
- JWT Authentication
- Bcrypt Password Hashing

---

## 🏗️ Architecture

The project follows a **Client-Server architecture**:

- **Android App (Frontend)** → User interface for clients and drivers
- **FastAPI Backend** → Business logic and API management
- **Supabase Database** → Data storage and management

---

## 📂 Project Structure
```
RideNow/
├── app/ # Android application
│ ├── activities/
│ ├── fragments/
│ ├── network/
│ └── utils/
│
├── backend/ # FastAPI backend
│ ├── main.py
│ ├── auth.py
│ ├── models.py
│ └── supabase_client.py
```

---

## 🔐 Security

- JWT-based authentication
- Password hashing with Bcrypt
- Secure API endpoints
- Role-based access (Client / Driver)

---


## 👨‍🎓 Author

- **Name:** Mohamed Yawina  
- **Project:** RideNow Mobile App  
- **Academic Year:** 2025–2026  

---

## 📌 Repository

GitHub:  
👉 https://github.com/mohamed-yawina/RideNow

---

## 📈 Future Improvements

- In-app payment system
- Real-time chat (client & driver)
- Push notifications (Firebase)
- AI-based price estimation
- Multi-language support
- Offline mode

---

## 📄 License

This project is for educational purposes.
