# PiggyBag Project Encyclopedia & Blueprint

## 1. Project Overview & Philosophy
PiggyBag is a high-performance, single-activity Android application designed to provide a comprehensive financial management suite. It prioritizes user privacy (local-first approach), extensive personalization (robust theme engine), and collaborative features (shared journals and group splitting).

## 2. Comprehensive Tech Stack
- **Language:** Kotlin 1.9+
- **UI Framework:** Jetpack Compose (Material 3)
- **Navigation:** Single Activity Architecture with a custom `activeFormStack` for overlays.
- **Persistence:** Room Database with SQL integration.
- **Network:** Retrofit 2 + OkHttp for Cloud API interaction.
- **Background Operations:** WorkManager for reliable background synchronization.
- **Security:** androidx.biometric for secure access, SHA-256 with salt for PIN hashing.
- **Utilities:** PDF Document API for statements, CSV BufferedWriter for data portability.

## 3. UI/UX Architecture & Personalization

### A. The Robust Appearance System (View Modes)
PiggyBag supports distinct **View Modes** that go beyond simple color swaps, completely altering the app's visual identity, spatial logic, and emotional resonance.

#### 1. Classic Edition (Default)
- **Philosophy:** Efficiency, speed, and modern standard compliance.
- **Design Language:** Material Design 3 (M3).
- **Typography:** IBM Plex Sans (Primary), Monospace (Numbers).
- **Visuals:** High-radius corners (24dp cards), Level 1-3 tonal elevation, clean vector charts.
- **Interaction:** Standard Material transitions, active-pill navigation indicators.

