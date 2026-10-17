# Cozitopia

Cozitopia is an Android app with the vision to turn your home into the one you’ve always dreamed of.


## Cozitopia app

Cozitopia lets homies share a home on Supabase: email/password accounts, homes with admins and homies, invitations, a Happy Home Checklist imported from CSV with proposed assignees and repetitions, and a Homies Calendar with `.ics` export. The UI is available in English, German and French and follows the system language, with an in-app override. The Android app and the web app (Compose Multiplatform, wasmJs) share one code base in `app/`.

### Setup

Prerequisites: JDK 17 or newer; for Android also the Android SDK (platform 37) and a device or emulator.

1. Create a Supabase project at https://supabase.com/dashboard.
2. In its SQL Editor run `supabase/schema.sql`, then optionally `supabase/rls_check.sql` (it rolls back and leaves no data). An empty one-row result means all checks passed.
3. For testing, disable "Confirm email" under Authentication > Providers > Email.
4. Copy the Project URL (without `/rest/v1/`) and the publishable key (`sb_publishable_...`) from Project Settings > API Keys. Never use the secret key. Store them outside the repo in `~/.gradle/gradle.properties`:

```
supabaseUrl=https://<project-ref>.supabase.co
supabaseAnonKey=sb_publishable_...
```

Alternatively set the environment variables `SUPABASE_URL` and `SUPABASE_ANON_KEY`.
5. Android only: create `app/local.properties` with `sdk.dir=/path/to/Android/sdk`.

### How to Run the app

Web, then open the printed URL (usually http://localhost:8080):

```
cd app && ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Android, build the APK (`app/androidApp/build/outputs/apk/debug/`) or install it on an attached device:

```
cd app && ./gradlew :androidApp:assembleDebug
cd app && ./gradlew :androidApp:installDebug
```

After changing the Supabase values, rebuild so they are compiled in.

### Tests

`cd app && ./gradlew jvmTest`

### Checklist CSV format

Columns `What, Where, Frequency, Duration, Assignee` with an optional header; comma, semicolon or tab delimiters; several assignees separated by `;`; `Everyone` / `Unassigned` (or their German and French equivalents) for all or nobody. Use "Download template" in the checklist screen for an example, see `ChecklistCsv.TEMPLATE`.

### License

Licensed under the Apache License 2.0 (Apache-2.0), see [LICENSE](LICENSE).
