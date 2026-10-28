# Implementation Plan — Ad Placement & Freemium Gating

Implement visual ad banners, rewarded video sync simulation, and Pro upgrade flows in `MainActivity.java` based on the finalized feature matrix.

## User Review Required

> [!IMPORTANT]
> We will add a styled Ad Banner at the bottom of the main screen so you can preview exactly where and how ads appear, along with a Rewarded Video sync prompt and Pro upgrade toggle.

## Proposed Changes

### [app] [MODIFY] [MainActivity.java](file:///C:/Users/fjlov/Documents/Codex/2026-10-10/wha/outputs/LottoTexasAndroidStudioProject/app/src/main/java/com/codex/lottotexas/MainActivity.java)

- **Banner Ad View**: Add a persistent bottom banner view in `MainActivity` styled with cyberpunk colors (`#1E293B` background, cyan border, and text `"Ad Banner · Tap to Remove via Pro ($9.99)"`). Tapping it can trigger the Pro upgrade dialog.
- **Pro Mode Toggle**: Add a Pro status checker (`isProUser()`) backed by `SharedPreferences` so you can easily toggle between Free (with ads & 3 favorites limit) and Pro (ad-free & unlimited favorites).
- **Favorites Limit Gating**: Enforce the max 3 favorites limit for Free users, prompting them to upgrade to Pro when saving a 4th set.
- **Rewarded Video Sync Dialog**: When Free users tap to sync live draws, show a dialog simulating a 30-second rewarded video ad before unlocking fresh data.

## Verification Plan

### Automated Tests
- Build and assemble debug APK (`app:assembleDebug`).

### Manual Verification
- Deploy to Samsung device.
- Visually inspect the banner ad at the bottom of the screen.
- Test Free vs. Pro upgrade flow.
