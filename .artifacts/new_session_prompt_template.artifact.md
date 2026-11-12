# Prompt Template / Instruction Ticket for New Session (Cash 5 App)
## Copy and paste this exact prompt into your new Cash 5 session to ensure 100% architectural parity with Texas Smart Picks.

---

```text
We are building "Texas Cash 5 Smart Picks", an Android Java application that must be architecturally identical in UI, design system, and core statistical engine to our existing "Texas Smart Picks" (Lotto Texas) app, but adapted specifically for Texas Cash 5 rules.

Please review the following requirements and structure:

1. ARCHITECTURE & UI (Direct Copy from Lotto Texas template):
- Single-Activity architecture (`MainActivity.java`) written in pure Java using native Android Views (LinearLayout, ScrollView, TextView, AlertDialog).
- Cyberpunk Dark Theme: Background `#0F172A`, Ink/Text `#F8FAFC`, Cyan borders `#0EA5E9`, Gold `#F59E0B` gradients, Mint Green hit balls (`#A7F3D0` background, `#10B981` emerald stroke, `#064E3B` text).
- Reusable UI helper methods: `text()`, `card()`, `roundBorder()`, `round()`, `divider()`, `margin()`, `dp()`.

2. GAME RULES (Texas Cash 5):
- Number Pool: Select 5 numbers out of 35 (Numbers 1 to 35). Matrix pool size = 35.
- Draw Schedule: Daily (Monday through Saturday).
- Data Source: Remote CSV file hosted at `https://data.quickhitsapps.com/cash5.csv` (backed by GitHub Actions scraping).

3. CORE FEATURES & ENGINE:
- Time Periods: Monthly, Quarterly, and 4-Week analytical tabs.
- Statistical Tiers: Era-adjusted frequency categorization (Top Tier, Next Tier, Frequent, Lower Frequency).
- Smart Pick Generator: Balanced Blend, Overdue Top-Tier, and Weighted Odds strategies.
- Custom Number Locking: Allow users to lock specific favorite numbers while generating picks.
- Saved Favorites: Max 3 saved sets for Free users; unlimited saved sets for Pro users ($9.99 lifetime unlock).
- Hit History & Search: Tap any number (1–35) to view historical hit frequency and year-over-year search results (current year free, past years require Pro).
- Monetization: Persistent cyberpunk Ad Banner at the top, Rewarded Video Ad simulation for live data sync, and Pro Upgrade dialog ($9.99 lifetime).

Please inspect our project structure, set up the MainActivity.java matching these exact specifications, and prepare the Cash 5 CSV data parser!
```
