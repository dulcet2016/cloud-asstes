# Event Asset Tracker — Kotlin / Jetpack Compose Edition

A 100% offline Android app for tracking event-decoration assets with QR codes — full native
rewrite using:

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (light theme only, no dark mode)
- **Database:** Room (SQLite) — fully offline, all data stays on the phone
- **QR generation:** ZXing (pure encoder, bundled — no network)
- **QR scanning:** CameraX + ML Kit Barcode Scanning (on-device bundled model — works fully
  offline, no Play Services / internet download needed)
- **PDF labels:** native `android.graphics.pdf.PdfDocument`, drawn directly with Canvas
- **Printing:** native Android Print framework (`PrintManager` / `PrintDocumentAdapter`) — a real
  Print dialog, not just a PDF download
- **Sharing:** WhatsApp via `Intent`
- **Settings/session:** DataStore Preferences (keeps you logged in until you explicitly log out)

## How to build the APK (no Android Studio needed)

1. Create a new **empty** GitHub repository.
2. Upload everything in this zip to the repository root, keeping the folder structure exactly as
   it is (`.github`, `app`, and the `.kts`/`.properties` files all belong at the root).
3. Open the repo's **Actions** tab — the "Build Android APK" workflow runs automatically.
4. When it finishes, open the run and download **Event-Asset-Tracker-APK** from Artifacts — it
   contains `app-debug.apk`.
5. Copy the APK to your phone and install it (allow "install from unknown sources" once).

Everything the build needs is either bundled in this zip or fetched automatically and normally
by Gradle from Google's/Maven's official repositories during the GitHub Actions run — the same
way every Android app in the world is built. Nothing needs to be pre-downloaded or pasted in
manually.

**First-time login:** Admin / `admin123` (change the password from Staff & Admin → Edit once
you're in).

## What's implemented

- **Login** — Admin (Login ID + password, multiple Admin accounts supported) or Staff (name +
  mobile). Once logged in, the app **never asks again** until you explicitly log out (session is
  saved via DataStore).
- **Home screen** — deliberately just two big buttons, **OUT** and **RETURN** — nothing else, so
  it's usable by anyone with zero training. Everything else lives behind the ⋮ menu in the top
  bar: Events, Assets, Asset QR Register, Staff & Admin, Missing Items Check, All Data Report,
  Settings, Logout.
- **Scanning — Camera and Gallery** — CameraX + ML Kit reads the QR live; there's also a
  "🖼️ Scan QR from Gallery" button that decodes a QR from any picked photo (useful if the label
  photo was sent to you rather than scanned in person). A sharp double-beep + vibration confirms
  a successful scan; a lower buzz + longer vibration flags a duplicate/invalid scan; rapid repeat
  reads of the same code within 2 seconds are ignored so one physical scan can't double-count.
- **Duplicate OUT and wrong RETURN protection** — an asset already OUT can't be scanned OUT again
  (and vice versa for RETURN), and returning an asset under a *different* event than the one it
  was actually sent out for is blocked with a clear message instead of silently mis-filing it.
- **The Event field on the scan screen is a combo box**: pick an existing event from the dropdown,
  **or type a brand new name and it's created automatically** — this is what makes OUT scanning
  "create the event" the first time, while RETURN can just pick that same event from the list.
- **QR Label Generator** *(admin-only — the one function that's restricted, as requested)* —
  type an item name + category + size + copy count, get sequential IDs (e.g. `COB001`,
  `COB002`, …) with Asset ID + Item Name + Category encoded in the QR itself, then Print (opens
  Android's real Print dialog) or Download an A4 PDF sheet or share the PDF straight to WhatsApp.
- **Asset QR Register — By Item and By Category** — every batch ever generated stays listed
  permanently (survives app restarts). Switch between "By Item" (one item name at a time, same as
  before) and "By Category" (print/download an entire category's labels — e.g. every "Lighting"
  label — in one PDF regardless of item name). Both views have Print / Download / per-label
  Reprint / delete-unassigned. Open to everyone.
- **Camera QR scanning needs nothing outside the app itself** — no server, no HTTPS, no Netlify,
  no internet connection of any kind. This is a real native Android app (CameraX + ML Kit's
  on-device bundled barcode model), not a website wrapped in a browser — camera access in a native
  app was never subject to the "must be HTTPS" rule that web browsers enforce for `getUserMedia`.
  The manifest doesn't even declare the `INTERNET` permission, so the app is physically incapable
  of making a network call even if something tried to.
- **Events** — create/edit/delete, newest first / oldest at the bottom. Tapping an event shows its
  OUT and RETURN entry lists, each entry individually editable/deletable, plus one-tap WhatsApp
  share of that report.
- **Assets** — full list with a search box (name / Asset ID / category), edit/delete any entry.
- **Missing Items Check — two modes**:
  - *Checklist* (no paste needed): pick an event, tick each item the moment you physically find it
    (records a real RETURN), whatever's left unticked is your missing list, one tap from a
    WhatsApp share.
  - *Paste & Compare*: for cross-checking against text copied from WhatsApp (e.g. an older
    message, or one from someone not using this app) — paste the OUT and RETURN text, get the
    missing list the same way as before.
- **All Data Report** — one comprehensive text report (event counts, every event's OUT/RETURN
  totals, everything still OUT, staff/admin counts) ready to share on WhatsApp in one tap.
- **Settings — Scanner/Device Name** — a name you set once per phone (e.g. "Front Gate Scanner"),
  stored in DataStore, automatically included in every report so you know which device an entry
  came from.
- **Staff & Admin** — add/edit/delete both Staff and Admin accounts, open to everyone as
  requested. *(One judgment call worth flagging: I kept this open per your instruction, but since
  any staff member could technically remove the only Admin account this way, consider keeping at
  least one Admin around at all times — happy to lock this back down to Admin-only if you'd
  rather be safe than fully open.)*
- **Full edit/delete** everywhere: Events, Assets, individual OUT/RETURN entries, Staff, Admin.
- **Light theme only** — no dark mode anywhere in the app.
- **Fully offline** — Room/SQLite only, no server, no external API, no network dependency of any
  kind; ML Kit's barcode model is the bundled on-device version, not the Play-Services one, so
  scanning works with zero internet too.

## A note on how this was verified

I don't have Android Studio or the Android SDK in the environment I build in, so I can't run a
full Gradle/Compose build myself before handing this to you. What I *did* do: downloaded the
Kotlin compiler and ran it across all 42 source files together. It confirms every file is
syntactically valid Kotlin, and — more importantly — that every one of my own function and class
names is referenced consistently and correctly across files (the only "unresolved" symbols are
Android/Compose/Room/CameraX/ML Kit/ZXing classes themselves, which simply aren't available
without the real Android SDK, and will resolve normally the moment GitHub Actions builds it with
the real toolchain).

If the very first GitHub Actions run hits an error, it's most likely a small library-version
mismatch rather than a logic problem — paste me the red error text from the Actions log and I'll
fix it immediately.
