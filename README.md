# Lotto Texas Trends — Android Studio project

This is a native Android app starter project for exploring Lotto Texas number history. It works offline and ships with the downloaded Texas Lottery results through October 7, 2026.

## Open in Android Studio

1. Extract the ZIP, if you are opening the packaged copy.
2. In Android Studio, choose **Open** and select the `LottoTexasAndroidStudioProject` folder (the folder containing `settings.gradle`).
3. Let Gradle sync. The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0, JDK 17, and Android SDK 37.
4. Select an Android phone or emulator and press **Run**.

## Included in this first version

- Offline historical analysis with month, quarter, and rolling four-week views.
- Frequency tiers, showing numbers that appeared in more period-years before lower tiers.
- Current/latest period numbers shown separately from the historical comparison.
- Manual draw entry with date, 1–54 range, unique-number, and duplicate-date checks.
- Local on-device storage for manually added drawings.
- A draw-history view and an explanation of the analysis.

The app does not fetch results from the web. The starter data is `app/src/main/assets/lottotexas.csv`, copied from the Texas Lottery's public download page:

https://www.texaslottery.com/export/sites/lottery/Games/Lotto_Texas/Winning_Numbers/download.html

The source file has different number-pool rules across eras. The app accounts for those eras when deciding whether a number was eligible to appear in a historical period. It excludes the current incomplete calendar month or quarter from the historical comparison, and shows its results in a separate current-period section.

Historical frequencies describe prior drawings; they do not predict future drawings or change the odds of a future drawing.
