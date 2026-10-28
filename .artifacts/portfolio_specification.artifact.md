# Quick Hits Apps — Master Portfolio & Technical Specification
## Umbrella Strategy, Monetization, and Architecture

---

## 1. Brand & Domain Architecture
* **Umbrella Brand**: **Quick Hits Apps**
* **Primary Domain**: `quickhitsapps.com`
* **Data Infrastructure Subdomain**: `data.quickhitsapps.com`
* **File Endpoints (Serverless via GitHub / Cloudflare R2)**:
  * Lotto Texas: `data.quickhitsapps.com/texas.csv`
  * Mega Millions: `data.quickhitsapps.com/megamillions.csv` (Future)
  * Powerball: `data.quickhitsapps.com/powerball.csv` (Future)

---

## 2. App Editions & Monetization

### Edition A: Free Version (Ad-Supported)
* **Monetization**:
  * Google AdMob Banner Ads (bottom of screens).
  * **Rewarded Video Ads for Syncing**: App ships with the **full historical database bundled statically**. Free users can watch a short 30-second rewarded video ad to sync the newest/latest live draw results from `data.quickhitsapps.com`.
* **Functional Limits**:
  * Saved favorites limited to a maximum of 3 custom sets.

### Edition B: Pro Version (Paid / Premium)
* **Monetization**:
  * **$9.99 One-Time Lifetime Purchase** via Google Play Billing.
* **Perks**:
  * **100% Ad-Free**: Clean, uninterrupted analytical user experience.
  * **Unlimited Favorites**: Save and inspect unlimited number sets.
  * **Instant Automatic Sync**: Background syncing directly from `data.quickhitsapps.com` without watching videos.

---

## 3. Serverless Data Pipeline
* **Update Schedule**: Automated GitHub Actions scripts running every Monday, Wednesday, and Friday.
* **Data Flow**:
  1. Script fetches official draw results.
  2. Updates repository CSV.
  3. Publishes instantly to `data.quickhitsapps.com`.
  4. App fetches CSV securely with client-side atomic fallback handling to prevent crashes during updates.

---

## 4. Technical Implementation Roadmap

### Step 1: Domain & Hosting Setup
* Register `quickhitsapps.com` (via Cloudflare Registrar for wholesale pricing).
* Configure DNS and map `data.quickhitsapps.com` to raw CSV endpoints.

### Step 2: Gradle Product Flavors (Codebase Split)
* Configure `free` and `paid` product flavors in `build.gradle`:
  * `free`: Includes AdMob SDK & Rewarded Video sync logic + 3 favorites limit.
  * `paid`: Excludes ads, unlocks unlimited favorites & instant background sync.

### Step 3: Google Play Billing & AdMob Integration
* Integrate Google Play Billing Library for the Pro upgrade flow ($9.99 lifetime unlock).
* Integrate Google AdMob SDK for banners and rewarded video syncs.

---
> [!NOTE]
> **Updated Specification**: Portfolio blueprint updated. Free users get **full bundled historical data** out-of-the-box, watching rewarded videos only to sync the **newest live draws**. Free favorites limit is 3, and Pro lifetime unlock is $9.99.
