# Business Requirements Document (BRD)
## Texas Cash 5 Smart Picks — Companion App Blueprint

---

## 1. Executive Summary & Objective
**Texas Cash 5 Smart Picks** is a standalone Android mobile application built as part of the **Quick Hits Apps** portfolio. It replicates the robust analytical engine, cyberpunk UI, freemium monetization model, and serverless cloud data pipeline of *Texas Smart Picks (Lotto Texas)*, tailored specifically to the **Texas Cash 5** lottery game (5 numbers out of 35, drawn Monday through Saturday).

---

## 2. Game Rules & Data Matrix
* **Lottery Game**: Texas Cash 5
* **Number Pool**: Select 5 numbers from **1 to 35** (Matrix pool size = 35).
* **Draw Schedule**: Daily (Monday through Saturday, 6 days a week).
* **Data Source**: Official Texas Lottery CSV export (`cash5.csv`), updated automatically via scheduled GitHub Actions scripts and hosted on Cloudflare R2 (`data.quickhitsapps.com/cash5.csv`).

---

## 3. Core Features & Architecture (Reused from Portfolio Template)
* **Time Periods**: Monthly, Quarterly, and 4-Week analytical tabs.
* **Statistical Tiers**: Era-adjusted frequency categorization (Top Tier, Next Tier, Frequent, Lower Frequency) based on multi-year historical data (2002–Present).
* **Smart Pick Generator**: Advanced algorithms for Balanced Blend, Overdue Top-Tier, and Weighted Odds.
* **Custom Number Locking**: Ability for users to lock favorite numbers (e.g., birthdates/lucky numbers) while generating picks.
* **Hit History & Search**: Tap any number (1–35) to view historical hit frequency and year-over-year search results.

---

## 4. Monetization & Freemium Gating Model

### Free Tier (Ad-Supported)
* **Banner Ads**: Persistent cyberpunk-styled ad banner at the top of screens.
* **Live Data Syncing**: App ships with foundational historical data; free users watch **1 Rewarded Video Ad** to sync the newest live monthly draws.
* **Saved Favorites Limit**: Maximum of **3 saved favorite sets** stored locally.
* **Search History Limit**: Current year search history is free; past years browsing requires Pro.

### Pro Tier ($9.99 Lifetime Purchase)
* **100% Ad-Free**: Clean, uninterrupted analytical experience.
* **Unlimited Saved Favorites**: Store unlimited custom number sets.
* **Past Years Search History**: Full access to historical search browsing.
* **Instant Automatic Sync**: Background syncing directly from `quickhitsapps.com` without watching videos.

---

## 5. Technology Stack
* **Language**: Java / Android SDK (Native Android Views (`LinearLayout`, `ScrollView`, `TextView`)).
* **Design System**: Cyberpunk Dark Theme (`#0F172A` background, cyan `#0EA5E9` borders, gold `#F59E0B` gradients, mint green `#A7F3D0` hit balls).
* **Cloud Backend**: GitHub Actions + Cloudflare R2 (`data.quickhitsapps.com/cash5.csv`).
