# PiggyBag

PiggyBag is a powerful, offline-first personal expense tracking and money management Android app with cloud-sync capabilities.

***

## Features

* **Dashboard & Financial Overview:** Get a visual summary of your daily/monthly financial health.
* **Income & Expense Tracking:** Log and organize daily transactions with custom categories.
* **Multi-Wallet/Account Management:** Track balances across multiple bank accounts and cash wallets.
* **Monthly Budgets:** Define category-specific budget limits and monitor spending.
* **Partner Journal Sharing:** Link accounts with a partner (using invitation codes or usernames) for real-time secure transaction synchronization.
* **Group & Trip Expenses:** Create group expense journals with multi-party splits (supporting both registered cloud users and guest participants).
* **Robust Offline Sync:** An offline-first experience utilizing Room Database for local storage, syncing mutations automatically with the companion cloud backend.
* **Modern Material 3 UI:** A responsive, highly polished native Android interface built entirely in Jetpack Compose with dark mode support.
* **Visual Analytics:** Breakdown spending patterns with insightful charts and export statement summaries.

***

## Backend Companion integration

PiggyBag syncs with a secure PostgreSQL cloud backend. 

### Sync Architecture
* Local database mutations are queued and batch-processed over `/sync`.
* Uses logical timestamp-based conflict resolution (last-updated-wins).
* Supports real-time balance propagation across connected partner journals.

***

## Credits & Attribution
This project is based on the original open-source repository [Expenso](https://github.com/darkvortex144/Expenso.git) by [darkvortex144](https://github.com/darkvortex144). Special thanks to the original author for creating this excellent personal finance tracking application.
