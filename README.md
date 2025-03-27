Interactive Physical Proficiency Trainer
Description
The Interactive Physical Proficiency Trainer is a mobile fitness application designed to help users track their physical activities, such as running, push-ups, and sit-ups, using only their smartphones. It integrates real-time tracking, pose estimation for exercise validation, and augmented reality (AR) features to guide users in maintaining proper form during workouts. The app provides accurate metrics for running, and exercise rep counting, and offers helpful tutorials to improve performance.

Key Features:
Push-up and Sit-up Rep Counting: Automatically counts reps based on body angles using ML Kit Pose Detection.

Running Tracker: Tracks distance, time, and pace using the device’s GPS.

Augmented Reality (AR): Provides real-time AR tutorials for proper push-up and sit-up form.

Offline Support: The app works entirely offline, making it accessible in outdoor or low-connectivity environments.

Real-Time Feedback: Provides immediate visual and audio feedback to help correct posture and improve workout performance.

Installation Instructions
Prerequisites:
Android Studio (recommended version: 4.1 or later)

Kotlin (supported by Android Studio)

An Android device or emulator (for testing)

Step-by-Step Installation:
Clone the repository:

Download or clone the repository to your local machine.

bash
Copy
git clone https://github.com/yourusername/ippt-fitness-tracker.git
Open in Android Studio:

Open Android Studio.

Click on File > Open and navigate to the folder where the project is saved.

Select the project folder and click Open.

Set up dependencies:

Android Studio will automatically sync the project dependencies when the project is opened. If it does not, you can manually sync the project by clicking File > Sync Project with Gradle Files.

Install necessary SDKs:

Ensure that the following SDKs are installed via Android Studio’s SDK Manager:

Android SDK

Google Play Services

ML Kit SDK (for pose detection)

Google Maps SDK (for running route visualization)

Configure permissions:

Ensure the following permissions are added in the AndroidManifest.xml file:

xml
Copy
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
Run the app:

Connect an Android device via USB or use an emulator in Android Studio.

Click on Run (the green play button) to install and launch the app on your device.

Testing:

Test the app’s key features: running tracking, push-up, and sit-up counting with pose detection, and AR form visualization.

Ensure that the app is tracking metrics correctly and providing real-time feedback during workouts.

Troubleshooting
Permission Issues:
Make sure that the app has the necessary permissions (camera and location) enabled. If permissions are denied, the app may not function correctly.

App Not Detecting Exercises:
Ensure that the camera is unobstructed and the user is in a well-lit environment. For optimal pose detection, the user should be facing the camera with sufficient body visibility.

Future Improvements
Integration with wearable devices for enhanced fitness tracking.

Addition of more workout types with AI-based form detection.

Cloud storage for syncing workout history across multiple devices.
