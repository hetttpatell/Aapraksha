# Aapraksha - Comprehensive Personal Safety Application

## 1. Overview
**Aapraksha** (translating to *Self-Defense / Protection*) is a robust, intelligent, and highly responsive Android-based personal safety application. Built on a modernized technology stack utilizing Java, Firebase, and Google Cloud, Aapraksha offers seamless onboarding, proactive safety monitoring, and a rapid SOS trigger mechanism. It is designed to be the ultimate digital companion for users navigating potentially unsafe environments, creating a protective ecosystem that instantly bridges the gap between a user in distress and their trusted emergency contacts or local community.

## 2. Key Features and Modules

*   **Seamless Authentication & Profile Management**
    Supports secure authentication via Email/Password and Google OAuth Sign-in, backed by Firebase Authentication and Firestore for reliable user profile state management.
*   **Streamlined Onboarding & Security PIN**
    First-time users undergo an intuitive 4-step interactive onboarding process to learn the SOS workflow. Crucially, they are prompted to set up a personalized 4-digit **Emergency PIN**. This PIN acts as a critical safety layer, authenticating the user when attempting to disable an active SOS alert, preventing unauthorized individuals (like attackers) from canceling the alarm.
*   **Instant SOS Lifecycle Management**
    *   **Hardware Trigger:** SOS can be initiated inconspicuously using device volume buttons (powered by a background `VolumeButtonService`).
    *   **Software Trigger:** A prominent, easily accessible in-app SOS button on the main Dashboard.
    *   **SOS Audio Recording:** Once an SOS is triggered, the app can automatically begin recording ambient audio to provide crucial context and evidence of the situation.
*   **Real-Time Location Tracking**
    During an emergency, the `LocationTrackingService` continuously tracks the victim’s movement in the background and streams high-accuracy coordinates to connected emergency contacts in real time.
*   **Multi-Channel Broadcasting**
    Utilizes an onboard `SMSHelper` to dispatch SMS notifications (containing distress messages and live map links) to predefined Emergency Contacts, alongside cloud-based Push Notifications powered by Firebase.
*   **Network and Community Alerts**
    Beyond personal contacts, Aapraksha facilitates `NetworkAlerts` to notify nearby application users or community members about localized threats, fostering community-driven safety.
*   **Historical Data & Activity Logs**
    Maintains a detailed `LocationHistory` and post-event `AlertHistory` for incident analysis, legal reporting, or peace of mind.

---

## 3. Complete Application Workflow

### Phase 1: Registration & Initial Setup
1.  **Installation:** The user installs the application and is welcomed by the Splash Screen.
2.  **Authentication:** The user registers or logs in via Google Sign-In or Email/Password.
3.  **Onboarding:** If the user is new, they are routed to the `OnboardingActivity`—a 4-slide interactive tutorial detailing the application's capabilities.
4.  **Security Initialization:** Post-onboarding, the user is required to set a 4-digit Emergency PIN.
5.  **Dashboard Access:** The user lands on the primary `DashboardActivity`, where they are prompted to add their trusted Emergency Contacts and grant necessary system permissions (Location, SMS, Audio).

### Phase 2: Active Monitoring (Everyday Use)
1.  **Background Readiness:** The app silently runs background services (e.g., `VolumeButtonService`, `LocationTrackingService` in idle state) optimized to prevent unnecessary battery drain.
2.  **Configuration:** The user can navigate to the Settings to customize notification preferences and adjust privacy/location sharing rules.

### Phase 3: Emergency Trigger (SOS Mode)
1.  **Activation:** The user feels threatened and triggers the SOS by rapidly pressing the volume hardware buttons (discreet) or tapping the SOS button within the application UI.
2.  **Alert Initialization:** `SosTriggeredActivity` takes over the screen.
    *   `LocationTrackingService` shifts to an aggressive tracking mode to fetch live, high-accuracy GPS coordinates.
    *   `SosAudioRecorder` secretly captures ambient audio.
    *   `SMSHelper` immediately dispatches formatted SMS messages with dynamic Google Maps tracking links to the user's Emergency Contacts.
    *   `SOSAlertRepository` logs the event to Firebase Firestore, initiating a cloud-level broadcast to nearby network members.

### Phase 4: Emergency Resolution
1.  **Contact Response:** Emergency contacts receive the SMS/Notification, open their response portal, and begin tracking the user's real-time location. Contacts can acknowledge they are en route.
2.  **Safe Termination:** Once the user is safe, they attempt to stop the SOS. To successfully terminate it, the application mandates the entry of the 4-digit Emergency PIN.
3.  **Post-Incident Logging:** The alert logs, audio snippets, and location traces are finalized and saved in History for future reference or incident reporting.

---

## 4. Primary Use Cases

*   **Night Commuting & Solo Travel:** A user traveling late at night or navigating unfamiliar areas uses Aapraksha to keep their location tracked, with the hardware button ready to alert family members instantly without drawing attention to themselves.
*   **High-Risk Altercations:** A user in a distress situation (e.g., domestic threat, robbery) can use the hardware button to trigger the alarm invisibly, notifying local authorities or contacts while secretly recording audio evidence.
*   **Medical Emergencies:** Can be configured for elderly individuals or patients, where a simple button press summons family members or caretakers with an exact real-time location.
*   **Community Safety & Crowd-Sourcing:** Nearby civilian users of Aapraksha receive a `NetworkAlert` regarding an active distress signal nearby, enabling quick bystander intervention even before official authorities arrive.
