# CareCue

> “Small things remembered. People better cared for.”

**CareCue** is a conversational household assistant designed for the **Amazon Developer — Build, Ship, Shape Hackathon** (Alexa+ simulated experience track).

CareCue transforms everyday spoken or typed conversational requests into validated, persistent, reviewable care cues, reminders, and household checklists.

---

## The Problem
Everyday family care, household maintenance, check-ins, medical refills, and groceries are scattered across messages, post-it notes, mental to-do lists, and fleeting verbal requests. Crucial commitments slip through the cracks.

## The Solution
CareCue provides a caring, conversational interface simulated in the style of Alexa+:
- Interprets compound conversational requests:
  > *“Remind me to call Dad at 7 PM tomorrow and add milk and bread to tomorrow's shopping list.”*
- Extracts structured actions with deterministic validation.
- Presents a compact **Action Review Card** requesting explicit user confirmation before touching the database.
- Truthfully verifies and persists the data using local SQLite Room persistence.
- Derive statuses (e.g. Overdue) dynamically rather than storing fragile static flags.

---

## Core Engineering Principles
1. **Deterministic Action Execution**: AI may interpret natural language requests, but deterministic application logic validates, confirms, and executes all database mutations.
2. **Zero Hardcoded Secrets**: No API keys are embedded into source code or committed files. All keys are configured exclusively at runtime via secure input or `.env`.
3. **Multi-Provider Flexibility**: Users can choose between CareCue's built-in offline Smart Engine (zero latency, offline support) or external LLM providers (Gemini, OpenAI, Anthropic).
4. **Honest Notifications**: In-app due-time alerts and Android system notifications (via NotificationManager and AlarmManager) with graceful permission handling and test-trigger controls.

---

## Architecture

```
com.example
├── MainActivity.kt                      # Adaptive UI (Bottom Navigation on mobile, Navigation Rail on tablets)
├── carecue
│   ├── data
│   │   ├── local                        # Room Database, DAOs, Entities, JSON TypeConverters
│   │   ├── model                        # TaskItem, ChecklistItem, ProposedAction, ChatMessage
│   │   └── repository                   # TaskRepository & SettingsRepository
│   ├── engine
│   │   └── CareCueSmartEngine.kt        # Deterministic NLP parser, compound intent splitter, TimeZone math
│   ├── notifications
│   │   └── CareCueNotificationHelper.kt # NotificationChannel & permission-aware reminder alerts
│   └── ui
│       ├── assistant                    # Primary Alexa+ simulated conversation & voice input
│       ├── today                        # Due today view with dynamic overdue warning banners
│       ├── schedule                     # Date-bucketed schedule (Today, Tomorrow, Upcoming)
│       ├── lists                        # Shopping & household checklists
│       ├── settings                     # Timezone, notification test, runtime API keys, demo reset
│       ├── components                   # ActionReviewCard, TaskCard, WaveformAnimation, EditTaskDialog
│       └── viewmodel                    # CareCueViewModel (MVVM StateFlow architecture)
└── ui.theme                             # Material 3 Dark Charcoal + Warm Orange Palette
```

---

## Hackathon Demo Walkthrough
1. **Open Assistant**: See the friendly welcome cue and animated speech waveform.
2. **Type or Speak**:
   `Remind me to call Dad at 7 PM tomorrow and add milk and bread to tomorrow's shopping list`
3. **Action Review**: CareCue presents an Action Review Card with two extracted actions:
   - Call Dad (Tomorrow at 7:00 PM)
   - Add milk and bread to Shopping List
4. **Confirm & Save**: Tap **Confirm & Save**. Verified success banner confirms persistence.
5. **Inspect Today & Schedule**:
   - Navigate to **Schedule** to see “Call Dad” in Tomorrow's bucket.
   - Navigate to **Lists** to see “milk” and “bread” in the Household Shopping List.
6. **Test Notifications**: Go to **Settings** and tap **Trigger Test Notification Now** to verify real system notification delivery.
7. **Reset Demo Data**: Tap **Reset Demo Tasks** at any time to restore the clean judge demo state.

---

## License
MIT License. See [LICENSE](LICENSE) for details.
