# Build APK from Android with GitHub Actions

1. Create a GitHub repository from your phone.
2. Upload all files and folders from this project, including `.github/workflows/build-apk.yml`.
3. Open the repository's **Actions** tab.
4. Select **Build Fake GPS APK**.
5. Press **Run workflow** (or push to `main`/`master` to trigger it automatically).
6. Wait for the workflow to finish successfully.
7. Open the completed workflow run and find **Artifacts**.
8. Download `FakeGPSJoystickPro-debug.zip`.
9. Extract it on your phone. Inside is `app-debug.apk`.
10. Install the APK. Android may ask you to allow installation from the browser/file manager.

The workflow builds a debug APK using JDK 17, Android SDK 35, and Gradle 8.9.
