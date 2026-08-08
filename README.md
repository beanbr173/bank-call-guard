# Bank Call Guard

Android app that screens incoming calls before they ring and warns you when a caller may be impersonating Wells Fargo, Bank of America, First Citizens, USAA, Chase, Citibank, U.S. Bank, Golden 1, East West Bank, or other monitored banks.

## Browser preview (no build required)

Open [`preview/index.html`](preview/index.html) in Chrome or any browser to interact with the UI and simulate scam/caution warnings before building the APK.

## GitHub cloud build (recommended — no Android Studio on your PC)

This repo includes a GitHub Actions workflow that builds the APK in the cloud.

### Step-by-step

1. **Create a GitHub account** at [github.com](https://github.com) if you do not have one.

2. **Create a new repository**
   - Click **New repository**
   - Name it e.g. `bank-call-guard`
   - Leave it **Public** (required for free unlimited Actions minutes on public repos)
   - Do **not** add a README, `.gitignore`, or license (this project already has them)

3. **Upload the project** (browser-only option — no Git install required)
   - On the new repo page, click **Add file → Upload files**
   - Drag in the entire `bank_call_guard` folder contents (all files and subfolders)
   - Include `.github`, `app`, `gradle`, `preview`, `gradlew`, `gradlew.bat`, etc.
   - Click **Commit changes**

   **Alternative:** if you have Git installed locally:
   ```bat
   cd D:\working\play_store_apps\bank_call_guard
   git init
   git add .
   git commit -m "Initial Bank Call Guard release"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/bank-call-guard.git
   git push -u origin main
   ```

4. **Wait for the build**
   - Open your repo on GitHub
   - Click the **Actions** tab
   - Select the **Build APK** workflow run (starts automatically after upload/push)
   - Wait about **8–15 minutes** for the green checkmark

5. **Download the APK**

   **Share with friends (one link, always latest build):**

   https://github.com/beanbr173/bank-call-guard/releases/latest/download/bank-call-guard.apk

   Each successful push to `main` publishes a new release; that URL always downloads the newest APK.

   **For yourself (specific build from Actions):**
   - Open the completed workflow run
   - Scroll to **Artifacts**
   - Download **bank-call-guard-debug-apk**
   - Unzip if needed; the file inside is `app-debug.apk`

6. **Install on your phone**
   - Copy `app-debug.apk` to your phone (USB, email, Google Drive, etc.)
   - Enable **Install unknown apps** for the app you use to open the file
   - Install, then open **Bank Call Guard**
   - Tap **Enable call screening** and accept the system prompt

7. **After you change the app**
   - Edit files locally (or on GitHub’s website)
   - Upload/commit again → Actions rebuilds → the [latest release link](https://github.com/beanbr173/bank-call-guard/releases/latest/download/bank-call-guard.apk) serves the new APK → reinstall on phone
   - UI-only tweaks can be checked first in `preview/index.html` (instant, no rebuild)

### Manual build trigger

In GitHub: **Actions → Build APK → Run workflow** to rebuild without pushing new commits.

## What it does

- Uses Android **Call Screening** to inspect incoming calls before the phone rings
- Compares the caller number against a curated list of official bank numbers
- Uses carrier **STIR/SHAKEN** verification when available
- Shows a full-screen warning such as **"Scam posing as Wells Fargo"** for high-risk calls
- Plays a **custom alarm ringtone** (instead of the normal ring) for high-risk scam warnings
- Optional **silence or reject** for high-risk calls; alert history and notification fallback
- Custom numbers, manual bank-list refresh, and per-bank simulate warnings
- Lets you dismiss the warning overlay (the phone may still be ringing unless you chose reject)

## Important limitations

This app **cannot guarantee** scam detection.

- Scammers often **spoof caller ID**, so your phone may show a real bank number for a fraudulent call.
- When a known bank number fails STIR/SHAKEN verification, that is a strong spoofing signal — but not proof.
- When verification is unavailable, the app shows **caution**, not a definitive scam label.
- **If unsure, hang up and call the number on the back of your card.**

> This app is not affiliated with Wells Fargo, Bank of America, or any financial institution. It does not access your bank accounts. It analyzes incoming call metadata only. False positives and false negatives are possible.

## Requirements

- Android 10 (API 29) or higher
- Android 11+ recommended for STIR/SHAKEN verification status
- Physical device recommended for end-to-end call screening tests

## Build the APK locally (optional)

Use this only if you install Android Studio or JDK + Android SDK on your machine.

### Option A: Android Studio

1. Open the project folder in Android Studio
2. Let Gradle sync
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Option B: Gradle CLI

```bat
cd D:\working\play_store_apps\bank_call_guard
gradlew.bat assembleDebug
```

Release build (requires your own signing config):

```bat
gradlew.bat assembleRelease
```

## Install (sideload)

1. Copy `app-debug.apk` to your phone
2. Enable **Install unknown apps** for your file manager or browser
3. Install the APK
4. Open **Bank Call Guard**
5. Tap **Enable call screening** and accept the system role prompt
6. Optionally request battery optimization exemption on aggressive OEM skins
7. Use **Simulate scam call warning** to preview the overlay without a real call

### If install says "App not installed"

1. Delete old `bank-call-guard*.apk` files from Downloads — download **one** fresh copy from the [latest release link](https://github.com/beanbr173/bank-call-guard/releases/latest/download/bank-call-guard.apk).
2. **Settings → Apps → search "Bank Call Guard"** — if it appears, tap **Uninstall** (even if you think it is not installed).
3. **Google Play Protect:** when installing, if you see a scan warning, tap **Install anyway** / **More details → Install anyway**.
4. **Install unknown apps:** **Settings → Apps → [My Files or Chrome]** → allow installs from that app.
5. Confirm **Android 10+** (required — minSdk 29).
6. After install, scroll to the bottom of the app — you should see **Version 1.0.3** or newer.

## How detection works

| Condition | Risk | Message |
|-----------|------|---------|
| Known bank number + STIR/SHAKEN failed | High | Scam posing as [Bank] |
| Caller ID name matches bank but number is not official (unverified/failed) | High | Possible scam — caller ID says [Bank] but number is not official |
| Caller ID name matches bank but number is not official (STIR passed) | Caution | Unverified name match — number is not official |
| Known bank number + verification unavailable | Caution | Unverified caller claiming to be [Bank] |
| Known bank number + verification passed | None | No overlay |
| Other calls | None | Pass through |

## Project layout

```
bank_call_guard/
├── .github/workflows/build-apk.yml
├── preview/index.html
├── app/src/main/assets/banks.json
├── app/src/main/java/com/kreativesolutions/bankcallguard/
│   ├── BankCallScreeningService.kt
│   ├── IncomingCallOverlayActivity.kt
│   ├── MainActivity.kt
│   ├── data/
│   └── domain/
└── app/src/test/java/...
```

## Tests

```bat
gradlew.bat test
```

Unit tests cover phone normalization, scam detection rules, and bank catalog loading.

## Updating bank numbers

Edit `app/src/main/assets/banks.json` and rebuild. Each bank entry includes:

- `bankId`
- `displayName`
- `numbers` (E.164 format, e.g. `+18008693557`)
- `aliases` (lowercase strings matched against caller ID display names)

## Version

1.1.0

## Play Store track

Google Play prep (privacy policy, signed AAB, store checklist) lives in a separate repo so sideload builds are unchanged:

**https://github.com/beanbr173/bank-call-guard-play**
