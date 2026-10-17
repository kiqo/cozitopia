# Cozitopia

Cozitopia is an Android app with the vision to turn your home into the one you’ve always dreamed of.


## Cozitopia app

Cozitopia lets homies share a home on Supabase: email/password accounts, homes with admins and homies, invitations, a Happy Home Checklist imported from CSV with proposed assignees and repetitions, and a Homies Calendar with `.ics` export. The UI is available in English, German and French and follows the system language, with an in-app override. The Android app and the web app (Compose Multiplatform, wasmJs) share one code base in `app/`.

### Setup

1. Create a Supabase project and apply `supabase/schema.sql` in the SQL editor. Optionally run `supabase/rls_check.sql` there to verify the isolation between homes (it rolls back).
2. For testing, disable "Confirm email" under Authentication in the Supabase dashboard.
3. Provide the project values without committing them, either in `~/.gradle/gradle.properties` as `supabaseUrl=...` and `supabaseAnonKey=...` or as the environment variables `SUPABASE_URL` and `SUPABASE_ANON_KEY`.
4. Create `app/local.properties` with `sdk.dir=/path/to/Android/sdk` for the Android build.

### Run

- Android: `cd app && ./gradlew :androidApp:assembleDebug` (APK in `app/androidApp/build/outputs/apk/debug/`) or `./gradlew :androidApp:installDebug` with a device attached.
- Web: `cd app && ./gradlew :composeApp:wasmJsBrowserDevelopmentRun`

### Tests

`cd app && ./gradlew jvmTest`

### Checklist CSV format

Columns `What, Where, Frequency, Duration, Assignee` with an optional header; comma, semicolon or tab delimiters; several assignees separated by `;`; `Everyone` / `Unassigned` (or their German and French equivalents) for all or nobody. Use "Download template" in the checklist screen for an example, see `ChecklistCsv.TEMPLATE`.

### License

Licensed under the Apache License 2.0 (Apache-2.0), see [LICENSE](LICENSE).
