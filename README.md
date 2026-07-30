# EduAI Notification Demo

Standalone Android app combining:

1. **Notification catalog** — manual test harness for every case in
   `NOTIFICATION_USE_CASES.md` (§1, §2, and §4 — 17 cases total).
2. **Live pipeline** — Room + AlarmManager scheduling from
   [Notification_EduAI](https://github.com/ANURAGMN/Notification_EduAI)
   (inactivity, streak milestones, custom reminders, analytics + optional Firestore sync).

Use the **Catalog** tab to tap a button and fire a notification with the exact icon,
text budget, and two-button pattern from §7. Use the **Live pipeline** tab to exercise
the scheduled notification flow (press Home and wait ~5/10 min for inactivity alerts,
stay 5+ seconds to build streaks, set daily reminders).

## Build & run

```powershell
cd C:\Users\anurag.mn\Desktop\Notification\notification-demo
.\gradlew.bat installDebug
```

Or open this folder in Android Studio and hit Run.

**Firebase (optional):** Firestore sync in the live pipeline requires a real
`app/google-services.json` from your Firebase project. A placeholder file is included
so the project builds; analytics still work locally in Room when Firestore is unavailable.

## Tabs

| Tab | Source | What it does |
|---|---|---|
| Catalog | This repo | Manual §1–§7 notification preview + frequency cap demo |
| Live pipeline | [Notification_EduAI](https://github.com/ANURAGMN/Notification_EduAI) | AlarmManager receivers, Room persistence, reminder picker |