#### 2. Diary Edition (Premium)
- **Philosophy:** Tactile, personal, and mindful. Turns the app into a "Personal Heirloom."
- **Visual Experience:** 
    - **Backgrounds:** Cream-colored parchment (#FDF5E6) with fiber textures and "book margin" shadows.
    - **Scaffold:** Dark "leather" or wood textures surrounding the "paper" area.
    - **Accents:** Washi-tape headers, paper-clip indicators, and bookmark ribbons.
- **Typography:** 
    - **Titles:** Shadows Into Light (Handwriting).
    - **Body:** Cormorant Garamond (Vintage Serif).
    - **Numbers:** Special Elite (Typewriter/Stamped).
- **Components:**
    - **Cards:** Ruled ledger rows, sticky note overlays, and torn-paper edges.
    - **Buttons:** Rubber ink stamps and ribbon tabs.
    - **Navigation:** Page dividers/tabs protruding from the notebook edge.
- **Animations:** 3D page-flip transitions, ink-bleed effects upon saving, and "rubber stamp" thud visuals.

### B. Typography Suite
... (rest of the existing typography section)
Users can choose from multiple font families:
- **Sans-Serif:** IBM Plex Sans (Default), Roboto, Google Sans.
- **Display/Decorative:** Yusei Magic, Smooch Sans, Shadows Into Light (Handwritten), Cormorant, Amatic SC.
- **System:** Standard Serif and Monospace.

### C. Layout & Navigation
- **Home Tab:** Recent records with dynamic date filtering (Monthly, Weekly, All Time).
- **Analytics Tab:** Graphical insights into spending habits.
- **Budgets Tab:** Progress bars showing budget utilization.
- **Accounts Tab:** List of all financial sources with running balances.
- **More Tab:** Access to security, cloud sync, groups, and advanced settings.
- **Overlay System:** A `MutableStateList` (stack) handles nested screens (e.g., Adding Category from within the Add Transaction form), ensuring complex navigation flows are smooth and back-pressable.

## 4. Database Schema & Data Relationships

### Core Financial Tables
- **`accounts`**: `id`, `name`, `type`, `openingBalance`, `currentBalance`, `icon`, `color`, `userId`.
- **`categories`**: `id`, `name`, `type` (income/expense), `icon`, `color`, `orderIndex`.
- **`transactions`**: `id`, `amount`, `type`, `categoryId`, `accountId`, `note`, `transactionDate`, `tags`, `attachmentPath`, `userId`.
    - *Links*: Foreign keys to `accounts` and `categories`.
- **`budgets`**: `id`, `categoryId` (null = overall), `budgetAmount`, `month`, `year`, `userId`.
- **`reminders`**: `id`, `title`, `type` (Medicine, Bills, etc.), `dueDate`, `amount`, `recurrence`, `enabled`.
- **`autopays`**: `id`, `name`, `categoryId`, `amount`, `accountId`, `frequency`, `startDate`, `nextExecutionDate`, `status`.
    - *Links*: Foreign keys to `accounts` and `categories` for automatic transaction generation.

### Multi-User & Group Tables
- **`local_user_profiles`**: Local identity (`id`, `name`, `email`, `partnerShareCode`).
- **`groups`**: `id`, `title`, `groupPin`, `createdBy`.
- **`group_members`**: `id`, `groupId`, `userId`, `displayName`.
- **`group_expenses`**: `id`, `groupId`, `userId`, `amount`, `description`.
- **`debt_records`**: Detailed debt entries linked to `local_user_profiles`.

### Cloud & Sync System
- **`cloud_journals`**: Syncable personal ledger entries.
- **`sync_queue`**: Tracks `entityId`, `operation` (INSERT/UPDATE/DELETE), and `status` (PENDING/FAILED).
- **`shared_journals`**: Multi-user shared ledgers with `joinToken` and `role`.

## 5. Detailed Working Flows

### I. Transaction Life Cycle
1. **Input:** User enters amount, selects category/account, and adds notes/tags.
2. **Persistence:** `TitanBagViewModel` calls `repository.insertTransaction`.
3. **Budget Evaluation:** `checkBudgetLimits` calculates current month's spending for the category. If `limitExceeded`, it triggers a `NotificationEvent`.
4. **Cloud Hook:** If the user is logged in, a sync item is queued for the cloud journal.

### II. SMS Transaction Extraction
1. **Trigger:** `SmsReceiver` intercepts `SMS_RECEIVED`.
2. **Parsing:** `TransactionExtractor` applies Regex to identify Bank Name, Last 4 digits, Amount, and Type (Debit/Credit).
3. **Update:** `FinanceRepository` updates the matching `BankAccount` balance or creates a new one.
4. **Feedback:** A system notification alert appears with the extracted details.

### III. Cloud Synchronization Flow
1. **Detection:** `SyncWorker` runs periodically or on-demand.
2. **Pull/Push:**
    - Local changes in `sync_queue` are pushed to `api.syncJournals`.
    - Remote changes are fetched.
3. **Conflict Resolution:** Newest `updatedAt` wins. If a local entry was deleted remotely, it is removed locally unless the local version is newer.
4. **Success/Retry:** On success, the queue is cleared. On failure, `retryCount` increments and status becomes `FAILED`.

### IV. Group Expense Split Logic
1. **Expense Entry:** A member adds an expense.
2. **Calculation:** `calculateGroupSettlements` runs:
    - `Share = TotalSpent / MemberCount`.
    - `Net = Spent - Share`.
3. **Settlement Pairing:** Members are sorted into "Debtors" (Net < 0) and "Creditors" (Net > 0). The algorithm pairs the largest debtor with the largest creditor, reducing the debt until balances reach zero, minimizing the total number of transactions.

### V. Partner Connection & Share Code Validation
1. **Local Mode Validation:**
    - The app searches the `local_user_profiles` table for a matching `partnerShareCode`.
    - **Logic Rule:** Connection is denied if:
        - The code matches the current user's own code.
        - The code does not exist in the local database.
        - A connection already exists between the two IDs.
2. **Cloud Mode Validation:**
    - The app sends the code to the Cloud API.
    - The **Cloud Server** validates the code against the central `cloud_users` (remote users table).
    - Upon successful server-side validation, the app updates the local `cloud_partners` and `local_partner_connections` tables.

### VI. Recurring Transaction Scheduler
1. **Frequency Logic:** Supports Daily, Weekly, Monthly, Yearly.
2. **End Conditions:** "Never", "Until Date", or "After X occurrences".
3. **Execution:** On app launch, `processRecurringTransactions` compares `nextExecutionDate` with current time. If due, it creates a `Transaction` and computes the next date.

### VII. Debt Reminder System
1. **Monitoring:** A background coroutine in `TitanBagViewModel` polls every 10 seconds.
2. **Trigger:** If a "Pending" debt has `remainderBoolean = true` and the `dateTimestamp` is <= current time, a notification is fired and the reminder is disabled to prevent duplicates.

### VIII. UI Navigation & Animation Logic
1. **Form Stack:** Uses `MutableStateList<String>` to allow multi-level overlays (e.g., Record -> Edit -> Add Category).
2. **Animations:** 
    - **Tabs:** Horizontal slide with 1/12 width offset and scale effect (0.98f).
    - **Forms:** Scale-in from bottom-right (0.85f, 0.9f) for transactions, or horizontal slide-in for settings.
3. **Tab Persistence:** Bottom navigation state is preserved across form overlays.

### IX. Advanced Authentication Flow
1. **Manual Registration:** User provides Username, Email, Password, and Display Name.
2. **Google Sign-Up/Login:**
    - App authenticates via Google.
    - Server checks if the user exists.
    - **New Google Account (`isNew == true`):** The app must show a "Create Password" field. The user can either set a password now or skip/update it later (though "Create Password" is the UI label).
    - **Existing Google Account:** If the account is found (re-install or other device), the user is logged in automatically. A "Welcome [Name]" message is displayed.
3. **Password Management:** If a Google user skips password creation, they can update/set it later through settings.
4. **Local Migration:** Upon any successful cloud authentication, the app automatically migrates all local data (Transactions, Accounts, Budgets) from the "default_user" or previous local profile to the new Cloud `userId`.

### X. Partner Share Code Generation & Validation
1. **Source of Truth:** Partner share codes are **not generated randomly by the app**.
2. **Generation:** They are auto-generated by the server database upon the creation of a new entry in the `users` table. The format is predefined (e.g., `XXX-XXXX-XXXX-XXXX-XXXX`).
3. **Retrieval:** The code is only visible to the user because it is fetched from the server's `users` table after authentication.
4. **Bug & Error Handling:** 
    - If a connection attempt fails, the server may return a raw JSON error like `{"message":"invalid share code. partner not found"}`.
    - **UI Flow:** The app must parse this JSON and display the `message` field clearly to the user instead of the raw string.
    - **Database Sync Issue:** If a user sees their share code but cannot be found in the local `cloud_users` table, it indicates a failure in the local persistence (`handleAuthSuccess`) or a delay in the `fetchProfile` call.

### XI. Smart Reminders vs. AutoPay
1. **Smart Reminders:**
    - **Focus:** User awareness and notifications.
    - **Behavior:** Triggers a system alert based on a `dueDate`. It tracks "Due Soon" (within 3 days) and "Overdue" statuses.
    - **Transaction Impact:** **None.** It does not modify the ledger or account balances. The user must manually record any payment.
    - **Use Case:** Medicine schedules, service appointments, or manual bill payments.
2. **AutoPay:**
    - **Focus:** Automated execution and ledger accuracy.
    - **Behavior:** On the `nextExecutionDate`, the system checks the linked `Account` balance.
    - **Transaction Impact:** **Automatic.** If funds are sufficient, it inserts a new `Transaction` and subtracts the `amount` from the `Account` balance.
    - **Use Case:** Recurring utility bills, SIPs, or rent payments where automatic tracking is desired.

## 6. System Robustness & Error Flows

### A. Network & Sync Errors
Managed via `SyncError` sealed class:
- **Authentication:** If 401/403 is received, app prompts for re-login via `Authentication` error.
- **Network Failure:** `Network` error triggers a "Check Internet" snackbar; `SyncWorker` schedules a retry.
- **Server Error:** 5xx responses trigger `Server` error, halting sync to prevent data corruption.
- **API Response Parsing:** All cloud API failures must be parsed for the `"message"` key to provide user-friendly feedback.

### B. Validation & Database Safety
- **Reference Integrity:** Categories and Accounts cannot be deleted if they have associated transactions (managed via Room `ForeignKey.RESTRICT`).
- **File Processing:** While exporting/importing (CSV/PDF), an overlay blocks UI interaction (`isProcessingFile`) to prevent concurrent modifications.

## 7. Security & Privacy Flow
- **Session Management:** `SessionManager` stores `authToken` securely.
- **Auto-Locking:** The app monitors `onUserLeaveHint`. If enabled, it triggers `lockApp()`, forcing a `SplashAndLockScreen` overlay upon return.
- **Biometrics:** Integrates with `BiometricPrompt` to provide seamless unlocking using Fingerprint/Face ID.
- **PIN Hashing:** Uses `SHA-256` with a hardcoded static salt for storage in `SharedPreferences`.

## 8. Data Life Cycle & Portability
- **CSV Import Logic:** 
    - Maps columns based on headers.
    - **Auto-Provisioning:** If a Category or Account name from the CSV doesn't exist, the app automatically creates it using default colors/icons.
- **PDF Generation Logic:**
    - Uses `PdfDocument` with a page width of 595 (A4).
    - Supports dynamic column weight (Notes get 1.8x weight, IDs get 0.7x).
    - **Summary Logic:** Calculates Income/Expense/Savings for the specific filter range (Today, This Month, Custom, etc.) and displays them in a "Summary Box" on page 1.
- **Search History:** Persists the last 10 unique trimmed queries in `SharedPreferences`.
