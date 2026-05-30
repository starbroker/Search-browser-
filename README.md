# Search Minimalist Web Browser

A premium minimalist web browser application built using Kotlin and Jetpack Compose, featuring lightning-fast secure lookup under the Hood.

---

## 🚀 GitHub Actions CI/CD (Auto-Release APK)

A GitHub Actions workflow has been fully configured for this repository to automate building the application and publishing the APKs directly to your GitHub repository.

### How to Trigger the Build

The workflow is configured to run automatically on:
1. **Repository Tags (`*`)**: Whenever you push a release tag like `v1.0.0` or `1.0`, it builds the app and publishes a **GitHub Release** complete with the APK file.
2. **Branch Pushes**: Any push or pull request to the `main` or `master` branches triggers a build to verify compilation, and saves the built APK as a downloadable **Action Artifact**.
3. **Manual Execution**: You can manually trigger a workflow run at any time from the **Actions** tab on your GitHub repository.

---

## 🛠️ Build Outputs

The build produces two types of APK files depending on your configuration:

1. **Debug APK (`Search-[version]-debug.apk`)**
   - Built automatically without any configuration.
   - Uses the standard debug key.
   - Perfect for testing, side-loading, and fast iteration.

2. **Signed Release APK (`Search-[version]-release.apk`)**
   - Built and signed automatically **only if** you set up the repository secrets on GitHub described below.

---

## 🔑 Setting up Production Release Signing (Optional)

To enable automatic signed release builds, add the following **Repository Secrets** to your GitHub repository settings under `Settings` -> `Secrets and variables` -> `Actions`:

1. **`KEYSTORE_BASE64`**:
   The `base64` encoded string of your Java Keystore (`.jks` or `.keystore`) file.
   *Windows command to generate:* `certutil -encode my-upload-key.jks tmp.txt && type tmp.txt`
   *macOS/Linux command:* `base64 -i my-upload-key.jks`
2. **`STORE_PASSWORD`**:
   The master password of your keystore keystore file.
   *Example:* `myStorePassword123`
3. **`KEY_PASSWORD`**:
   The password of your release key alias.
   *Example:* `myKeyPassword123`

When these secrets are provided, the workflow will automatically sign the APK and append the signed release file into your GitHub Releases!

---

## 📄 License

This open source code is licensed under the MIT License.
