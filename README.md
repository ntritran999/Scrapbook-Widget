# Scrapbook Widget

<p align="center">
  <img src="app\src\main\ic_launcher-playstore.png" alt="Scrapbook Widget Logo" width="160" />
</p>

<p align="center"><strong>Capture, decorate, and relive shared memories with your closest groups.</strong></p>

<p align="center">
  <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build Status" />
  <img src="https://img.shields.io/badge/license-NONE-lightgrey" alt="License" />
  <img src="https://img.shields.io/badge/version-1.0.0-blue" alt="Version" />
  <img src="https://img.shields.io/badge/last_commit-unknown-lightgrey" alt="Last Commit" />
  <img src="https://img.shields.io/badge/open_issues-unknown-lightgrey" alt="Open Issues" />
</p>

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Environment Variables](#environment-variables)
- [Usage / Examples](#usage--examples)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)

## Overview

**Scrapbook Widget** is a collaborative memory-sharing mobile application where users capture moments, style them like scrapbook pieces, and share them into group pages and chat threads.

### What problem it solves

Traditional photo sharing apps are optimized for feeds and likes, not intimate group memory-building. Scrapbook Widget focuses on:

- Small-group storytelling
- Fast camera-to-memory workflows
- Playful visual editing and page composition
- Real-time chat around memories
- Always-visible memories through Android home-screen widgets

### Target users

- Friend groups
- Couples/families
- Small communities that want a private memory space
- Users who want "camera + scrapbook + chat" in one flow

### Demo Placeholder

![Screenshot 1](docs/assets/screenshot1.png)
![Screenshot 2](docs/assets/screenshot2.png)
![Screenshot 3](docs/assets/screenshot3.png)

## Features

- Authentication and onboarding:
- Email/password login
- OTP-assisted registration flow
- Google Sign-In via Firebase Auth + backend verification

- Group collaboration:
- Create groups and invite members
- Join groups by invite link (`scrapbook://invite?code=...`)
- Accept/decline invitations
- Group member management and leave-group flow

- Scrapbook experience:
- Multi-page group scrapbook model
- Add photos/items to pages
- Captions and metadata support
- Page curl and photo flip visual effects
- Background selection and page rendering states

- Camera and editing:
- CameraX capture with flash/zoom/front-back switching
- In-app image editing with drawing/mask overlays
- Save edited output to gallery
- Paste edited image directly into scrapbook

- AI-assisted features:
- Smart Reply suggestions in chat (ML Kit)
- Face detection + embedding extraction (ML Kit + MobileFaceNet LiteRT)
- Optional face enrollment flow to support face-tagging scenarios
- User-facing toggle for AI features in app settings

- Chat and realtime:
- Group chat with optimistic sending state
- Seen/unread handling
- Real-time updates over WebSocket
- Today-memory surfacing in chat UI

- Notifications and widget:
- Firebase Cloud Messaging integration
- Notification categories for messages, photo events, reactions
- Android App Widget showing latest memory/status
- Periodic widget refresh via WorkManager (every 30 minutes)

- Offline-aware behavior:
- Local cache of key user/group/message/memory data
- Graceful fallback messaging when network is unavailable

## Tech Stack

| Layer | Technologies |
| --- | --- |
| Mobile Client | Java 11, Android SDK (min 29 / target 36), Fragments, Navigation, DataBinding, ViewBinding |
| Architecture | MVVM, LiveData, Repository pattern, Hilt DI |
| Camera/Media | CameraX, Glide, uCrop |
| AI/ML | ML Kit Face Detection, ML Kit Smart Reply, LiteRT (`MobileFaceNet.tflite`) |
| Networking | Retrofit 3, Gson Converter, OkHttp 4 (logging + interceptors), WebSocket |
| Auth & Cloud | Firebase Auth, Firebase Messaging, Firebase Analytics, Firebase Firestore |
| Background Jobs | WorkManager + Hilt Worker integration |
| Real-time | Custom `GroupRealtimeSocketClient` over WebSocket |
| Widget | Android AppWidget APIs + Glide `AppWidgetTarget` |
| Backend/API | External REST API (`/api/v1`) consumed by Retrofit services |
| Database | Firestore used in parts of repository layer; backend database is managed in the companion API service (not in this repo) |
| Infrastructure/Deploy (Inferred) | Android client build/deploy pipeline + self-hosted/cloud backend + Firebase services |

## Prerequisites

- Android Studio (Hedgehog or newer recommended)
- JDK 11
- Android SDK Platform 36 + Build Tools
- Gradle compatibility with AGP `8.13.2`
- A running backend API compatible with this client
- Firebase project configured for Authentication, Cloud Messaging, and Firestore (Analytics is optional but included)

Recommended local tools:

- Git
- A physical Android device or emulator (API 29+)
- `adb` for deep-link and notification testing

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/ntritran999/Scrapbook-Widget.git
cd Scrapbook-Widget
```

### 2. Configure Firebase

1. Place `google-services.json` in `app/` (already present in this repo; replace with your project if needed).
2. Ensure your Firebase project has Android app registration for package:

```text
com.group04.scrapbookwidget
```

3. Confirm OAuth Web Client ID in `app/src/main/res/values/strings.xml` (`web_client_id`) matches your Firebase/Google setup.

### 3. Configure Backend Base URL

The app currently hardcodes API base URL in:

- `app/src/main/java/com/group04/scrapbookwidget/data/service/ServiceModule.java`

Current value:

```java
private final String BASE_URL = "http://192.168.1.6:3000/api/v1/";
```

Update this to a reachable backend URL for your environment.

### 4. Install Dependencies / Sync Project

- Open the project in Android Studio.
- Let Gradle sync automatically.
- Or via command line:

```bash
./gradlew tasks
```

Windows:

```powershell
.\gradlew.bat tasks
```

### 5. Run the App (Dev)

```powershell
.\gradlew.bat installDebug
```

Then launch `ScrapbookWidget` on device/emulator.

### 6. Database Migrations

This mobile repo does not include server-side migrations.

- Run database migrations in the **companion backend repository** (if your backend uses SQL/NoSQL schema migration tools).

### 7. Optional Docker Setup (Backend Side)

No `Dockerfile` or `docker-compose.yml` is included in this mobile client repository. If your team runs the backend via Docker, use your API repo's compose setup.

Example placeholder:

```yaml
services:
  api:
    image: your-org/scrapbook-api:latest
    ports:
      - "3000:3000"
    environment:
      - PORT=3000
      - NODE_ENV=development
```

## Environment Variables

This Android client currently relies mostly on code/config files (not a native `.env` loader). For team-friendly setup, use the following contract in your backend and local build pipeline.

### A. Mobile Client Configuration (Recommended Externalization)

| Variable | Required | Example | Used By | Description |
| --- | --- | --- | --- | --- |
| `API_BASE_URL` | Yes | `http://10.0.2.2:3000/api/v1/` | Retrofit/Hilt module | Base URL for all REST calls and websocket conversion |
| `GOOGLE_WEB_CLIENT_ID` | Yes | `12345-abc.apps.googleusercontent.com` | Google Sign-In | OAuth client ID used for token acquisition |
| `ENABLE_HTTP_LOGGING` | No | `true` | OkHttp | Enables verbose request/response logs in dev |
| `DEFAULT_AI_FEATURES_ENABLED` | No | `true` | App settings | Default value for AI toggle |
| `DEFAULT_PAGE_CURL_ENABLED` | No | `true` | Scrapbook UI | Default page curl preference |

### B. Backend/API Environment (Inferred)

| Variable | Required | Example | Description |
| --- | --- | --- | --- |
| `PORT` | Yes | `3000` | API server port |
| `API_PREFIX` | Yes | `/api/v1` | Route prefix expected by mobile client |
| `FIREBASE_PROJECT_ID` | Yes | `my-firebase-project` | Firebase Admin verification context |
| `FIREBASE_CLIENT_EMAIL` | Yes | `firebase-adminsdk@...` | Firebase Admin service account identity |
| `FIREBASE_PRIVATE_KEY` | Yes | `-----BEGIN PRIVATE KEY-----...` | Firebase Admin signing key |
| `WS_ENABLED` | No | `true` | Enables websocket endpoints for realtime group events |
| `CORS_ORIGINS` | No | `*` | Cross-origin policy (if backend serves web tooling too) |

## Usage / Examples

### Start the App and Validate Core Flow

1. Launch app.
2. Register/login (email, OTP flow, or Google).
3. Create or join a group.
4. Open camera, capture/edit image.
5. Paste image into scrapbook page with caption.
6. Open chat for smart replies and realtime updates.
7. Add home-screen widget and verify periodic updates.

### Deep Link Invite Testing (ADB)

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "scrapbook://invite?code=INVITE_CODE"
```

### Build and Test Commands

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Run unit tests
.\gradlew.bat testDebugUnitTest

# Run instrumented tests (device/emulator required)
.\gradlew.bat connectedDebugAndroidTest

# Android lint
.\gradlew.bat lintDebug
```

### API Call Example: Send Group Message

```bash
curl -X POST "http://localhost:3000/api/v1/groups/<groupId>/messages" \
  -H "Authorization: Bearer <firebase_id_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello scrapbook team!",
    "createdBy": "<userId>",
    "type": "text"
  }'
```

### API Call Example: Join by Invite Link

```bash
curl -X POST "http://localhost:3000/api/v1/groups/join-by-link" \
  -H "Authorization: Bearer <firebase_id_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "ABC123"
  }'
```

## API Reference

Base URL pattern (configured client-side):

```text
{API_BASE_URL} = http://<host>:<port>/api/v1/
```

Auth: most endpoints are called through an OkHttp interceptor that attaches Firebase ID token as `Authorization: Bearer <token>` when user is logged in.

### Auth Module

| Method | Endpoint | Description | Auth Required | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | `/auth/login` | Email/password login | No | `User` credentials | `User` |
| POST | `/auth/register/otp` | Request registration OTP | No | `{ "email": string }` | `RegisterOtpResponse` |
| POST | `/auth/register` | Complete registration with OTP | No | `RegisterOtpConfirmRequest` | `RegisterResponse` |
| POST | `/auth/session` | Verify Firebase session token | Yes | `{ "idToken": string }` | `User` |
| POST | `/auth/signout` | Server-side logout hook | Yes | none | `204/200` |
| DELETE | `/auth/account` | Delete current account | Yes | none | `204/200` |
| POST | `/auth/google` | Login with Google/Firebase token | No (token in body) | `{ "idToken": string }` | `User` |

Example request:

```json
{
  "idToken": "eyJhbGciOi..."
}
```

Example response:

```json
{
  "id": "u_123",
  "email": "user@example.com",
  "displayName": "Alex",
  "avatarUrl": "https://cdn.example.com/avatar.jpg"
}
```

### User Module

| Method | Endpoint | Description | Auth Required | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | `/users` | Get users list | Yes | none | `User[]` |
| GET | `/users/{userId}` | Get user by ID | Yes | none | `User` |
| POST | `/users` | Create user | Yes | `User` | `User` |
| PATCH | `/users/{userId}` | Update user profile fields | Yes | Partial `User` | `User` |
| GET | `/users/{userId}/groups` | Get groups for user | Yes | none | `Group[]` |
| GET | `/users/check-username?q=...` | Validate username availability | Yes | none | `UsernameCheckResponse` |
| POST | `/users/avatar` | Upload avatar image | Yes | Multipart file | `AvatarUploadResponse` |
| POST | `/users/{userId}/enroll-face` | Save face embedding vector | Yes | `{ "faceVector": number[] }` | `FaceEnrollmentResponse` |
| POST | `/users/me/device-token` | Register device token for push | Yes | `DeviceTokenRequest` | `204/200` |
| DELETE | `/users/me/device-token?token=...` | Delete registered push token | Yes | none | `204/200` |
| PATCH | `/users/me/device-token/settings` | Update push notification settings | Yes | `NotificationSettingsRequest` | `204/200` |

Example face enrollment request:

```json
{
  "faceVector": [0.001, -0.023, 0.187, 0.045]
}
```

Example username check response:

```json
{
  "available": true,
  "valid": true,
  "reason": "ok"
}
```

### Group Module

| Method | Endpoint | Description | Auth Required | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | `/groups` | List groups | Yes | none | `Group[]` |
| GET | `/groups/{groupId}` | Get group details | Yes | none | `Group` |
| POST | `/groups` | Create group | Yes | `{ groupName, avatarUrl, memberIds[] }` | `Group` |
| PATCH | `/groups/{groupId}/name` | Update group name | Yes | `{ groupName }` | `Group` |
| PATCH | `/groups/{groupId}/avatar` | Update group avatar URL | Yes | `{ avatarUrl }` | `Group` |
| POST | `/groups/{groupId}/avatar` | Upload group avatar file | Yes | Multipart file | `AvatarUploadResponse` |
| GET | `/groups/{groupId}/members` | List group members | Yes | none | `User[]` |
| POST | `/groups/{groupId}/leave` | Leave group | Yes | none | `LeaveGroupResponse` |
| DELETE | `/groups/{groupId}/members/{userId}` | Remove member from group | Yes | none | `204/200` |
| GET | `/users/discover?q=...` | Search users to invite | Yes | none | `User[]` |
| POST | `/groups/{groupId}/invitations` | Invite user to group | Yes | `{ userId }` | `204/200` |
| GET | `/groups/{groupId}/invite-link` | Generate/fetch invite link | Yes | none | `InviteLinkResponse` |
| POST | `/groups/join-by-link` | Join group with invite code | Yes | `JoinByLinkRequest` | `JoinByLinkResponse` |
| PUT | `/groups/{groupId}/members/{userId}` | Add member directly | Yes | none | `204/200` |
| GET | `/groups/invitations/me` | List my invitations | Yes | none | `Invitation[]` |
| POST | `/groups/{groupId}/invitations/accept` | Accept invitation | Yes | none | `204/200` |
| POST | `/groups/{groupId}/invitations/decline` | Decline invitation | Yes | none | `204/200` |

Example create group request:

```json
{
  "groupName": "Weekend Crew",
  "avatarUrl": "",
  "memberIds": ["u_1", "u_2"]
}
```

Example invite-link response:

```json
{
  "inviteLink": "scrapbook://invite?code=ABC123",
  "inviteCode": "ABC123"
}
```

### Scrapbook Pages & Items Module

| Method | Endpoint | Description | Auth Required | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | `/groups/{groupId}/scrapbook-pages` | List scrapbook pages | Yes | none | `ScrapbookPage[]` |
| POST | `/groups/{groupId}/scrapbook-pages` | Create scrapbook page | Yes | `ScrapbookPage` | `ScrapbookPage` |
| DELETE | `/groups/{groupId}/scrapbook-pages/{pageId}` | Delete scrapbook page | Yes | none | `204/200` |
| GET | `/groups/{groupId}/scrapbook-pages/{pageId}/items` | List page items | Yes | none | `ScrapbookItem[]` |
| GET | `/groups/{groupId}/scrapbook-pages/{pageId}/{itemId}` | Get single scrapbook item | Yes | none | `ScrapbookItem` |
| POST | `/groups/{groupId}/scrapbook-pages/{pageId}/items` | Create item (JSON payload) | Yes | `ScrapbookItem` | `ScrapbookItem` |
| POST | `/groups/{groupId}/scrapbook-pages/{pageId}/items` | Create item with uploaded file | Yes | Multipart (`file` + `payload`) | `ScrapbookItem` |

Example create item request:

```json
{
  "content": {
    "imageUrl": "https://cdn.example.com/photo.png",
    "caption": "Sunset from the bridge"
  },
  "layout": {
    "x": 90,
    "y": 140,
    "width": 240,
    "height": 280,
    "rotation": 8,
    "scale": 1
  }
}
```

Example response:

```json
{
  "id": "item_456",
  "pageId": "page_123",
  "createdBy": "u_1"
}
```

### Chat, Memories, and Reactions Module

| Method | Endpoint | Description | Auth Required | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | `/groups/{groupId}/messages` | Fetch chat messages | Yes | none | `Message[]` |
| POST | `/groups/{groupId}/messages` | Send chat message | Yes | `{ content, createdBy, type }` | `Message` |
| PUT | `/groups/{groupId}/messages/{messageId}/seen-by/{userId}` | Mark message as seen | Yes | none | `Message.SeenBy` |
| GET | `/groups/{groupId}/today-memory` | Get today memory cards | Yes | none | `TodayMemory[]` |
| GET | `/groups/{groupId}/scrapbook-pages/{pageId}/{itemId}/reactions` | Get item reactions | Yes | none | `Reaction[]` |
| POST | `/groups/{groupId}/scrapbook-pages/{pageId}/{itemId}/reactions` | Add reaction | Yes | `Reaction` | `Reaction` |
| DELETE | `/groups/{groupId}/scrapbook-pages/{pageId}/{itemId}/{userId}` | Remove user reaction | Yes | none | `boolean` |

Example send message request:

```json
{
  "content": "Look at this memory!",
  "createdBy": "u_1",
  "type": "text"
}
```

Example message response:

```json
{
  "id": "msg_99",
  "content": "Look at this memory!",
  "createdBy": "u_1",
  "createdAt": "2026-05-18T09:12:00.000Z"
}
```

### Widget and Background Module

| Method | Endpoint | Description | Auth Required | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | `/users/{userId}/widgets` | Fetch widget payload for user | Yes | none | `Widget[]` |
| GET | `/backgrounds` | List available scrapbook backgrounds | Yes (typically) | none | `string[]` |

Example widget response:

```json
[
  {
    "groupId": "group_1",
    "pageId": "page_1",
    "latestPhotoUrl": "https://cdn.example.com/photo.jpg",
    "senderAvatar": "https://cdn.example.com/avatar.jpg",
    "status": "Just now"
  }
]
```

## Project Structure

```text
Scrapbook-Widget/
├─ app/
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ java/com/group04/scrapbookwidget/
│  │  │  │  ├─ data/
│  │  │  │  │  ├─ cache/                 # Offline cache store + network status helper
│  │  │  │  │  ├─ model/                 # DTOs/domain models used across app
│  │  │  │  │  ├─ realtime/              # WebSocket client for group realtime events
│  │  │  │  │  ├─ repository/            # Repository interfaces + implementations
│  │  │  │  │  ├─ service/               # Retrofit service contracts + DI providers
│  │  │  │  │  └─ worker/                # WorkManager workers (widget updates)
│  │  │  │  ├─ ml/                       # Face detection/embedding logic
│  │  │  │  ├─ notifications/            # FCM service, channels, device token sync
│  │  │  │  ├─ ui/
│  │  │  │  │  ├─ auth/                  # Login/register screens and viewmodels
│  │  │  │  │  ├─ camera/                # Camera + image editor workflows
│  │  │  │  │  ├─ group/                 # Chat, group settings, invitations
│  │  │  │  │  ├─ pagecurl/              # OpenGL/page curl rendering
│  │  │  │  │  ├─ scrapbookview/         # Main scrapbook scene and page controls
│  │  │  │  │  └─ adapter/               # Recycler/spinner adapters
│  │  │  │  └─ ScrapbookWidgetApplication.java  # Hilt app + scheduled worker bootstrap
│  │  │  ├─ res/                         # Layouts, drawables, nav graphs, themes, raw assets
│  │  │  ├─ assets/MobileFaceNet.tflite  # Face embedding model
│  │  │  └─ AndroidManifest.xml          # App permissions, deep links, receivers, services
│  │  ├─ androidTest/                    # Instrumented tests (camera/auth/effects)
│  │  └─ test/                           # Local unit tests
│  └─ build.gradle.kts                   # App module config and dependencies
├─ gradle/libs.versions.toml             # Version catalog
├─ build.gradle.kts                      # Root build config
├─ settings.gradle.kts                   # Gradle settings
└─ README.md
```