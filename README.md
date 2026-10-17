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

### How to Run

#### Web

```
cd app && ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
Then open the printed URL (usually http://localhost:8080).

#### Android

Build the APK (`app/androidApp/build/outputs/apk/debug/`) or install it on an attached device:

```
cd app && ./gradlew :androidApp:assembleDebug
cd app && ./gradlew :androidApp:installDebug
```

After changing the Supabase values, rebuild so they are compiled in.

### SignUp & Login

Sign up a new user with any user name and password (at least 6 characters).
Then login with this user and password.

### Tests

`cd app && ./gradlew jvmTest`

### Checklist CSV format

Columns `What, Where, Frequency, Duration, Assignee` with an optional header; comma, semicolon or tab delimiters; several assignees separated by `;`; `Everyone` / `Unassigned` (or their German and French equivalents) for all or nobody. Use "Download template" in the checklist screen for an example, see `ChecklistCsv.TEMPLATE`.

"Export CSV" writes `What, Where, Frequency, Duration, Assignee, Repetition, Next Start, Pinned` (header always present, comma delimiter, UTF-8) and importing that file restores the rows. `Repetition` uses one English grammar regardless of the UI language, parsed case-insensitively and tolerant of spacing:

- `Every N day(s)`
- `Every N week(s), Sunday`
- `Every N month(s)` or `Every N year(s)` followed by `, 1st|2nd|3rd|4th|last Sunday` or `, day D`, and optionally `, month M`

Examples: `Every 1 year(s), 1st Sunday, month 4`, `Every 6 month(s), 1st Sunday`, `Every 1 week(s), Sunday`, `Every 3 day(s)`, `Every 1 year(s), day 15, month 8`. A non-empty Repetition replaces the proposal from Frequency; an unparseable one flags the row. `Next Start` is `yyyy-MM-dd HH:mm` (local time); a valid Next Start pins the task (it is kept when dates are recomputed, other tasks are packed around it) unless `Pinned` is `false`, in which case the date is ignored and recomputed. Editing the Next Start column in the table pins the task; the lock icon unpins it. "Assign all Tasks" only assigns tasks that are Unassigned.

### License

Licensed under the Apache License 2.0 (Apache-2.0), see [LICENSE](LICENSE).
