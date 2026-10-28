# Business Requirements Document (BRD)
## Lotto Texas Trends — Dual-Edition Strategy (Free vs. Paid / Pro)

---

## 1. Executive Summary

Lotto Texas Trends is a high-performance Android application providing historical lottery analysis, pattern tracking, number generation, and favorites management. To maximize user reach and monetization potential, the application will be split into two distinct editions:
1. **Lotto Texas Trends (Free Edition)**: Ad-supported (`Google AdMob`), featuring core historical analysis, limited recent draw viewing, and standard number generation.
2. **Lotto Texas Trends Pro (Paid Edition)**: Ad-free, premium experience featuring full 30+ year history access, advanced due-number filters, full week-by-week draw inspection, and unlimited saved favorites.

---

## 2. Product Variants & Scope

### A. Lotto Texas Trends (Free Edition)
* **Monetization**: Banner ads at the bottom of screens, occasional interstitial ads between heavy actions (e.g., generating smart picks).
* **Limitations**:
  * History limited to the last 12 months (instead of full 30+ years).
  * Favorites restricted to a maximum of 3 saved sets (instead of unlimited).
  * Advanced Tier filtering & due-number deep dives locked behind a Pro upgrade prompt.

### B. Lotto Texas Trends Pro (Paid Edition)
* **Monetization**: Upfront purchase via Google Play Billing Library (or in-app purchase unlock).
* **Core Offerings**:
  * **100% Ad-Free**: Clean, uninterrupted cyberpunk UI experience.
  * **Full Historical Access**: Complete 30+ year Lotto Texas draw database.
  * **Unlimited Favorites**: Save and inspect unlimited custom number sets with week-by-week draw tracking.
  * **Advanced Analytics**: Full access to all Tiers (Top Tier, Next Tier, Frequent, Due patterns).

---

## 3. Feature Comparison Matrix

| Feature / Capability | Free Edition (Ad-Supported) | Pro Edition (Paid / Unlocked) |
| :--- | :--- | :--- |
| **Ads** | Yes (Banner & Interstitial) | **None (Ad-Free)** |
| **Historical Data** | Last 12 months only | **Full 30+ Years (1992–Present)** |
| **Saved Favorites** | Up to 3 sets max | **Unlimited saved sets** |
| **Smart Pick Generator** | Standard generator | **Advanced filters & locking** |
| **Favorites Draw Inspection** | Latest draw & current month | **Full week-by-week history inspection** |
| **Price** | Free ($0.00) | One-time purchase / Pro Upgrade |

---

## 4. Technical Architecture (Android Product Flavors)

To maintain a single, clean codebase while supporting two separate app variants, Gradle **Product Flavors** will be implemented:

```groovy
android {
    flavorDimensions "edition"
    productFlavors {
        free {
            dimension "edition"
            applicationIdSuffix ".free"
            versionNameSuffix "-free"
        }
        paid {
            dimension "edition"
            applicationIdSuffix ".pro"
            versionNameSuffix "-pro"
        }
    }
}
```

* **Shared Code (`src/main`)**: Core logic, UI components, data parsers, storage managers.
* **Flavor-Specific Code (`src/free` & `src/paid`)**: AdMob SDK initialization, Google Play Billing integration, feature gating flags.

---

## 5. Review & Next Steps

> [!IMPORTANT]
> **User Review Required**: Please review the feature breakdown and pricing model above. Once approved, we can proceed with setting up Android Product Flavors and integrating Google AdMob & Google Play Billing.

* **Open Questions**:
  1. Will the paid version be distributed as a **separate app listing** on Google Play, or as an **In-App Purchase (IAP)** unlock inside a single app?
  2. What is your target price point for the Pro edition?
