## Summary
Add CI checks for Android builds and Firestore rules, introduce a protected tag-based Android release workflow, and strengthen dependency hygiene.

## Changes
- Replace the mismatched Node/Webpack workflow with Android lint, JVM tests, debug APK build, and dependency review.
- Add a separate Firestore Rules CI workflow that runs the rules tests against the Firebase Emulator Suite.
- Add a `v*` tag-triggered release workflow. It builds signed APK/AAB artifacts only after verification and approval through the protected `release` environment.
- Add pinned direct Node dependencies for Firestore rules testing.
- Add weekly Dependabot updates for Gradle, npm, and GitHub Actions.
- Remove the release-signing fallback to the debug keystore and make Android lint failures fail the build.

## Required setup
Create a GitHub Environment named `release`, configure required reviewers, and add these environment secrets:
- `KEYSTORE_BASE64`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- `KEY_ALIAS`

Commit a generated `package-lock.json` and use `npm ci` in CI to lock transitive npm dependencies. Add and commit the Gradle wrapper, then use it in the Android workflows for reproducible builds.

## Validation
- `./gradlew :app:lintDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
- Run `npm run test:firestore` with the Firestore emulator

## Notes
The release workflow publishes APK and AAB files to a GitHub Release; it does not publish to Google Play. These CI changes do not turn the app’s simulated checkout or payout flows into live financial integrations.
