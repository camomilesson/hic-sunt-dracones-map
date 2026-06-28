# Hic Sunt Dracones

"Hic Sunt Dracones" (Here be Dragons) is an exploration-based Android application that gamifies the act of uncovering the world. As you move, you clear a "fog of war" represented by H3 hexagonal cells, revealing the map beneath.

## Project Setup & Installation

To build and run this project in Android Studio, you will need to provide your own API keys and configuration files which are excluded from version control for security.

### 1. Google Maps API Key
The application uses Google Maps SDK for Android.
1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable the **Maps SDK for Android**.
3. Create an API Key under **Credentials**.
4. In the root directory of this project, create a file named `local.properties` (or copy `local.properties.example`).
5. Add the following line to `local.properties`:
   ```properties
   MAPS_API_KEY=YOUR_ACTUAL_API_KEY_HERE
   ```

### 2. Firebase Configuration
The project uses Firebase Crashlytics for error reporting.
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with the package name `com.andrei.dracones`.
3. Download the `google-services.json` file.
4. Place the `google-services.json` file into the `app/` directory of the project.

### 3. Build and Run
1. Open the project in **Android Studio (Ladybug or newer)**.
2. Let Gradle synchronize.
3. Select an emulator or physical device (Android 9.0+ / API 28+).
4. Click **Run**.

## Key Features
- **Real-time Exploration**: Track your movement and clear H3 hexagonal cells on the map.
- **Customizable Map**: Choose between Default, Parchment, and Night themes.
- **Fog of War**: Persistent exploration data stored locally using Room.
- **Foreground Tracking**: Continue exploring even when the app is in the background via a persistent notification.



