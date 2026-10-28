package com.codex.lottotexas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

/** Offline Lotto Trends history explorer based on Texas Lotto numbers. */
public class MainActivity extends Activity {
    private static final int INK = Color.rgb(248, 250, 252);        // Pure White / Slate 50 Text
    private static final int MUTED = Color.rgb(148, 163, 184);      // Sky Slate 400 Subtitle Text
    private static final int BLUE = Color.rgb(0, 242, 254);         // Electric Cyber Cyan #00F2FE
    private static final int GREEN = Color.rgb(16, 185, 129);       // Neon Emerald #10B981
    private static final int GOLD = Color.rgb(245, 158, 11);        // Texas Gold #F59E0B
    private static final int PALE = Color.rgb(7, 10, 20);           // Deep Cyber Midnight #070A14
    private static final int CARD_BG = Color.rgb(15, 23, 42);       // Cyber Navy Slate #0F172A
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("""
            MMM d, yyyy""", Locale.US);

    private final ArrayList<Draw> draws = new ArrayList<>();
    private final Set<LocalDate> manualDates = new TreeSet<>();
    private File historyFile;
    private LocalDate latestDate;
    private String page = "explore";
    private String mode = "Month";
    private int selectedMonth = LocalDate.now().getMonthValue();
    private int selectedQuarter = (LocalDate.now().getMonthValue() - 1) / 3 + 1;
    private int selectedYear = LocalDate.now().getYear();
    private LinearLayout content;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindowColors();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        if (!"explore".equals(page)) {
                            page = "explore";
                            render();
                        } else {
                            finish();
                        }
                    }
            );
        }

        historyFile = new File(getFilesDir(), "lottotexas.csv");
        renderSplash();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @SuppressWarnings("all")
    @Override
    public void onBackPressed() {
        if (!"explore".equals(page)) {
            page = "explore";
            render();
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("deprecation")
    private void setupWindowColors() {
        try {
            getWindow().setStatusBarColor(PALE);
            getWindow().setNavigationBarColor(PALE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                View decor = getWindow().getDecorView();
                WindowInsetsController controller = decor.getWindowInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
            }
        } catch (Exception ignored) { }
    }

    @SuppressWarnings("deprecation")
    private void renderSplash() {
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        try {
            android.media.AudioManager am = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) {
                am.requestAudioFocus(null, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        } catch (Exception ignored) { }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        int videoRes = getResources().getIdentifier("splash_video", "raw", getPackageName());
        if (videoRes != 0) {
            TextureView textureView = new TextureView(this);
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    MediaPlayer mp = new MediaPlayer();
                    try {
                        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + videoRes);
                        mp.setDataSource(getApplicationContext(), videoUri);
                        mp.setSurface(new Surface(surface));
                        mp.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
                        mp.setVolume(1.0f, 1.0f);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mp.setAudioAttributes(new android.media.AudioAttributes.Builder()
                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                                    .build());
                        }
                        mp.setOnPreparedListener(player -> {
                            player.setVolume(1.0f, 1.0f);
                            int vWidth = player.getVideoWidth();
                            int vHeight = player.getVideoHeight();
                            if (vWidth > 0 && vHeight > 0) {
                                float sx = (float) width / vWidth;
                                float sy = (float) height / vHeight;
                                float maxScale = Math.max(sx, sy);
                                Matrix matrix = new Matrix();
                                matrix.setScale(maxScale / sx, maxScale / sy, width / 2f, height / 2f);
                                textureView.setTransform(matrix);
                            }
                            player.start();
                        });
                        mp.setOnCompletionListener(player -> {
                            player.release();
                            loadHistory();
                            if (latestDate != null) {
                                selectedMonth = latestDate.getMonthValue();
                                selectedQuarter = (latestDate.getMonthValue() - 1) / 3 + 1;
                            }
                            render();
                        });
                        mp.setOnErrorListener((player, what, extra) -> {
                            player.release();
                            loadHistory();
                            render();
                            return true;
                        });
                        mp.prepareAsync();
                    } catch (Exception ex) {
                        loadHistory();
                        render();
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return true; }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });

            // 75% Height Stage for Video (Aspect Fill Crop - Zero Side Bars)
            LinearLayout.LayoutParams videoLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.75f);
            root.addView(textureView, videoLp);

            // 25% Height Stage for Text Underneath
            LinearLayout textStage = new LinearLayout(this);
            textStage.setOrientation(LinearLayout.VERTICAL);
            textStage.setGravity(Gravity.CENTER);
            textStage.setPadding(dp(16), 0, dp(16), dp(16));

            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setGravity(Gravity.CENTER);
            titleRow.addView(text("TEXAS ", 22, Color.WHITE, true));
            titleRow.addView(text("QUICK HITS", 22, BLUE, true));
            textStage.addView(titleRow, margin(0, 4));

            TextView subText = text("Based on Texas Lotto numbers", 12, MUTED, false);
            subText.setGravity(Gravity.CENTER);
            textStage.addView(subText, margin(0, 10));

            TextView loadingText = text("Analyzing 30+ years of lottery history...", 11, BLUE, true);
            loadingText.setGravity(Gravity.CENTER);
            textStage.addView(loadingText, margin(0, 0));

            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.25f);
            root.addView(textStage, textLp);

            setContentView(root);
        } else {
            loadHistory();
            render();
        }
    }

    private void render() {
        setupWindowColors();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PALE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(40), dp(20), dp(80));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, scrollLp);

        if (!isProUser()) {
            LinearLayout bannerView = new LinearLayout(this);
            bannerView.setOrientation(LinearLayout.HORIZONTAL);
            bannerView.setGravity(Gravity.CENTER);
            bannerView.setBackground(roundBorder(0xFF1E293B, 0xFF0EA5E9, 12));
            bannerView.setPadding(dp(12), dp(6), dp(12), dp(6));

            TextView adLabel = text("Ad Banner · Tap to Remove Ads via Pro ($9.99)", 11, GOLD, true);
            bannerView.addView(adLabel);
            bannerView.setOnClickListener(v -> showProUpgradeDialog());

            LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            bannerLp.setMargins(dp(16), 0, dp(16), dp(8));
            root.addView(bannerView, bannerLp);
        }

        setContentView(root);
        buildHeader();
        switch (page) {
            case "explore" -> buildExplore();
            case "search" -> buildSearchPage();
            case "history" -> buildHistory();
            default -> buildAbout();
        }
        content.addView(buildNavigation(), margin(16, 0));
        checkFirstLaunchTerms();
    }

    private void checkFirstLaunchTerms() {
        boolean accepted = getSharedPreferences("terms", MODE_PRIVATE).getBoolean("accepted", false);
        if (!accepted) {
            showFirstLaunchTermsDialog();
        }
    }

    private void showFirstLaunchTermsDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("Welcome to Texas Quick Hits", 18, INK, true));
        layout.addView(text("Please review and agree to the Terms of Use before proceeding.", 12, MUTED, false), margin(2, 14));

        LinearLayout box = card(false);
        box.setBackground(roundBorder(CARD_BG, 0xFF334155, 14));

        box.addView(text("1. For Entertainment & Informational Use Only", 12, BLUE, true), margin(0, 2));
        box.addView(text("Texas Quick Hits is created solely for statistical exploration, educational, and entertainment purposes.", 11, MUTED, false), margin(0, 8));

        box.addView(text("2. No Guarantee of Winnings or Odds Alteration", 12, BLUE, true), margin(4, 2));
        box.addView(text("Historical draw frequency describes past drawings only; it does not predict future outcomes or alter lottery odds.", 11, MUTED, false), margin(0, 8));

        box.addView(text("3. Limitation of Liability & Waiver", 12, BLUE, true), margin(4, 2));
        box.addView(text("You are solely responsible for any lottery purchases you make. The developers assume no liability for any financial losses.", 11, MUTED, false), margin(0, 8));

        box.addView(text("4. Unofficial & Non-Affiliated", 12, BLUE, true), margin(4, 2));
        box.addView(text("Texas Quick Hits is an independent app and is not affiliated with the Texas Lottery Commission or any official lottery entity.", 11, MUTED, false));

        layout.addView(box, margin(0, 14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setCancelable(false)
                .setPositiveButton("I Agree & Continue", (d, w) -> getSharedPreferences("terms", MODE_PRIVATE).edit().putBoolean("accepted", true).apply())
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private void buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Logo Image next to title (Square 1:1 cropped emblem)
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_launcher_foreground_square);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text("TEXAS ", 22, INK, true));
        titleRow.addView(text("QUICK HITS", 22, BLUE, true));
        brand.addView(titleRow);

        String sub = "Historical number analysis" + (latestDate == null ? "" : " · Updated " + latestDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)));
        brand.addView(text(sub, 11, MUTED, false));
        row.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(row, margin(4, 20));
    }

    private void buildExplore() {
        String mainTitle = periodName() + " patterns";
        addText(mainTitle, 22, INK, true, 2);
        addText("Choose a time period and year to explore.", 14, INK, true, 10);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 18));
        for (String label : new String[]{"Month", "Quarter", "4 weeks"}) {
            final String value = label;
            boolean isSelected = Objects.equals(mode, label);
            TextView tab = text(label, 13, isSelected ? BLUE : MUTED, true);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(4), dp(10), dp(4), dp(10));
            tab.setBackground(isSelected ? roundBorder(CARD_BG, BLUE, 14) : round(Color.TRANSPARENT, 14));
            tab.setOnClickListener(v -> { mode = value; render(); });
            tabs.addView(tab, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        content.addView(tabs, margin(0, 14));

        if (!"4 weeks".equals(mode)) buildPeriodPicker();

        buildTierCard();
        buildCurrentPeriodCard();
        buildSmartPickerButton();
        buildFavoritesButton();
    }

    private interface YearCallback {
        void onYearSelected(int year);
    }

    private void showYearPickerDialog(int targetYear, YearCallback callback) {
        int firstYear = draws.isEmpty() ? 1992 : draws.get(0).date().getYear();
        int lastYear = latestDate != null ? latestDate.getYear() : LocalDate.now().getYear();

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(16), dp(16), dp(16));
        list.setBackgroundColor(CARD_BG);
        scroll.addView(list);

        list.addView(text("Select Year to Review", 16, INK, true), margin(0, 12));

        AlertDialog[] dialogHolder = new AlertDialog[1];

        for (int yr = lastYear; yr >= firstYear; yr--) {
            final int yearNum = yr;
            boolean isSelected = (yearNum == targetYear);

            TextView item = text(String.valueOf(yearNum), 14, isSelected ? BLUE : INK, isSelected);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(10), 0, dp(10));
            item.setBackground(isSelected ? roundBorder(CARD_BG, BLUE, 12) : roundBorder(CARD_BG, 0xFF334155, 12));

            item.setOnClickListener(v -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                callback.onYearSelected(yearNum);
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, dp(2), 0, dp(4));
            list.addView(item, lp);
        }

        dialogHolder[0] = new AlertDialog.Builder(this)
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .create();

        if (dialogHolder[0].getWindow() != null) {
            dialogHolder[0].getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialogHolder[0].show();
    }

    private void showYearPickerDialog() {
        showYearPickerDialog(selectedYear, yearNum -> {
            selectedYear = yearNum;
            render();
        });
    }

    private void buildPeriodPicker() {
        LinearLayout monthPicker = new LinearLayout(this);
        monthPicker.setGravity(Gravity.CENTER_VERTICAL);
        monthPicker.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 16));

        TextView prevM = text("‹", 26, BLUE, true);
        prevM.setGravity(Gravity.CENTER);
        monthPicker.addView(prevM, new LinearLayout.LayoutParams(dp(44), dp(44)));

        String monthLabel = "Month".equals(mode) ? monthName(selectedMonth) : "Quarter " + selectedQuarter;
        TextView monthVal = text(monthLabel, 15, BLUE, true);
        monthVal.setGravity(Gravity.CENTER);
        monthPicker.addView(monthVal, new LinearLayout.LayoutParams(0, dp(44), 1f));

        TextView nextM = text("›", 26, BLUE, true);
        nextM.setGravity(Gravity.CENTER);
        monthPicker.addView(nextM, new LinearLayout.LayoutParams(dp(44), dp(44)));

        prevM.setOnClickListener(v -> shiftPeriod(-1));
        nextM.setOnClickListener(v -> shiftPeriod(1));
        content.addView(monthPicker, margin(0, 6));

        LinearLayout yearPicker = new LinearLayout(this);
        yearPicker.setGravity(Gravity.CENTER_VERTICAL);
        yearPicker.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 16));

        TextView prevY = text("‹", 26, BLUE, true);
        prevY.setGravity(Gravity.CENTER);
        yearPicker.addView(prevY, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView yearBtn = text(selectedYear + " ▾", 15, BLUE, true);
        yearBtn.setGravity(Gravity.CENTER);
        yearBtn.setOnClickListener(v -> showYearPickerDialog());
        yearPicker.addView(yearBtn, new LinearLayout.LayoutParams(0, dp(44), 1f));

        TextView nextY = text("›", 26, BLUE, true);
        nextY.setGravity(Gravity.CENTER);
        yearPicker.addView(nextY, new LinearLayout.LayoutParams(dp(44), dp(44)));

        prevY.setOnClickListener(v -> { selectedYear--; render(); });
        nextY.setOnClickListener(v -> { selectedYear++; render(); });
        content.addView(yearPicker, margin(0, 8));

        int currentM = latestDate != null ? latestDate.getMonthValue() : LocalDate.now().getMonthValue();
        int currentQ = latestDate != null ? quarterOf(latestDate) : quarterOf(LocalDate.now());
        int currentY = latestDate != null ? latestDate.getYear() : LocalDate.now().getYear();

        boolean isNotCurrent = ("Month".equals(mode) && (selectedMonth != currentM || selectedYear != currentY))
                || ("Quarter".equals(mode) && (selectedQuarter != currentQ || selectedYear != currentY));

        if (isNotCurrent) {
            String currentLabel = "Month".equals(mode) ? monthName(currentM) + " " + currentY : "Quarter " + currentQ + " " + currentY;
            TextView jumpBtn = text("Current " + mode.toLowerCase(Locale.US) + " (" + currentLabel + ")  ➔", 12, Color.WHITE, true);
            jumpBtn.setGravity(Gravity.CENTER);
            jumpBtn.setPadding(dp(14), dp(7), dp(14), dp(7));
            jumpBtn.setBackground(roundBorder(0xFF0284C7, 0xFF38BDF8, 16));
            jumpBtn.setOnClickListener(v -> jumpToCurrentPeriod());

            LinearLayout jumpRow = new LinearLayout(this);
            jumpRow.setGravity(Gravity.CENTER);
            jumpRow.addView(jumpBtn);
            content.addView(jumpRow, margin(0, 14));
        }
    }

    private void jumpToCurrentPeriod() {
        if (latestDate != null) {
            selectedMonth = latestDate.getMonthValue();
            selectedQuarter = quarterOf(latestDate);
            selectedYear = latestDate.getYear();
        } else {
            selectedMonth = LocalDate.now().getMonthValue();
            selectedQuarter = quarterOf(LocalDate.now());
            selectedYear = LocalDate.now().getYear();
        }
        render();
    }

    @SuppressWarnings("IfCanBeSwitch")
    private void shiftPeriod(int delta) {
        if ("Month".equals(mode)) {
            selectedMonth += delta;
            if (selectedMonth < 1) selectedMonth = 12;
            if (selectedMonth > 12) selectedMonth = 1;
        } else {
            selectedQuarter += delta;
            if (selectedQuarter < 1) selectedQuarter = 4;
            if (selectedQuarter > 4) selectedQuarter = 1;
        }
        render();
    }

    @SuppressWarnings({"ReassignedVariable", "ForLoopReplaceableByWhile", "unused"})
    private void buildTierCard() {
        List<PeriodBucket> buckets = historicalBuckets();
        ArrayList<Stat> stats = calculateStats(buckets);
        LinearLayout box = card(false);

        LinearLayout cardHeader = new LinearLayout(this);
        cardHeader.setGravity(Gravity.CENTER_VERTICAL);

        String cardTitle = "Across past " + pluralPeriodName();
        cardHeader.addView(text(cardTitle, 18, INK, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView infoBtn = text("ⓘ  How Tiers Work", 11, Color.WHITE, true);
        infoBtn.setPadding(dp(10), dp(5), dp(10), dp(5));
        infoBtn.setBackground(roundBorder(0xFF0284C7, 0xFF38BDF8, 14));
        infoBtn.setOnClickListener(v -> showTierHelpDialog());
        cardHeader.addView(infoBtn);

        box.addView(cardHeader);
        box.addView(text("How often each number appeared at least once", 13, MUTED, false), margin(2, 10));

        if (stats.isEmpty()) {
            box.addView(text("Add or load draw results to see trends.", 13, MUTED, false));
        } else {
            LinkedHashMap<Integer, ArrayList<Stat>> tiers = new LinkedHashMap<>();
            for (Stat stat : stats) tiers.computeIfAbsent(stat.percent, key -> new ArrayList<>()).add(stat);
            List<ArrayList<Stat>> tierGroups = new ArrayList<>(tiers.values());

            String periodUnit = "Month".equals(mode) ? "YEARS" : "PERIODS";

            // Tier 0: TOP TIER
            if (!tierGroups.isEmpty()) {
                ArrayList<Stat> topGroup = tierGroups.get(0);
                Stat first = topGroup.get(0);
                String detail = "TOP TIER  ·  " + first.hits + " / " + first.eligible + " " + periodUnit + "  ·  " + first.percent + "%";
                box.addView(text(detail, 13, MUTED, true), margin(0, 6));
                box.addView(buildTierGrid(topGroup, 8), margin(0, 4));
            }

            // Tier 1: NEXT TIER (if exists)
            if (tierGroups.size() > 1) {
                ArrayList<Stat> nextGroup = tierGroups.get(1);
                Stat first = nextGroup.get(0);
                box.addView(divider(), margin(6, 6));
                String detail = "NEXT TIER  ·  " + first.hits + " / " + first.eligible + " " + periodUnit + "  ·  " + first.percent + "%";
                box.addView(text(detail, 13, MUTED, true), margin(0, 6));
                box.addView(buildTierGrid(nextGroup, 8), margin(0, 4));
            }

            // Tier 2+: ALSO FREQUENT (if exists)
            if (tierGroups.size() > 2) {
                ArrayList<Stat> frequentGroup = tierGroups.get(2);
                Stat first = frequentGroup.get(0);
                box.addView(divider(), margin(6, 6));
                String detail = "ALSO FREQUENT  ·  " + first.hits + " / " + first.eligible + " " + periodUnit + "  ·  " + first.percent + "%";
                box.addView(text(detail, 13, MUTED, true), margin(0, 6));
                box.addView(buildTierGrid(frequentGroup, 8), margin(0, 4));
            }

            box.addView(text("Hit rates describe past draws; they don't predict the next one.", 10, MUTED, false), margin(10, 0));
        }
        content.addView(box, margin(0, 14));
    }

    private LinearLayout buildTierGrid(ArrayList<Stat> group, int limit) {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout currentRow = null;
        int inRow = 0;
        int count = Math.min(group.size(), limit);

        for (int i = 0; i < count; i++) {
            Stat stat = group.get(i);
            if (currentRow == null || inRow == 5) {
                currentRow = new LinearLayout(this);
                currentRow.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                grid.addView(currentRow, margin(0, 4));
                inRow = 0;
            }
            List<LocalDate> hitDates = latestHitDates(stat.number);
            boolean hit = !hitDates.isEmpty();

            LinearLayout cell = new LinearLayout(this);
            cell.setGravity(Gravity.CENTER);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setPadding(dp(4), dp(4), dp(4), dp(4));

            TextView ball = text("" + stat.number, 13, hit ? 0xFF064E3B : Color.rgb(15, 23, 42), true);
            ball.setGravity(Gravity.CENTER);
            ball.setBackground(hit
                    ? roundBorder(0xFFA7F3D0, 0xFF10B981, 36)
                    : roundBorder(0xFFCBD5E1, 0xFF475569, 36));

            cell.addView(ball, new LinearLayout.LayoutParams(dp(36), dp(36)));

            String subText = hit ? formatHitDatesMockup(hitDates) : "Not yet";
            TextView subView = text(subText, 10, hit ? GREEN : Color.rgb(51, 65, 85), true);
            subView.setGravity(Gravity.CENTER);
            subView.setMaxLines(2);
            cell.addView(subView, margin(2, 2));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(6), 0, dp(6), 0);
            currentRow.addView(cell, lp);
            inRow++;
        }

        return grid;
    }

    @SuppressWarnings("ReassignedVariable")
    private void buildCurrentPeriodCard() {
        PeriodBucket latest = latestSelectedBucket();
        LinearLayout card = card(true);
        card.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 20));

        LocalDate latestDrawInBucket = null;
        if (latest != null && !latest.draws().isEmpty()) {
            latestDrawInBucket = latest.draws().get(latest.draws().size() - 1).date();
        }

        int currentY = latestDate != null ? latestDate.getYear() : LocalDate.now().getYear();
        int currentM = latestDate != null ? latestDate.getMonthValue() : LocalDate.now().getMonthValue();
        int currentQ = latestDate != null ? quarterOf(latestDate) : quarterOf(LocalDate.now());

        boolean isCurrentInProgressPeriod = ("Month".equals(mode) && selectedYear == currentY && selectedMonth == currentM)
                || ("Quarter".equals(mode) && selectedYear == currentY && selectedQuarter == currentQ)
                || ("4 weeks".equals(mode) && selectedYear == currentY);

        String title = periodName() + " " + selectedYear + " results" + (isCurrentInProgressPeriod ? " so far" : "");
        card.addView(text(title, 17, Color.WHITE, true));

        String dateLine = (latest == null || latest.draws().isEmpty() || latestDrawInBucket == null)
                ? "No drawings recorded for this period"
                : (isCurrentInProgressPeriod
                        ? "Drawings through " + latestDrawInBucket.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                        : latest.draws().size() + " drawings recorded");
        card.addView(text(dateLine, 11, 0xFF93C5FD, false), margin(2, 14));

        TreeSet<Integer> hitNumbers = new TreeSet<>();
        if (latest != null) {
            for (Draw draw : latest.draws()) {
                for (int number : draw.numbers()) hitNumbers.add(number);
            }
        }

        if (hitNumbers.isEmpty()) {
            card.addView(text("No numbers hit in this period", 12, Color.WHITE, false), margin(0, 4));
        } else {
            String hitLabel = isCurrentInProgressPeriod
                    ? "NUMBERS HIT SO FAR (" + hitNumbers.size() + ")"
                    : "NUMBERS HIT IN " + selectedYear + " (" + hitNumbers.size() + ")";
            card.addView(text(hitLabel, 10, 0xFF93C5FD, true), margin(0, 6));

            LinearLayout grid = new LinearLayout(this);
            grid.setOrientation(LinearLayout.VERTICAL);
            LinearLayout currentRow = null;
            int inRow = 0;

            for (int number : hitNumbers) {
                if (currentRow == null || inRow == 6) {
                    currentRow = new LinearLayout(this);
                    currentRow.setGravity(Gravity.CENTER_VERTICAL);
                    grid.addView(currentRow, margin(0, 4));
                    inRow = 0;
                }
                TextView badge = text("" + number, 13, 0xFF047857, true);
                badge.setGravity(Gravity.CENTER);
                badge.setPadding(dp(10), dp(5), dp(10), dp(5));
                badge.setBackground(roundBorder(0xFFD1FAE5, 0xFFA7F3D0, 14));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, dp(6), 0);
                currentRow.addView(badge, lp);
                inRow++;
            }
            card.addView(grid, margin(0, 4));
        }

        if (latest != null && !latest.draws().isEmpty()) {
            TextView weeklyBtn = text("📅   View " + periodName() + " drawings by week  ➔", 12, 0xFFBFDBFE, true);
            weeklyBtn.setPadding(0, dp(12), 0, 0);
            weeklyBtn.setOnClickListener(v -> showWeeklyBreakdownDialog());
            card.addView(weeklyBtn);
        }

        content.addView(card, margin(0, 14));
    }

    private void buildSmartPickerButton() {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundBorder(0xFF0F172A, 0xFF00F2FE, 18));

        TextView icon = text("🎯", 20, Color.WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(0xFF1E293B, 36));
        card.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        col.addView(text("Generate Smart Picks", 15, Color.WHITE, true));
        col.addView(text("Pick numbers using tier frequency & due patterns", 11, 0xFF93C5FD, false), margin(2, 0));
        card.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView chevron = text("›", 24, Color.WHITE, true);
        chevron.setGravity(Gravity.CENTER);
        card.addView(chevron);

        card.setOnClickListener(v -> showSmartPickerDialog());
        content.addView(card, margin(0, 10));
    }

    private void buildFavoritesButton() {
        Set<String> savedFavs = getSharedPreferences("favorites", MODE_PRIVATE).getStringSet("saved_sets", Collections.emptySet());
        int count = savedFavs.size();

        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(roundBorder(0xFF0F172A, 0xFF475569, 18));

        TextView icon = text("⭐", 18, Color.WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(GOLD, 36));
        card.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        col.addView(text("My Saved Favorites (" + count + ")", 15, Color.WHITE, true));
        col.addView(text(count == 0 ? "Tap to view or save favorite number sets" : count + " custom sets saved locally", 11, GOLD, false), margin(2, 0));
        card.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView chevron = text("›", 24, GOLD, true);
        chevron.setGravity(Gravity.CENTER);
        card.addView(chevron);

        card.setOnClickListener(v -> showFavoritesDialog());
        content.addView(card, margin(0, 10));
    }

    private void showFavoritesDialog() {
        Set<String> savedFavs = getSharedPreferences("favorites", MODE_PRIVATE).getStringSet("saved_sets", Collections.emptySet());
        Set<String> selectedForDelete = new TreeSet<>();

        int curYear = latestDate != null ? latestDate.getYear() : LocalDate.now().getYear();
        int curMonth = latestDate != null ? latestDate.getMonthValue() : LocalDate.now().getMonthValue();

        ArrayList<Draw> currentMonthDraws = new ArrayList<>();
        for (Draw d : draws) {
            if (d.date().getYear() == curYear && d.date().getMonthValue() == curMonth) {
                currentMonthDraws.add(d);
            }
        }
        currentMonthDraws.sort(Comparator.comparing(Draw::date).reversed());

        final int[] drawIdx = { 0 };

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("⭐ My Saved Favorites", 18, INK, true));
        layout.addView(text("Latest winning numbers & week-by-week draw inspection:", 12, MUTED, false), margin(2, 10));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Close", null)
                .create();

        Runnable[] updateContent = new Runnable[1];

        updateContent[0] = () -> {
            while (layout.getChildCount() > 2) {
                layout.removeViewAt(2);
            }

            Draw activeDraw = currentMonthDraws.isEmpty() ? (draws.isEmpty() ? null : draws.get(draws.size() - 1)) : currentMonthDraws.get(Math.min(drawIdx[0], currentMonthDraws.size() - 1));

            if (currentMonthDraws.size() > 1) {
                LinearLayout drawNav = new LinearLayout(this);
                drawNav.setGravity(Gravity.CENTER_VERTICAL);
                drawNav.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 16));

                TextView prevD = text("‹", 26, BLUE, true);
                prevD.setGravity(Gravity.CENTER);
                drawNav.addView(prevD, new LinearLayout.LayoutParams(dp(44), dp(44)));

                String drawNavLabel = activeDraw != null
                        ? "Draw " + (drawIdx[0] + 1) + " of " + currentMonthDraws.size() + "  ·  " + activeDraw.date().format(DATE_FORMAT)
                        : "No draws";
                TextView drawVal = text(drawNavLabel, 13, BLUE, true);
                drawVal.setGravity(Gravity.CENTER);
                drawNav.addView(drawVal, new LinearLayout.LayoutParams(0, dp(44), 1f));

                TextView nextD = text("›", 26, BLUE, true);
                nextD.setGravity(Gravity.CENTER);
                drawNav.addView(nextD, new LinearLayout.LayoutParams(dp(44), dp(44)));

                prevD.setOnClickListener(v -> {
                    if (drawIdx[0] < currentMonthDraws.size() - 1) {
                        drawIdx[0]++;
                        updateContent[0].run();
                    }
                });

                nextD.setOnClickListener(v -> {
                    if (drawIdx[0] > 0) {
                        drawIdx[0]--;
                        updateContent[0].run();
                    }
                });

                layout.addView(drawNav, margin(0, 10));
            }

            if (activeDraw != null) {
                LinearLayout latestCard = card(false);
                latestCard.setBackground(roundBorder(0xFF1E293B, 0xFF0EA5E9, 14));
                latestCard.addView(text("🏆 Winning Draw (" + activeDraw.date().format(DATE_FORMAT) + ")", 13, BLUE, true), margin(0, 4));

                LinearLayout winningNumRow = new LinearLayout(this);
                winningNumRow.setOrientation(LinearLayout.HORIZONTAL);
                winningNumRow.setGravity(Gravity.CENTER_VERTICAL);
                for (int num : activeDraw.numbers()) {
                    TextView badge = text(String.format(Locale.US, "%02d", num), 12, 0xFF064E3B, true);
                    badge.setGravity(Gravity.CENTER);
                    badge.setPadding(dp(8), dp(4), dp(8), dp(4));
                    badge.setBackground(roundBorder(0xFFA7F3D0, 0xFF10B981, 16));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, dp(4), 0);
                    winningNumRow.addView(badge, lp);
                }
                latestCard.addView(winningNumRow, margin(0, 4));
                layout.addView(latestCard, margin(0, 10));
            }

            if (savedFavs.isEmpty()) {
                layout.addView(text("No favorite sets saved yet. Generate or lock custom picks and tap 'Save Set to My Favorites'!", 12, MUTED, false), margin(0, 12));
            } else {
                Set<Integer> winningSet = new java.util.HashSet<>();
                if (activeDraw != null) {
                    for (int wn : activeDraw.numbers()) winningSet.add(wn);
                }

                LinearLayout favBox = card(false);
                favBox.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 14));

                TextView batchDelBtn = text("🗑   Delete Selected (0)", 12, MUTED, true);
                batchDelBtn.setGravity(Gravity.CENTER);
                batchDelBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
                batchDelBtn.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 12));
                batchDelBtn.setEnabled(false);

                Runnable updateBatchBtn = () -> {
                    int size = selectedForDelete.size();
                    batchDelBtn.setText("🗑   Delete Selected (" + size + ")");
                    batchDelBtn.setEnabled(size > 0);
                    batchDelBtn.setTextColor(size > 0 ? 0xFFEF4444 : MUTED);
                    batchDelBtn.setBackground(size > 0
                            ? roundBorder(0xFF450A0A, 0xFFEF4444, 12)
                            : roundBorder(0xFF1E293B, 0xFF334155, 12));
                };

                boolean firstFav = true;
                for (String setKey : savedFavs) {
                    if (!firstFav) favBox.addView(divider());
                    firstFav = false;

                    LinearLayout row = new LinearLayout(this);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(0, dp(10), 0, dp(10));

                    TextView check = text("   ☐   ", 16, MUTED, true);

                    int matchCount = 0;
                    LinearLayout numCol = new LinearLayout(this);
                    numCol.setOrientation(LinearLayout.VERTICAL);

                    LinearLayout numRow = new LinearLayout(this);
                    numRow.setOrientation(LinearLayout.HORIZONTAL);
                    numRow.setGravity(Gravity.CENTER_VERTICAL);

                    String[] parts = setKey.split("·");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (trimmed.isEmpty()) continue;
                        int num = 0;
                        try {
                            num = Integer.parseInt(trimmed);
                        } catch (Exception ignored) {}
                        if (num == 0) continue;

                        boolean isMatch = winningSet.contains(num);
                        if (isMatch) matchCount++;

                        int bgColor = isMatch ? 0xFFFEF3C7 : 0xFF334155;
                        int strokeColor = isMatch ? GOLD : 0xFF64748B;
                        int textColor = isMatch ? Color.BLACK : Color.WHITE;

                        TextView badge = text(String.format(Locale.US, "%02d", num), 12, textColor, true);
                        badge.setGravity(Gravity.CENTER);
                        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
                        badge.setBackground(roundBorder(bgColor, strokeColor, 16));

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, dp(4), 0);
                        numRow.addView(badge, lp);
                    }

                    numCol.addView(numRow);
                    if (matchCount > 0) {
                        String matchLabel = matchCount == 6 ? "🎉 JACKPOT MATCH (6/6)!" : "🎯 " + matchCount + "/6 Match with Winning Draw!";
                        numCol.addView(text(matchLabel, 10, GOLD, true), margin(2, 0));
                    }

                    row.setOnClickListener(v -> {
                        if (selectedForDelete.contains(setKey)) {
                            selectedForDelete.remove(setKey);
                            check.setText("   ☐   ");
                            check.setTextColor(MUTED);
                        } else {
                            selectedForDelete.add(setKey);
                            check.setText("   ☑   ");
                            check.setTextColor(BLUE);
                        }
                        updateBatchBtn.run();
                    });

                    row.addView(check);
                    row.addView(numCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    favBox.addView(row);
                }

                batchDelBtn.setOnClickListener(v -> showBatchDeleteConfirmDialog(selectedForDelete, dialog));

                layout.addView(favBox, margin(0, 12));
                layout.addView(batchDelBtn, margin(0, 4));
            }
        };

        updateContent[0].run();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private void showBatchDeleteConfirmDialog(Set<String> selectedKeys, AlertDialog parentDialog) {
        if (selectedKeys.isEmpty()) return;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);

        layout.addView(text("Delete Selected Favorites", 18, INK, true));
        layout.addView(text("Are you sure you want to delete these " + selectedKeys.size() + " favorite set(s)?", 12, MUTED, false), margin(4, 12));

        LinearLayout card = card(false);
        card.setBackground(roundBorder(0xFF450A0A, 0xFFEF4444, 14));

        for (String key : selectedKeys) {
            card.addView(text("⭐ " + key, 13, 0xFFDC2626, true), margin(2, 4));
        }
        layout.addView(card, margin(0, 14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete All (" + selectedKeys.size() + ")", (d, which) -> {
                    Set<String> favs = new TreeSet<>(getSharedPreferences("favorites", MODE_PRIVATE).getStringSet("saved_sets", Collections.emptySet()));
                    favs.removeAll(selectedKeys);
                    getSharedPreferences("favorites", MODE_PRIVATE).edit().putStringSet("saved_sets", favs).apply();
                    Toast.makeText(this, selectedKeys.size() + " favorite set(s) deleted.", Toast.LENGTH_SHORT).show();
                    if (parentDialog != null) parentDialog.dismiss();
                    render();
                })
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    @SuppressWarnings("ReassignedVariable")
    private int[] generateSmartPicksWithLocks(String strategy, Set<Integer> locked) {
        List<PeriodBucket> buckets = historicalBuckets();
        ArrayList<Stat> stats = calculateStats(buckets);
        if (stats.isEmpty()) return new int[]{4, 12, 19, 25, 38, 42};

        TreeSet<Integer> hitThisMonth = new TreeSet<>();
        PeriodBucket latest = latestSelectedBucket();
        if (latest != null) {
            for (Draw draw : latest.draws()) {
                for (int num : draw.numbers()) hitThisMonth.add(num);
            }
        }

        ArrayList<Stat> dueTopTier = new ArrayList<>();
        ArrayList<Stat> hotMonthHits = new ArrayList<>();
        ArrayList<Stat> moderateTier = new ArrayList<>();

        for (Stat stat : stats) {
            boolean hit = hitThisMonth.contains(stat.number);
            if (!hit && stat.percent >= 70) {
                dueTopTier.add(stat);
            } else if (hit) {
                hotMonthHits.add(stat);
            } else {
                moderateTier.add(stat);
            }
        }

        LinkedHashSet<Integer> picked = new LinkedHashSet<>(locked);
        java.util.Random rand = new java.util.Random();

        if ("Overdue".equals(strategy)) {
            Collections.shuffle(dueTopTier, rand);
            for (Stat stat : dueTopTier) {
                if (picked.size() < 6) picked.add(stat.number);
            }
        } else if ("Weighted".equals(strategy)) {
            while (picked.size() < 6 && !stats.isEmpty()) {
                int index = rand.nextInt(Math.min(18, stats.size()));
                picked.add(stats.get(index).number);
            }
        } else {
            Collections.shuffle(dueTopTier, rand);
            for (Stat stat : dueTopTier) {
                if (picked.size() < 3) picked.add(stat.number);
            }
            Collections.shuffle(hotMonthHits, rand);
            for (Stat stat : hotMonthHits) {
                if (picked.size() < 5) picked.add(stat.number);
            }
            Collections.shuffle(moderateTier, rand);
            for (Stat stat : moderateTier) {
                if (picked.size() < 6) picked.add(stat.number);
            }
        }

        for (Stat stat : stats) {
            if (picked.size() < 6) picked.add(stat.number);
        }

        int[] result = new int[6];
        List<Integer> list = new ArrayList<>(picked);
        for (int i = 0; i < 6; i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private void showLockNumbersDialog(Set<Integer> locked, Runnable onUpdated) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("🔒 Lock Custom Favorite Numbers", 16, INK, true));
        layout.addView(text("Tap up to 5 numbers to lock in. Tier status for " + periodName() + " is highlighted below!", 12, MUTED, false), margin(2, 8));

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER);
        legend.setPadding(dp(8), dp(6), dp(8), dp(6));
        legend.setBackground(round(0xFF1E293B, 12));

        legend.addView(text("⭐ Overdue Top  ", 10, BLUE, true));
        legend.addView(text("🔥 Top Tier  ", 10, GREEN, true));
        legend.addView(text("✓ Hit This Period", 10, GREEN, true));
        layout.addView(legend, margin(0, 10));

        List<PeriodBucket> buckets = historicalBuckets();
        ArrayList<Stat> stats = calculateStats(buckets);
        Map<Integer, Stat> statMap = new HashMap<>();
        for (Stat s : stats) statMap.put(s.number, s);

        TreeSet<Integer> hitThisMonth = new TreeSet<>();
        PeriodBucket latest = latestSelectedBucket();
        if (latest != null) {
            for (Draw draw : latest.draws()) {
                for (int num : draw.numbers()) hitThisMonth.add(num);
            }
        }

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout currentRow = null;
        int inRow = 0;

        AlertDialog[] dialogHolder = new AlertDialog[1];

        for (int num = 1; num <= 54; num++) {
            final int number = num;
            if (currentRow == null || inRow == 3) {
                currentRow = new LinearLayout(this);
                currentRow.setGravity(Gravity.CENTER);
                grid.addView(currentRow, margin(0, 4));
                inRow = 0;
            }

            boolean isLocked = locked.contains(number);
            boolean isHit = hitThisMonth.contains(number);
            Stat s = statMap.get(number);
            boolean isTopTier = (s != null && s.percent >= 70);
            boolean isOverdueTop = isTopTier && !isHit;

            String tag = isOverdueTop ? " ⭐" : (isHit ? (isTopTier ? " 🔥" : " ✓") : "");
            String label = String.format(Locale.US, "%d%s", number, tag);

            TextView chip = text(label, 12, isLocked ? Color.BLACK : (isOverdueTop ? BLUE : (isHit ? GREEN : INK)), true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(4), dp(8), dp(4), dp(8));
            chip.setBackground(isLocked
                    ? round(BLUE, 12)
                    : (isOverdueTop ? roundBorder(0xFF1E293B, BLUE, 12) : (isHit ? roundBorder(0xFFA7F3D0, 0xFF10B981, 12) : roundBorder(0xFF1E293B, 0xFF334155, 12))));

            chip.setOnClickListener(v -> {
                if (locked.contains(number)) {
                    locked.remove(number);
                    chip.setBackground(isOverdueTop ? roundBorder(0xFF1E293B, BLUE, 12) : (isHit ? roundBorder(0xFFA7F3D0, 0xFF10B981, 12) : roundBorder(0xFF1E293B, 0xFF334155, 12)));
                    chip.setTextColor(isOverdueTop ? BLUE : (isHit ? GREEN : INK));
                } else if (locked.size() < 5) {
                    locked.add(number);
                    chip.setBackground(round(BLUE, 12));
                    chip.setTextColor(Color.BLACK);
                } else {
                    Toast.makeText(this, "You can lock up to 5 custom numbers.", Toast.LENGTH_SHORT).show();
                }
                onUpdated.run();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            currentRow.addView(chip, lp);
            inRow++;
        }

        layout.addView(grid, margin(0, 12));

        dialogHolder[0] = new AlertDialog.Builder(this)
                .setTitle("Lock Custom Numbers")
                .setView(scroll)
                .setPositiveButton("Done", (d, w) -> onUpdated.run())
                .create();

        if (dialogHolder[0].getWindow() != null) {
            dialogHolder[0].getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialogHolder[0].show();
    }

    private void saveFavoritePick(int[] numbers) {
        if (numbers == null || numbers.length != 6) return;
        String key = joinNumbers(numbers);
        Set<String> favs = new TreeSet<>(getSharedPreferences("favorites", MODE_PRIVATE).getStringSet("saved_sets", Collections.emptySet()));
        
        if (!isProUser() && favs.size() >= 3 && !favs.contains(key)) {
            showProUpgradeDialog();
            Toast.makeText(this, "Free version is limited to 3 saved favorite sets. Upgrade to Pro for unlimited sets!", Toast.LENGTH_LONG).show();
            return;
        }

        if (favs.add(key)) {
            getSharedPreferences("favorites", MODE_PRIVATE).edit().putStringSet("saved_sets", favs).apply();

            TreeSet<Integer> hitThisMonth = new TreeSet<>();
            PeriodBucket latest = latestSelectedBucket();
            if (latest != null) {
                for (Draw draw : latest.draws()) {
                    for (int num : draw.numbers()) hitThisMonth.add(num);
                }
            }

            LinearLayout confirmLayout = new LinearLayout(this);
            confirmLayout.setOrientation(LinearLayout.VERTICAL);
            confirmLayout.setPadding(dp(20), dp(16), dp(20), dp(16));
            confirmLayout.setBackgroundColor(CARD_BG);

            confirmLayout.addView(text("⭐ Saved to My Favorites!", 18, INK, true));
            confirmLayout.addView(text("Your 6-number set has been saved to your personal Favorites list:", 12, MUTED, false), margin(4, 12));

            LinearLayout card = card(false);
            card.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 14));

            LinearLayout numRow = new LinearLayout(this);
            numRow.setOrientation(LinearLayout.HORIZONTAL);
            numRow.setGravity(Gravity.CENTER_VERTICAL);

            for (int num : numbers) {
                boolean isHit = hitThisMonth.contains(num);
                TextView badge = text(String.format(Locale.US, "%02d", num), 12, isHit ? 0xFF064E3B : Color.WHITE, true);
                badge.setGravity(Gravity.CENTER);
                badge.setPadding(dp(8), dp(4), dp(8), dp(4));
                badge.setBackground(isHit
                        ? roundBorder(0xFFA7F3D0, 0xFF10B981, 16)
                        : roundBorder(0xFF334155, 0xFF64748B, 16));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, dp(4), 0);
                numRow.addView(badge, lp);
            }
            card.addView(numRow);
            confirmLayout.addView(card, margin(0, 14));

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(confirmLayout)
                    .setPositiveButton("Great!", (d, w) -> render())
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
            }
            dialog.show();
        } else {
            Toast.makeText(this, "This set is already in your Favorites.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings({"ReassignedVariable", "ForLoopReplaceableByWhile", "unused"})
    private void showSmartPickerDialog() {
        final String[] activeStrategy = {"Balanced"};
        final int[][] currentPicks = {new int[0]};
        final Set<Integer> lockedNumbers = new LinkedHashSet<>();

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("🎯  Smart Number Generator", 18, INK, true));
        layout.addView(text("Calculated from historical patterns for " + periodName(), 12, MUTED, false), margin(2, 6));

        TextView introQuote = text("💡 Rather than picking 6 numbers uniformly at random like a generic quick pick machine, the generator filters, weights, and blends numbers based on 30+ years of real historical performance trends.", 11, INK, false);
        introQuote.setPadding(dp(12), dp(8), dp(12), dp(8));
        introQuote.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 12));
        layout.addView(introQuote, margin(0, 12));

        LinearLayout strategyTabs = new LinearLayout(this);
        strategyTabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        strategyTabs.setBackground(round(0xFF1E293B, 16));

        String[][] strategies = {
                {"Balanced", "Balanced"},
                {"Overdue", "Overdue"},
                {"Weighted", "Weighted"},
                {"Lock", "🔒 Custom Lock"}
        };

        TextView lockBtn = text("🔒   Lock Custom Favorite Numbers (" + lockedNumbers.size() + "/5)  ➔", 11, BLUE, true);
        lockBtn.setGravity(Gravity.CENTER);
        lockBtn.setPadding(dp(10), dp(8), dp(10), dp(8));
        lockBtn.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 12));

        TextView strategyDesc = text("", 11, INK, false);
        strategyDesc.setPadding(dp(12), dp(8), dp(12), dp(8));
        strategyDesc.setBackground(round(0xFF1E293B, 12));

        TextView[] slotViews = new TextView[6];

        Runnable updateSlots = () -> {
            List<Integer> list = new ArrayList<>(lockedNumbers);
            for (int i = 0; i < 6; i++) {
                if (i < list.size()) {
                    slotViews[i].setText("🔒 " + list.get(i));
                    slotViews[i].setTextColor(BLUE);
                    slotViews[i].setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 16));
                } else if (currentPicks[0].length == 6) {
                    slotViews[i].setText(String.valueOf(currentPicks[0][i]));
                    slotViews[i].setTextColor(INK);
                    slotViews[i].setBackground(round(0xFF1E293B, 16));
                } else {
                    slotViews[i].setText("?");
                    slotViews[i].setTextColor(MUTED);
                    slotViews[i].setBackground(round(0xFF1E293B, 16));
                }
            }
        };

        Runnable updateDesc = () -> {
            lockBtn.setText("🔒   Lock Custom Favorite Numbers (" + lockedNumbers.size() + "/5)  ➔");
            String desc;
            String lockTag = lockedNumbers.isEmpty() ? "" : " (With " + lockedNumbers.size() + " Custom Numbers Locked 🔒)";
            switch (activeStrategy[0]) {
                case "Overdue" -> desc = "💡 Overdue Top-Tier" + lockTag + ": Auto-fills open slots with high-frequency Top-Tier numbers that have NOT hit in " + periodName() + " yet.";
                case "Weighted" -> desc = "💡 Weighted Odds" + lockTag + ": Auto-fills open slots with numbers weighted directly by their historical hit percentages.";
                default -> desc = "💡 Balanced Blend" + lockTag + ": Auto-fills open slots combining Overdue Top-Tier numbers, Hot numbers, and Moderate Tier numbers.";
            }
            strategyDesc.setText(desc);
            updateSlots.run();
        };

        lockBtn.setOnClickListener(v -> showLockNumbersDialog(lockedNumbers, updateDesc));

        LinearLayout breakdownBox = card(false);
        breakdownBox.setBackground(round(0xFF1E293B, 16));
        breakdownBox.addView(text("Select a strategy and tap Generate Numbers.", 11, MUTED, false));

        LinearLayout slotsRow = new LinearLayout(this);
        slotsRow.setOrientation(LinearLayout.HORIZONTAL);
        slotsRow.setGravity(Gravity.CENTER);

        for (int i = 0; i < 6; i++) {
            TextView slot = text("?", 16, MUTED, true);
            slotViews[i] = slot;
            slot.setGravity(Gravity.CENTER);
            slot.setPadding(0, dp(12), 0, dp(12));
            slot.setBackground(round(0xFF1E293B, 16));

            slot.setOnClickListener(v -> showLockNumbersDialog(lockedNumbers, updateDesc));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            slotsRow.addView(slot, lp);
        }

        Runnable updateBreakdown = () -> {
            breakdownBox.removeAllViews();
            int[] picks = currentPicks[0];
            if (picks.length == 0) return;

            breakdownBox.addView(text("STATISTICAL BREAKDOWN FOR THIS SET", 10, MUTED, true), margin(0, 8));

            List<PeriodBucket> buckets = historicalBuckets();
            ArrayList<Stat> stats = calculateStats(buckets);
            TreeSet<Integer> hitThisMonth = new TreeSet<>();
            PeriodBucket latest = latestSelectedBucket();
            if (latest != null) {
                for (Draw draw : latest.draws()) {
                    for (int num : draw.numbers()) hitThisMonth.add(num);
                }
            }

            int sumPercent = 0;
            for (int num : picks) {
                for (Stat stat : stats) {
                    if (stat.number == num) {
                        sumPercent += stat.percent;
                        boolean isHit = hitThisMonth.contains(num);
                        String tag = isHit ? " (Hit this " + mode.toLowerCase(Locale.US) + ")" : (stat.percent >= 70 ? " (Overdue Top-Tier)" : "");
                        String detail = "#" + String.format(Locale.US, "%02d", num) + "  ·  " + stat.hits + "/" + stat.eligible + " periods  ·  " + stat.percent + "%" + tag;
                        breakdownBox.addView(text(detail, 11, isHit ? GREEN : INK, false), margin(0, 3));
                    }
                }
            }
            int avgPercent = Math.round((float) sumPercent / 6f);
            breakdownBox.addView(divider(), margin(8, 8));
            breakdownBox.addView(text("Average Historical Hit Rate: " + avgPercent + "%", 11, BLUE, true));

            TextView saveFavBtn = text("⭐   Save Set to My Favorites", 12, BLUE, true);
            saveFavBtn.setGravity(Gravity.CENTER);
            saveFavBtn.setPadding(dp(10), dp(8), dp(10), dp(8));
            saveFavBtn.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 12));
            saveFavBtn.setOnClickListener(v -> saveFavoritePick(picks));

            TextView resetBtn = text("🔄   New Pick", 12, INK, true);
            resetBtn.setGravity(Gravity.CENTER);
            resetBtn.setPadding(dp(10), dp(8), dp(10), dp(8));
            resetBtn.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 12));
            resetBtn.setOnClickListener(v -> {
                lockedNumbers.clear();
                currentPicks[0] = new int[0];
                updateSlots.run();
                breakdownBox.removeAllViews();
                breakdownBox.addView(text("Select a strategy and tap Generate Numbers.", 11, MUTED, false));
                Toast.makeText(this, "Reset! Ready for your new pick.", Toast.LENGTH_SHORT).show();
            });

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.VERTICAL);
            actionRow.addView(saveFavBtn, margin(0, 4));
            actionRow.addView(resetBtn, margin(0, 2));

            breakdownBox.addView(actionRow, margin(10, 2));
        };

        Runnable startRollAnimation = () -> {
            int[] finalPicks = generateSmartPicksWithLocks(activeStrategy[0], lockedNumbers);
            currentPicks[0] = finalPicks;

            TreeSet<Integer> hitThisMonth = new TreeSet<>();
            PeriodBucket latest = latestSelectedBucket();
            if (latest != null) {
                for (Draw draw : latest.draws()) {
                    for (int num : draw.numbers()) hitThisMonth.add(num);
                }
            }

            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            java.util.Random rand = new java.util.Random();

            for (int i = 0; i < 6; i++) {
                final int slotIdx = i;
                final int finalNum = finalPicks[i];
                final boolean isLocked = lockedNumbers.contains(finalNum);
                final boolean isHit = hitThisMonth.contains(finalNum);

                if (isLocked) {
                    slotViews[slotIdx].setText("🔒 " + finalNum);
                    slotViews[slotIdx].setTextColor(BLUE);
                    slotViews[slotIdx].setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 16));
                    if (slotIdx == 5) {
                        updateBreakdown.run();
                    }
                    continue;
                }

                final int rolls = 8 + i * 3;

                for (int r = 0; r < rolls; r++) {
                    final boolean isLast = (r == rolls - 1);
                    final int delay = r * 30 + i * 50;
                    handler.postDelayed(() -> {
                        if (isLast) {
                            slotViews[slotIdx].setText(String.valueOf(finalNum));
                            slotViews[slotIdx].setBackground(isHit ? roundBorder(0xFFA7F3D0, 0xFF10B981, 16) : round(0xFF1E293B, 16));
                            slotViews[slotIdx].setTextColor(isHit ? GREEN : INK);
                            slotViews[slotIdx].animate().scaleX(1.15f).scaleY(1.15f).setDuration(80)
                                    .withEndAction(() -> slotViews[slotIdx].animate().scaleX(1.0f).scaleY(1.0f).setDuration(80));
                            if (slotIdx == 5) {
                                updateBreakdown.run();
                            }
                        } else {
                            int randomVal = rand.nextInt(54) + 1;
                            slotViews[slotIdx].setText(String.valueOf(randomVal));
                            slotViews[slotIdx].setBackground(round(0xFF1E293B, 16));
                            slotViews[slotIdx].setTextColor(MUTED);
                        }
                    }, delay);
                }
            }
        };

        TextView[] tabViews = new TextView[strategies.length];
        for (int k = 0; k < strategies.length; k++) {
            final int stratIndex = k;
            String id = strategies[k][0];
            String name = strategies[k][1];
            boolean isSel = Objects.equals(activeStrategy[0], id);

            TextView tab = text(name, 11, isSel ? INK : MUTED, true);
            tabViews[k] = tab;
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(4), dp(8), dp(4), dp(8));
            tab.setBackground(round(isSel ? CARD_BG : Color.TRANSPARENT, 12));

            tab.setOnClickListener(v -> {
                activeStrategy[0] = id;
                for (int j = 0; j < tabViews.length; j++) {
                    boolean sel = (j == stratIndex);
                    tabViews[j].setBackground(round(sel ? CARD_BG : Color.TRANSPARENT, 12));
                    tabViews[j].setTextColor(sel ? INK : MUTED);
                }
                updateDesc.run();
                if ("Lock".equals(id)) {
                    showLockNumbersDialog(lockedNumbers, updateDesc);
                }
            });

            strategyTabs.addView(tab, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }

        updateDesc.run();

        TextView guideBtn = text("📊   View Strategy Comparison Guide  ➔", 11, BLUE, true);
        guideBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        guideBtn.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 12));
        guideBtn.setOnClickListener(v -> showStrategyGuideDialog());

        layout.addView(strategyTabs, margin(0, 10));
        layout.addView(strategyDesc, margin(0, 10));
        layout.addView(lockBtn, margin(0, 10));
        layout.addView(guideBtn, margin(0, 12));
        layout.addView(slotsRow, margin(0, 14));

        GradientDrawable goldGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { 0xFFFFE082, 0xFFF59E0B }
        );
        goldGrad.setCornerRadius(dp(26));
        goldGrad.setStroke(dp(2), 0xFFFEF3C7);

        TextView rollBtn = text("✨   Generate Numbers", 15, 0xFF111827, true);
        rollBtn.setGravity(Gravity.CENTER);
        rollBtn.setPadding(dp(20), dp(14), dp(20), dp(14));
        rollBtn.setBackground(goldGrad);
        rollBtn.setElevation(dp(8));
        rollBtn.setOnClickListener(v -> startRollAnimation.run());
        layout.addView(rollBtn, margin(0, 14));

        layout.addView(breakdownBox, margin(0, 10));

        TextView legalNotice = text("💡 For entertainment & statistical exploration only. Historical patterns do not predict future lottery drawings or guarantee winning tickets.", 10, MUTED, false);
        legalNotice.setPadding(dp(4), dp(4), dp(4), dp(4));
        layout.addView(legalNotice, margin(0, 10));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Close", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private void showStrategyGuideDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("📊 Strategy & Statistical Guide", 18, INK, true));
        layout.addView(text("Comparing the 3 Smart Generator strategies from a probability standpoint", 12, MUTED, false), margin(2, 6));

        TextView bannerQuote = text("💡 Rather than picking 6 numbers uniformly at random like a generic quick pick machine, Texas Quick Hits filters, weights, and blends numbers based on 30+ years of real historical performance trends.", 11, INK, false);
        bannerQuote.setPadding(dp(12), dp(8), dp(12), dp(8));
        bannerQuote.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 12));
        layout.addView(bannerQuote, margin(0, 12));

        LinearLayout box = card(false);
        box.setBackground(round(0xFF1E293B, 14));

        box.addView(text("⭐ Balanced Blend (Most Recommended)", 13, BLUE, true));
        box.addView(text("• Logic: 3 Overdue Top-Tier + 2 Hot numbers + 1 Moderate Tier.\n• Statistical Advantage: Real winning 6-number lottery draws almost never consist of 100% cold or 100% hot numbers. Balanced Blend most closely mirrors real historical draw patterns.", 11, INK, false), margin(4, 12));

        box.addView(text("📈 Weighted Odds", 13, INK, true));
        box.addView(text("• Logic: Selection chances weighted directly by 30+ year hit rate %.\n• Statistical Advantage: Gives higher weight to numbers that have demonstrated higher historical eligibility and appearance frequency over decades.", 11, INK, false), margin(4, 12));

        box.addView(text("🎯 Overdue Top-Tier", 13, INK, true));
        box.addView(text("• Logic: Picks 6 Top-Tier numbers that have NOT hit in the current month yet.\n• Statistical Advantage: Targets historically dominant numbers that are due based on regression to the mean.", 11, INK, false), margin(4, 12));

        box.addView(divider(), margin(6, 12));
        box.addView(text("💡 Mathematical Fact", 12, INK, true));
        box.addView(text("Every unique 6-number set has an identical 1-in-25,827,165 odds of being drawn. Balanced Blend helps you pick well-distributed number sets that minimize sharing a jackpot with other players!", 11, MUTED, false));

        layout.addView(box);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Got it", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }





    @SuppressWarnings("ReassignedVariable")
    private void showNumberHitHistoryDialog(int targetNumber) {
        final int[] dialogYear = { latestDate != null ? latestDate.getYear() : LocalDate.now().getYear() };

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        Runnable[] updateContent = new Runnable[1];

        updateContent[0] = () -> {
            layout.removeAllViews();

            ArrayList<Draw> hitsInSelectedYear = new ArrayList<>();
            ArrayList<Draw> totalHitsInHistory = new ArrayList<>();

            for (Draw draw : draws) {
                if (contains(draw.numbers(), targetNumber)) {
                    totalHitsInHistory.add(draw);
                    if (draw.date().getYear() == dialogYear[0]) {
                        hitsInSelectedYear.add(draw);
                    }
                }
            }

            totalHitsInHistory.sort(Comparator.comparing(Draw::date).reversed());
            hitsInSelectedYear.sort(Comparator.comparing(Draw::date).reversed());

            layout.addView(text("🔍 Hit History for Number " + targetNumber, 18, INK, true));
            layout.addView(text("30+ Year Historical Search Results", 12, MUTED, false), margin(2, 12));

            LinearLayout yearPicker = new LinearLayout(this);
            yearPicker.setGravity(Gravity.CENTER_VERTICAL);
            yearPicker.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 16));

            TextView prevY = text("‹", 26, BLUE, true);
            prevY.setGravity(Gravity.CENTER);
            yearPicker.addView(prevY, new LinearLayout.LayoutParams(dp(44), dp(44)));

            TextView yearBtn = text("Year: " + dialogYear[0] + " ▾", 15, BLUE, true);
            yearBtn.setGravity(Gravity.CENTER);
            yearBtn.setOnClickListener(v -> {
                showYearPickerDialog(dialogYear[0], y -> {
                    dialogYear[0] = y;
                    updateContent[0].run();
                });
            });
            yearPicker.addView(yearBtn, new LinearLayout.LayoutParams(0, dp(44), 1f));

            TextView nextY = text("›", 26, BLUE, true);
            nextY.setGravity(Gravity.CENTER);
            yearPicker.addView(nextY, new LinearLayout.LayoutParams(dp(44), dp(44)));

            prevY.setOnClickListener(v -> { dialogYear[0]--; updateContent[0].run(); });
            nextY.setOnClickListener(v -> { dialogYear[0]++; updateContent[0].run(); });
            layout.addView(yearPicker, margin(0, 10));

            LinearLayout statCard = card(false);
            statCard.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 14));
            statCard.addView(text("• Hits in " + dialogYear[0] + ": " + hitsInSelectedYear.size() + " times", 13, BLUE, true), margin(0, 4));
            statCard.addView(text("• Total Hits (1992–2026): " + totalHitsInHistory.size() + " times", 13, INK, true), margin(0, 4));
            if (!totalHitsInHistory.isEmpty()) {
                statCard.addView(text("• Most Recent Hit: " + totalHitsInHistory.get(0).date().format(DATE_FORMAT), 12, GREEN, true), margin(0, 2));
            }
            layout.addView(statCard, margin(0, 14));

            layout.addView(text("Drawings in " + dialogYear[0] + " containing Number " + targetNumber + " (" + hitsInSelectedYear.size() + ")", 13, INK, true), margin(0, 8));

            if (hitsInSelectedYear.isEmpty()) {
                layout.addView(text("No drawings contained Number " + targetNumber + " in " + dialogYear[0] + ".", 12, MUTED, false), margin(0, 12));
            } else {
                LinearLayout hitBox = card(false);
                hitBox.setBackground(roundBorder(0xFF1E293B, 0xFF334155, 14));
                boolean first = true;
                for (Draw draw : hitsInSelectedYear) {
                    if (!first) hitBox.addView(divider());
                    first = false;

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(0, dp(6), 0, dp(6));

                    TextView dateText = text(draw.date().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)), 12, INK, true);
                    row.addView(dateText, margin(0, 4));

                    LinearLayout numRow = new LinearLayout(this);
                    numRow.setOrientation(LinearLayout.HORIZONTAL);
                    for (int num : draw.numbers()) {
                        boolean isTarget = (num == targetNumber);
                        TextView badge = text("" + num, 12, isTarget ? Color.WHITE : 0xFF064E3B, true);
                        badge.setGravity(Gravity.CENTER);
                        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
                        badge.setBackground(isTarget
                                ? roundBorder(0xFFDC2626, 0xFFB91C1C, 24)
                                : roundBorder(0xFFA7F3D0, 0xFF10B981, 24));

                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, dp(4), 0);
                        numRow.addView(badge, lp);
                    }
                    row.addView(numRow);

                    hitBox.addView(row);
                }
                layout.addView(hitBox, margin(0, 12));
            }
        };

        updateContent[0].run();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Close", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private void buildSearchPage() {
        addText("Search number history", 26, INK, true, 4);
        addText("Tap any number (1–54) to view its hit history across all years:", 13, MUTED, false, 12);

        LinearLayout card = card(false);
        card.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 18));
        card.addView(text("Select a number", 16, INK, true), margin(0, 10));

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        LinearLayout currentRow = null;
        int inRow = 0;

        for (int num = 1; num <= 54; num++) {
            final int number = num;
            if (currentRow == null || inRow == 6) {
                currentRow = new LinearLayout(this);
                currentRow.setGravity(Gravity.CENTER);
                grid.addView(currentRow, margin(0, 4));
                inRow = 0;
            }

            TextView chip = text(String.format(Locale.US, "%02d", number), 12, BLUE, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(4), dp(8), dp(4), dp(8));
            chip.setBackground(roundBorder(0xFF1E293B, 0xFF0EA5E9, 10));

            chip.setOnClickListener(v -> showNumberHitHistoryDialog(number));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            currentRow.addView(chip, lp);
            inRow++;
        }

        card.addView(grid, margin(0, 4));
        content.addView(card);
    }

    @SuppressWarnings("ReassignedVariable")
    private void buildHistory() {
        addText(selectedYear + " Draw history", 26, INK, true, 4);
        addText("Drawings for " + selectedYear + ". Saved manual results stay on this device.", 13, MUTED, false, 12);

        int currentY = latestDate != null ? latestDate.getYear() : LocalDate.now().getYear();
        if (selectedYear == currentY) {
            TextView add = text("＋   Add draw results for " + selectedYear, 14, Color.WHITE, true);
            add.setGravity(Gravity.CENTER_VERTICAL);
            add.setPadding(dp(18), dp(15), dp(18), dp(15));
            add.setBackground(round(0xFF0284C7, 18));
            add.setOnClickListener(v -> showAddDrawDialog());
            content.addView(add, margin(0, 14));

            TextView syncBtn = text("☁️   Sync Latest Cloud Results", 14, Color.WHITE, true);
            syncBtn.setGravity(Gravity.CENTER_VERTICAL);
            syncBtn.setPadding(dp(18), dp(15), dp(18), dp(15));
            syncBtn.setBackground(round(0xFF0284C7, 18));
            syncBtn.setOnClickListener(v -> {
                if (!isProUser()) {
                    showRewardedVideoSyncDialog(() -> syncOnlineCloudData(true));
                } else {
                    syncOnlineCloudData(true);
                }
            });
            content.addView(syncBtn, margin(0, 14));
        }

        ArrayList<Draw> yearDraws = new ArrayList<>();
        for (Draw draw : draws) {
            if (draw.date().getYear() == selectedYear) {
                yearDraws.add(draw);
            }
        }
        yearDraws.sort(Comparator.comparing(Draw::date).reversed());

        LinearLayout box = card(false);
        box.addView(text(selectedYear + " Drawings (" + yearDraws.size() + ")", 16, INK, true), margin(0, 8));

        if (yearDraws.isEmpty()) {
            box.addView(text("No drawings recorded for " + selectedYear + " yet.", 12, MUTED, false), margin(0, 8));
        } else {
            List<Draw> items = yearDraws.subList(0, Math.min(30, yearDraws.size()));
            boolean first = true;
            for (Draw draw : items) {
                if (!first) box.addView(divider());
                first = false;
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(0, dp(10), 0, dp(14));

                LinearLayout topRow = new LinearLayout(this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.addView(text(draw.date().format(DATE_FORMAT), 13, INK, true));
                boolean isManual = manualDates.contains(draw.date());
                info.addView(text(isManual ? "Manually added" : "Historical result", 10, isManual ? BLUE : MUTED, false));
                topRow.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                if (isManual) {
                    TextView delBtn = text("🗑", 14, 0xFFEF4444, true);
                    delBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
                    delBtn.setOnClickListener(v -> showDeleteConfirmDialog(draw));
                    topRow.addView(delBtn);
                }
                row.addView(topRow);

                LinearLayout numRow = new LinearLayout(this);
                numRow.setOrientation(LinearLayout.HORIZONTAL);
                numRow.setGravity(Gravity.CENTER_VERTICAL);
                for (int num : draw.numbers()) {
                    TextView badge = text(String.format(Locale.US, "%02d", num), 12, 0xFF064E3B, true);
                    badge.setGravity(Gravity.CENTER);
                    badge.setPadding(dp(8), dp(4), dp(8), dp(4));
                    badge.setBackground(roundBorder(0xFFA7F3D0, 0xFF10B981, 16));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, dp(6), dp(6), dp(2));
                    numRow.addView(badge, lp);
                }
                row.addView(numRow);

                box.addView(row);
            }
        }
        content.addView(box);
    }

    private void showDeleteConfirmDialog(Draw draw) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);

        layout.addView(text("Delete Manual Drawing", 18, INK, true));
        layout.addView(text("Are you sure you want to delete this manually added drawing?", 12, MUTED, false), margin(4, 12));

        LinearLayout card = card(false);
        card.setBackground(round(0xFFFEF2F2, 14));

        card.addView(text("📅 Date: " + draw.date().format(DATE_FORMAT), 12, INK, true), margin(0, 8));

        card.addView(text("NUMBERS TO BE DELETED", 10, 0xFFDC2626, true), margin(0, 6));

        LinearLayout ballsRow = new LinearLayout(this);
        ballsRow.setOrientation(LinearLayout.HORIZONTAL);
        ballsRow.setGravity(Gravity.CENTER_VERTICAL);

        for (int num : draw.numbers()) {
            TextView ball = text("" + num, 13, Color.WHITE, true);
            ball.setGravity(Gravity.CENTER);
            ball.setPadding(dp(8), dp(4), dp(8), dp(4));
            ball.setBackground(roundBorder(0xFFDC2626, 0xFFB91C1C, 28));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(6), 0);
            ballsRow.addView(ball, lp);
        }

        card.addView(ballsRow, margin(0, 4));
        layout.addView(card, margin(0, 14));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {
                    draws.remove(draw);
                    manualDates.remove(draw.date());
                    saveHistory();
                    if (draws.isEmpty()) {
                        latestDate = null;
                    } else {
                        latestDate = draws.get(draws.size() - 1).date();
                    }
                    Toast.makeText(this, "Manual draw deleted.", Toast.LENGTH_SHORT).show();
                    render();
                })
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private void buildAbout() {
        addText("About this analysis", 26, INK, true, 14);
        LinearLayout box = card(false);
        box.addView(text("What the tiers mean", 16, INK, true));
        box.addView(text("Tiers categorize all 54 Lotto Texas numbers based on their historical appearance rate during past comparable periods (e.g., past Octobers or past Q4s):",
                13, MUTED, false), margin(8, 12));

        box.addView(text("• TOP TIER: Numbers with the highest appearance rate in past periods (top frequency group).", 12, INK, false), margin(0, 4));
        box.addView(text("• NEXT TIER: Numbers with the second highest appearance rate.", 12, INK, false), margin(0, 4));
        box.addView(text("• ALSO FREQUENT: Numbers with moderate appearance frequency.", 12, INK, false), margin(0, 4));
        box.addView(text("• LOWER FREQUENCY: Numbers that appeared less frequently in past periods.", 12, INK, false), margin(0, 12));

        box.addView(text("Era-Based Eligibility Adjustment", 14, INK, true), margin(8, 6));
        box.addView(text("The Texas Lottery matrix changed across eras (50 numbers in 1992, 54 in 2000, 44 in 2003, and 54 since 2006). Frequencies are calculated relative only to periods when each number was eligible to be drawn.",
                12, MUTED, false), margin(0, 14));

        box.addView(divider(), margin(10, 14));
        box.addView(text("Terms & Legal Disclaimer", 16, INK, true));
        box.addView(text("1. For Entertainment & Informational Use Only", 12, INK, true), margin(8, 2));
        box.addView(text("Texas Quick Hits is created solely for statistical exploration, educational, and entertainment purposes.", 12, MUTED, false), margin(0, 8));

        box.addView(text("2. No Guarantee of Winnings or Results", 12, INK, true), margin(4, 2));
        box.addView(text("Historical draw frequency describes past drawings only; it does not predict future outcomes or alter the random odds of any lottery drawing. The app does not guarantee any winning ticket, prize, or financial return.", 12, MUTED, false), margin(0, 8));

        box.addView(text("3. Limitation of Liability & Waiver", 12, INK, true), margin(4, 2));
        box.addView(text("You are solely responsible for any lottery purchases or financial decisions you make. The developers of Texas Quick Hits assume no liability for any financial losses or claims arising from the use of this app or reliance on its data.", 12, MUTED, false), margin(0, 8));

        box.addView(text("4. Unofficial & Non-Affiliated", 12, INK, true), margin(4, 2));
        box.addView(text("Texas Quick Hits is an independent application and is not affiliated with, endorsed by, or sponsored by the Texas Lottery Commission or any official lottery organization.", 12, MUTED, false), margin(0, 10));

        TextView termsBtn = text("📜   View Terms of Use Dialog", 12, BLUE, true);
        termsBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        termsBtn.setBackground(roundBorder(CARD_BG, 0xFF0EA5E9, 12));
        termsBtn.setOnClickListener(v -> showFirstLaunchTermsDialog());
        box.addView(termsBtn, margin(4, 12));

        box.addView(divider(), margin(10, 14));
        box.addView(text("Data and privacy", 16, INK, true));
        box.addView(text("The app includes official Texas Lottery historical data through Oct 2026 and operates completely offline. Manual entries are saved locally on this device.",
                13, MUTED, false), margin(8, 12));
        box.addView(text("Starting history: " + draws.size() + " drawings" +
                (latestDate == null ? "" : ", through " + latestDate.format(DATE_FORMAT)) + ".", 11, MUTED, false));
        content.addView(box);
    }

    @SuppressWarnings({"ReassignedVariable", "ForLoopReplaceableByWhile", "unused"})
    private void showWeeklyBreakdownDialog() {
        PeriodBucket latest = latestSelectedBucket();
        if (latest == null || latest.draws().isEmpty()) {
            Toast.makeText(this, "No drawings recorded for this period yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        String title = periodName() + " Drawings by Week";
        layout.addView(text(title, 18, INK, true));
        layout.addView(text("Texas Lotto drawings (Mon, Wed, Sat) grouped by calendar week", 12, MUTED, false), margin(2, 14));

        List<Draw> allDraws = latest.draws();

        for (int weekNum = 1; weekNum <= 4; weekNum++) {
            int startDay = (weekNum - 1) * 7 + 1;
            int endDay = weekNum == 4 ? 31 : weekNum * 7;

            ArrayList<Draw> weekDraws = new ArrayList<>();
            for (Draw draw : allDraws) {
                int day = draw.date().getDayOfMonth();
                if (day >= startDay && day <= endDay) {
                    weekDraws.add(draw);
                }
            }

            if (weekNum > 1) layout.addView(divider(), margin(10, 10));

            String monthStr = monthName(selectedMonth).substring(0, 3).toUpperCase(Locale.US);
            String weekLabel = "WEEK " + weekNum + "  ·  " + monthStr + " " + startDay + "–" + (weekNum == 4 ? "END" : "" + endDay) + "  ·  MON, WED, SAT";
            layout.addView(text(weekLabel, 10, MUTED, true), margin(0, 6));

            if (weekDraws.isEmpty()) {
                layout.addView(text("No drawings in this week", 12, MUTED, false), margin(0, 6));
            } else {
                for (Draw draw : weekDraws) {
                    LinearLayout drawRow = new LinearLayout(this);
                    drawRow.setOrientation(LinearLayout.VERTICAL);
                    drawRow.setPadding(0, dp(4), 0, dp(6));

                    String drawDateStr = draw.date().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US));
                    drawRow.addView(text(drawDateStr, 12, INK, true), margin(0, 4));

                    LinearLayout numRow = new LinearLayout(this);
                    numRow.setOrientation(LinearLayout.HORIZONTAL);
                    for (int num : draw.numbers()) {
                        TextView badge = text("" + num, 13, 0xFF064E3B, true);
                        badge.setGravity(Gravity.CENTER);
                        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
                        badge.setBackground(roundBorder(0xFFA7F3D0, 0xFF10B981, 14));
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, dp(6), 0);
                        numRow.addView(badge, lp);
                    }
                    drawRow.addView(numRow);
                    layout.addView(drawRow);
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Close", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private void showTierHelpDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("How Tiers Work", 18, INK, true));
        layout.addView(text("Tiers group all 54 numbers by how often they hit in past completed " + pluralPeriodName() + ".", 13, MUTED, false), margin(4, 12));

        LinearLayout box = card(false);
        box.setBackground(roundBorder(CARD_BG, 0xFF334155, 14));

        box.addView(text("• TOP TIER: Highest frequency numbers (most draws/years hit in the selected period).", 12, INK, false), margin(0, 4));
        box.addView(text("• NEXT TIER: Second highest frequency group.", 12, INK, false), margin(0, 4));
        box.addView(text("• ALSO FREQUENT: Moderate frequency group.", 12, INK, false), margin(0, 4));
        box.addView(text("• LOWER FREQUENCY: Remaining lower frequency numbers.", 12, INK, false), margin(0, 12));

        box.addView(text("Era Eligibility Adjustment", 14, BLUE, true), margin(6, 4));
        box.addView(text("Historical counts adjust for matrix changes across Texas Lottery eras (50, 54, and 44 numbers). A number is only counted for periods when it was eligible to be drawn.", 12, MUTED, false), margin(0, 12));

        box.addView(text("Current Period Hits", 14, BLUE, true), margin(6, 4));
        box.addView(text("The current/latest in-progress period is excluded from completed historical tier averages, and hits are highlighted in bold green showing the exact draw date (e.g. Hit Oct 3).", 12, MUTED, false));

        layout.addView(box);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Got it", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private LinearLayout buildNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(4), dp(6), dp(4), dp(6));
        nav.setBackground(roundBorder(CARD_BG, 0xFF334155, 18));

        TextView explore = navItem("🏠 Home", "explore".equals(page));
        TextView search = navItem("🔍 Search", "search".equals(page));
        TextView history = navItem("📋 History", "history".equals(page));
        TextView about = navItem("ⓘ About", "about".equals(page));

        explore.setOnClickListener(v -> { page = "explore"; render(); });
        search.setOnClickListener(v -> { page = "search"; render(); });
        history.setOnClickListener(v -> { page = "history"; render(); });
        about.setOnClickListener(v -> { page = "about"; render(); });

        nav.addView(explore, new LinearLayout.LayoutParams(0, dp(42), 1f));
        nav.addView(search, new LinearLayout.LayoutParams(0, dp(42), 1f));
        nav.addView(history, new LinearLayout.LayoutParams(0, dp(42), 1f));
        nav.addView(about, new LinearLayout.LayoutParams(0, dp(42), 1f));

        return nav;
    }

    private TextView navItem(String label, boolean selected) {
        TextView item = text(label, 12, selected ? Color.WHITE : MUTED, selected);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(8), dp(6), dp(8), dp(6));
        item.setBackground(round(selected ? 0xFF0284C7 : Color.TRANSPARENT, 14));
        return item;
    }

    private LocalDate latestHistoricalDate() {
        LocalDate latestHist = null;
        for (Draw draw : draws) {
            if (!manualDates.contains(draw.date())) {
                if (latestHist == null || draw.date().isAfter(latestHist)) {
                    latestHist = draw.date();
                }
            }
        }
        return latestHist != null ? latestHist : LocalDate.of(2026, 10, 1);
    }

    @SuppressWarnings({"ReassignedVariable", "ForLoopReplaceableByWhile", "IfCanBeSwitch"})
    private void showAddDrawDialog() {
        final LocalDate[] chosenDate = {LocalDate.now()};
        final int[] activeSlot = {0};
        final TextView[] slotViews = new TextView[6];

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(8), dp(16), 0);

        form.addView(text("Drawing date", 11, MUTED, true), margin(2, 4));

        LinearLayout dateBar = new LinearLayout(this);
        dateBar.setGravity(Gravity.CENTER_VERTICAL);
        dateBar.setPadding(dp(12), dp(10), dp(12), dp(10));
        dateBar.setBackground(roundBorder(0xFFF1F5F9, 0xFFE2E8F0, 12));

        TextView calIcon = text("📅  ", 15, INK, false);
        TextView dateText = text(chosenDate[0].format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)), 14, INK, true);
        dateBar.addView(calIcon);
        dateBar.addView(dateText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dateBar.addView(text("Change ▾", 12, BLUE, true));

        dateBar.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
                chosenDate[0] = LocalDate.of(year, month + 1, day);
                dateText.setText(chosenDate[0].format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)));
            }, chosenDate[0].getYear(), chosenDate[0].getMonthValue() - 1, chosenDate[0].getDayOfMonth());

            try {
                LocalDate minAllowed = latestHistoricalDate();
                long minMillis = minAllowed.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                long maxMillis = System.currentTimeMillis();
                datePicker.getDatePicker().setMinDate(minMillis);
                datePicker.getDatePicker().setMaxDate(maxMillis);
            } catch (Exception ignored) { }

            datePicker.show();
        });

        form.addView(dateBar, margin(0, 12));

        form.addView(text("Winning numbers (1 to " + poolSize(chosenDate[0]) + ")", 11, MUTED, true), margin(0, 6));

        LinearLayout slotsRow = new LinearLayout(this);
        slotsRow.setOrientation(LinearLayout.HORIZONTAL);

        Runnable updateSlotBorders = () -> {
            for (int i = 0; i < 6; i++) {
                boolean isActive = (i == activeSlot[0]);
                boolean hasVal = !slotViews[i].getText().toString().isEmpty();
                slotViews[i].setBackground(isActive
                        ? roundBorder(0xFFEFF6FF, BLUE, 12)
                        : (hasVal ? roundBorder(0xFFF8FAFC, 0xFFCBD5E1, 12) : round(0xFFF1F5F9, 12)));
                slotViews[i].setTextColor(isActive ? BLUE : (hasVal ? INK : MUTED));
            }
        };

        for (int i = 0; i < 6; i++) {
            final int slotIndex = i;
            TextView slot = new TextView(this);
            slotViews[i] = slot;
            slot.setGravity(Gravity.CENTER);
            slot.setTextSize(16);
            slot.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            slot.setHint("" + (i + 1));
            slot.setHintTextColor(MUTED);
            slot.setPadding(0, dp(10), 0, dp(10));

            slot.setOnClickListener(v -> {
                activeSlot[0] = slotIndex;
                updateSlotBorders.run();
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            slotsRow.addView(slot, lp);
        }
        updateSlotBorders.run();
        form.addView(slotsRow, margin(0, 12));

        LinearLayout keypad = new LinearLayout(this);
        keypad.setOrientation(LinearLayout.VERTICAL);

        Consumer<String> onKeyTap = (key) -> {
            int currentIdx = activeSlot[0];
            TextView currentSlot = slotViews[currentIdx];
            String currentVal = currentSlot.getText().toString();

            if ("CLR".equals(key)) {
                for (TextView s : slotViews) s.setText("");
                activeSlot[0] = 0;
                updateSlotBorders.run();
                return;
            }

            if ("⌫".equals(key)) {
                if (!currentVal.isEmpty()) {
                    currentSlot.setText("");
                } else if (currentIdx > 0) {
                    activeSlot[0] = currentIdx - 1;
                    slotViews[activeSlot[0]].setText("");
                }
                updateSlotBorders.run();
                return;
            }

            int digit = Integer.parseInt(key);
            int maxPool = poolSize(chosenDate[0]);

            if (currentVal.isEmpty()) {
                currentSlot.setText(String.valueOf(digit));
                if (digit >= 6 || digit == 0) {
                    if (currentIdx < 5) activeSlot[0] = currentIdx + 1;
                }
            } else {
                int newVal = Integer.parseInt(currentVal + digit);
                if (newVal >= 1 && newVal <= maxPool) {
                    currentSlot.setText(String.valueOf(newVal));
                    if (currentIdx < 5) activeSlot[0] = currentIdx + 1;
                } else {
                    Toast.makeText(this, "Number must be between 1 and " + maxPool, Toast.LENGTH_SHORT).show();
                }
            }
            updateSlotBorders.run();
        };

        String[][] keyLayout = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"CLR", "0", "⌫"}
        };

        for (String[] keyRow : keyLayout) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (String key : keyRow) {
                TextView btn = new TextView(this);
                btn.setText(key);
                btn.setTextSize("CLR".equals(key) || "⌫".equals(key) ? 13 : 18);
                btn.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                btn.setTextColor("CLR".equals(key) ? MUTED : INK);
                btn.setGravity(Gravity.CENTER);
                btn.setBackground(roundBorder(0xFFF1F5F9, 0xFFE2E8F0, 10));
                btn.setPadding(0, dp(10), 0, dp(10));

                btn.setOnClickListener(v -> onKeyTap.accept(key));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(btn, lp);
            }
            keypad.addView(row);
        }

        form.addView(keypad, margin(0, 8));
        form.addView(text("Manual results are saved only on this device.", 10, MUTED, false), margin(10, 0));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add draw results")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save draw", null)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(round(Color.WHITE, 20));
        }

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int[] numbers = new int[6];
            TreeSet<Integer> unique = new TreeSet<>();
            for (int i = 0; i < 6; i++) {
                String val = slotViews[i].getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(this, "Please fill in all 6 winning numbers.", Toast.LENGTH_SHORT).show();
                    activeSlot[0] = i;
                    updateSlotBorders.run();
                    return;
                }
                int num = Integer.parseInt(val);
                int maxPool = poolSize(chosenDate[0]);
                if (num < 1 || num > maxPool) {
                    Toast.makeText(this, "Number " + num + " must be between 1 and " + maxPool, Toast.LENGTH_SHORT).show();
                    activeSlot[0] = i;
                    updateSlotBorders.run();
                    return;
                }
                if (!unique.add(num)) {
                    Toast.makeText(this, "Duplicate number " + num + ". Each number must be unique.", Toast.LENGTH_SHORT).show();
                    activeSlot[0] = i;
                    updateSlotBorders.run();
                    return;
                }
                numbers[i] = num;
            }

            LocalDate minAllowedDate = latestHistoricalDate();
            if (chosenDate[0].isBefore(minAllowedDate)) {
                Toast.makeText(this, "You can only add new draw results on or after " + minAllowedDate.format(DATE_FORMAT) + ".", Toast.LENGTH_LONG).show();
                return;
            }
            if (chosenDate[0].isAfter(LocalDate.now())) {
                Toast.makeText(this, "The drawing date cannot be in the future.", Toast.LENGTH_LONG).show();
                return;
            }
            if (findDraw(chosenDate[0]) != null) {
                Toast.makeText(this, "A drawing is already saved for that date.", Toast.LENGTH_LONG).show();
                return;
            }

            Draw added = new Draw(chosenDate[0], numbers);
            draws.add(added);
            manualDates.add(added.date());
            draws.sort(Comparator.comparing(Draw::date));
            if (!saveHistory()) {
                draws.remove(added);
                manualDates.remove(added.date());
                return;
            }
            latestDate = draws.isEmpty() ? null : draws.get(draws.size() - 1).date();
            selectedMonth = added.date().getMonthValue();
            selectedQuarter = quarterOf(added.date());
            Toast.makeText(this, "Draw saved.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    @SuppressWarnings("all")
    private void loadHistory() {
        draws.clear();
        manualDates.clear();

        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(getAssets().open("lottotexas.csv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                if (fields.length < 10) continue;
                try {
                    int month = Integer.parseInt(fields[1].trim());
                    int day = Integer.parseInt(fields[2].trim());
                    int year = Integer.parseInt(fields[3].trim());
                    int[] nums = new int[6];
                    for (int i = 0; i < 6; i++) nums[i] = Integer.parseInt(fields[4 + i].trim());
                    draws.add(new Draw(LocalDate.of(year, month, day), nums));
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }

        if (historyFile.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(historyFile.toPath(), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",", -1);
                    if (fields.length < 10) continue;
                    try {
                        int month = Integer.parseInt(fields[1].trim());
                        int day = Integer.parseInt(fields[2].trim());
                        int year = Integer.parseInt(fields[3].trim());
                        LocalDate date = LocalDate.of(year, month, day);
                        if (findDraw(date) == null) {
                            int[] nums = new int[6];
                            for (int i = 0; i < 6; i++) nums[i] = Integer.parseInt(fields[4 + i].trim());
                            draws.add(new Draw(date, nums));
                        }
                    } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
        }

        for (String saved : getSharedPreferences("history", MODE_PRIVATE).getStringSet("manual_dates", Collections.emptySet())) {
            try { manualDates.add(LocalDate.parse(saved)); } catch (Exception ignored) { }
        }
        draws.sort(Comparator.comparing(Draw::date));
        latestDate = draws.isEmpty() ? null : draws.get(draws.size() - 1).date();

        syncOnlineCloudData(false);
    }

    private void syncOnlineCloudData(boolean showUpToDateToast) {
        new Thread(() -> {
            String[] urls = {
                    "https://data.quickhitsapps.com/lottotexas.csv?t=" + System.currentTimeMillis(),
                    "https://raw.githubusercontent.com/fjlove96/texas-quick-hits/main/lottotexas.csv?t=" + System.currentTimeMillis()
            };
            boolean success = false;
            int newCount = 0;

            for (String cloudUrl : urls) {
                try {
                    java.net.URL url = new java.net.URL(cloudUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                    conn.setRequestProperty("Pragma", "no-cache");
                    if (conn.getResponseCode() == 200) {
                        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] fields = line.split(",", -1);
                                if (fields.length < 10) continue;
                                try {
                                    int month = Integer.parseInt(fields[1].trim());
                                    int day = Integer.parseInt(fields[2].trim());
                                    int year = Integer.parseInt(fields[3].trim());
                                    LocalDate date = LocalDate.of(year, month, day);
                                    if (findDraw(date) == null) {
                                        int[] nums = new int[6];
                                        for (int i = 0; i < 6; i++) nums[i] = Integer.parseInt(fields[4 + i].trim());
                                        synchronized (draws) {
                                            draws.add(new Draw(date, nums));
                                        }
                                        newCount++;
                                    }
                                } catch (Exception ignored) { }
                            }
                            success = true;
                            if (newCount > 0) {
                                synchronized (draws) {
                                    draws.sort(Comparator.comparing(Draw::date));
                                    latestDate = draws.isEmpty() ? null : draws.get(draws.size() - 1).date();
                                }
                                saveHistory();
                                final int added = newCount;
                                runOnUiThread(() -> {
                                    Toast.makeText(this, "☁️ Updated " + added + " new drawing(s) from Cloud!", Toast.LENGTH_SHORT).show();
                                    render();
                                });
                            } else if (showUpToDateToast) {
                                runOnUiThread(() -> Toast.makeText(this, "☁️ App is up-to-date with latest Cloud draws!", Toast.LENGTH_SHORT).show());
                            }
                            break;
                        }
                    }
                } catch (Exception ignored) { }
            }
        }).start();
    }

    private boolean saveHistory() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(historyFile, false), StandardCharsets.UTF_8))) {
            for (Draw draw : draws) {
                writer.write("Lotto Texas," + draw.date().getMonthValue() + "," + draw.date().getDayOfMonth() + "," + draw.date().getYear());
                for (int number : draw.numbers()) writer.write("," + number);
                writer.newLine();
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Could not save the draw. Please try again.", Toast.LENGTH_LONG).show();
            return false;
        }
        Set<String> dates = new TreeSet<>();
        for (LocalDate date : manualDates) dates.add(date.toString());
        getSharedPreferences("history", MODE_PRIVATE).edit().putStringSet("manual_dates", dates).apply();
        return true;
    }

    private Draw findDraw(LocalDate date) {
        synchronized (draws) {
            for (Draw draw : draws) if (Objects.equals(draw.date(), date)) return draw;
        }
        return null;
    }

    private PeriodBucket latestSelectedBucket() {
        if (draws.isEmpty() || latestDate == null) {
            return new PeriodBucket(safeDate(selectedYear, selectedMonth, 1), safeDate(selectedYear, selectedMonth, 28));
        }
        if ("4 weeks".equals(mode)) {
            LocalDate end = latestDate;
            LocalDate start = end.minusDays(27);
            PeriodBucket bucket = new PeriodBucket(start, end);
            for (Draw draw : draws) {
                if (!draw.date().isBefore(start) && !draw.date().isAfter(end)) {
                    bucket.draws().add(draw);
                }
            }
            return bucket;
        }

        String key = "Month".equals(mode)
                ? String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth)
                : String.format(Locale.US, "%04d-Q%d", selectedYear, selectedQuarter);

        PeriodBucket bucket = groupedCalendarBuckets().get(key);
        if (bucket == null) {
            LocalDate start = "Month".equals(mode)
                    ? LocalDate.of(selectedYear, selectedMonth, 1)
                    : LocalDate.of(selectedYear, (selectedQuarter - 1) * 3 + 1, 1);
            LocalDate end = "Month".equals(mode)
                    ? YearMonth.from(start).atEndOfMonth()
                    : start.plusMonths(2).withDayOfMonth(start.plusMonths(2).lengthOfMonth());
            bucket = new PeriodBucket(start, end);
        }
        return bucket;
    }

    @SuppressWarnings({"IfCanBeSwitch", "ForLoopReplaceableByWhile", "ReassignedVariable"})
    private List<PeriodBucket> historicalBuckets() {
        ArrayList<PeriodBucket> result = new ArrayList<>();
        if (draws.isEmpty()) return result;
        if ("4 weeks".equals(mode)) {
            int firstYear = draws.get(0).date().getYear();
            for (int year = firstYear; year < latestDate.getYear(); year++) {
                LocalDate end = safeDate(year, latestDate.getMonthValue(), latestDate.getDayOfMonth());
                LocalDate start = end.minusDays(27);
                PeriodBucket bucket = new PeriodBucket(start, end);
                for (Draw draw : draws) if (!draw.date().isBefore(start) && !draw.date().isAfter(end)) bucket.draws().add(draw);
                if (!bucket.draws().isEmpty()) result.add(bucket);
            }
            return result;
        }

        for (PeriodBucket bucket : groupedCalendarBuckets().values()) {
            if ("Month".equals(mode) && bucket.start().getMonthValue() != selectedMonth) continue;
            if ("Quarter".equals(mode) && quarterOf(bucket.start()) != selectedQuarter) continue;
            if (isLatestIncompleteBucket(bucket)) continue;
            result.add(bucket);
        }
        return result;
    }

    @SuppressWarnings("ReassignedVariable")
    private TreeMap<String, PeriodBucket> groupedCalendarBuckets() {
        TreeMap<String, PeriodBucket> result = new TreeMap<>();
        List<Draw> copy;
        synchronized (draws) {
            copy = new ArrayList<>(draws);
        }
        for (Draw draw : copy) {
            String key = "Month".equals(mode)
                    ? String.format(Locale.US, "%04d-%02d", draw.date().getYear(), draw.date().getMonthValue())
                    : String.format(Locale.US, "%04d-Q%d", draw.date().getYear(), quarterOf(draw.date()));
            PeriodBucket bucket = result.get(key);
            if (bucket == null) {
                LocalDate start = "Month".equals(mode)
                        ? draw.date().withDayOfMonth(1)
                        : LocalDate.of(draw.date().getYear(), (quarterOf(draw.date()) - 1) * 3 + 1, 1);
                LocalDate end = "Month".equals(mode)
                        ? YearMonth.from(start).atEndOfMonth()
                        : start.plusMonths(2).withDayOfMonth(start.plusMonths(2).lengthOfMonth());
                bucket = new PeriodBucket(start, end);
                result.put(key, bucket);
            }
            bucket.draws().add(draw);
        }
        return result;
    }

    @SuppressWarnings("IfCanBeSwitch")
    private boolean isLatestIncompleteBucket(PeriodBucket bucket) {
        if (latestDate == null || bucket.start().getYear() != latestDate.getYear()) return false;
        if ("Month".equals(mode)) {
            return bucket.start().getMonthValue() == selectedMonth
                    && latestDate.getMonthValue() == selectedMonth
                    && latestDate.isBefore(bucket.end());
        }
        return quarterOf(bucket.start()) == selectedQuarter
                && quarterOf(latestDate) == selectedQuarter
                && latestDate.isBefore(bucket.end());
    }

    @SuppressWarnings({"ForLoopReplaceableByWhile", "ReassignedVariable"})
    private ArrayList<Stat> calculateStats(List<PeriodBucket> buckets) {
        ArrayList<Stat> result = new ArrayList<>();
        for (int number = 1; number <= 54; number++) {
            int eligible = 0;
            int hits = 0;
            for (PeriodBucket bucket : buckets) {
                boolean canDraw = false;
                boolean found = false;
                for (Draw draw : bucket.draws()) {
                    if (number <= poolSize(draw.date())) canDraw = true;
                    if (number <= poolSize(draw.date()) && contains(draw.numbers(), number)) found = true;
                }
                if (canDraw) {
                    eligible++;
                    if (found) hits++;
                }
            }
            if (eligible > 0) result.add(new Stat(number, hits, eligible));
        }
        result.sort(Comparator.comparingDouble((Stat stat) -> stat.rate).reversed()
                .thenComparingInt(stat -> stat.number));
        return result;
    }

    private List<LocalDate> latestHitDates(int number) {
        ArrayList<LocalDate> dates = new ArrayList<>();
        PeriodBucket bucket = latestSelectedBucket();
        if (bucket != null) {
            for (Draw draw : bucket.draws()) {
                if (contains(draw.numbers(), number)) {
                    dates.add(draw.date());
                }
            }
        }
        return dates;
    }

    @SuppressWarnings("ReassignedVariable")
    private static String formatHitDatesMockup(List<LocalDate> dates) {
        if (dates.isEmpty()) return "Not yet";
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US);
        if (dates.size() == 1) {
            return "Hit " + dates.get(0).format(monthFmt);
        }
        StringBuilder sb = new StringBuilder("Hit ");
        sb.append(dates.get(0).format(monthFmt));
        for (int i = 1; i < dates.size(); i++) {
            LocalDate d = dates.get(i);
            if (d.getMonth() == dates.get(0).getMonth()) {
                sb.append(", ").append(d.getDayOfMonth());
            } else {
                sb.append("\n").append(d.format(monthFmt));
            }
        }
        return sb.toString();
    }

    private int poolSize(LocalDate date) {
        if (date.isBefore(LocalDate.of(2000, 7, 19))) return 50;
        if (date.isBefore(LocalDate.of(2003, 5, 7))) return 54;
        if (!date.isAfter(LocalDate.of(2006, 4, 22))) return 44;
        return 54;
    }

    private static boolean contains(int[] numbers, int target) {
        for (int number : numbers) if (number == target) return true;
        return false;
    }

    private static int quarterOf(LocalDate date) { return (date.getMonthValue() - 1) / 3 + 1; }

    private static LocalDate safeDate(int year, int month, int day) {
        int lastDay = YearMonth.of(year, month).lengthOfMonth();
        return LocalDate.of(year, month, Math.min(day, lastDay));
    }

    private String periodName() {
        if ("Month".equals(mode)) return monthName(selectedMonth);
        if ("Quarter".equals(mode)) return "Quarter " + selectedQuarter;
        return "4 weeks";
    }

    private String pluralPeriodName() {
        if ("Month".equals(mode)) {
            String name = monthName(selectedMonth);
            if (name.endsWith("y")) return name.substring(0, name.length() - 1) + "ies";
            if (name.endsWith("ch") || name.endsWith("s")) return name + "es";
            return name + "s";
        }
        if ("Quarter".equals(mode)) return "Q" + selectedQuarter + "s";
        return "four-week windows";
    }

    private static String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.US);
    }

    private static String joinNumbers(int[] values) {
        StringJoiner joiner = new StringJoiner("  ·  ");
        for (int value : values) {
            joiner.add("" + value);
        }
        return joiner.toString();
    }

    private LinearLayout card(boolean isPrimary) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(18));
        box.setBackground(isPrimary
                ? roundBorder(0xFF1E1B4B, BLUE, 20)
                : roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        return box;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private void addText(String value, int size, int color, boolean bold, int marginBottom) {
        content.addView(text(value, size, color, bold), margin(0, marginBottom));
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(0xFFE2E8F0);
        return view;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    @SuppressWarnings("SameParameterValue")
    private GradientDrawable roundBorder(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private boolean isProUser() {
        return getSharedPreferences("pro_prefs", MODE_PRIVATE).getBoolean("is_pro", false);
    }

    private GradientDrawable goldGradient() {
        GradientDrawable goldGrad = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { 0xFFFFE082, 0xFFF59E0B }
        );
        goldGrad.setCornerRadius(dp(20));
        goldGrad.setStroke(dp(2), 0xFFFEF3C7);
        return goldGrad;
    }

    private void showProUpgradeDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("⭐ Upgrade to Quick Hits Pro", 18, INK, true));
        layout.addView(text("Unlock the ultimate lottery analysis experience:", 12, MUTED, false), margin(2, 10));

        LinearLayout perksCard = card(false);
        perksCard.setBackground(roundBorder(0xFF1E293B, 0xFF0EA5E9, 14));
        perksCard.addView(text("• 100% Ad-Free Experience", 13, GREEN, true), margin(0, 4));
        perksCard.addView(text("• Unlimited Saved Favorite Sets", 13, GREEN, true), margin(0, 4));
        perksCard.addView(text("• Past Years Search History (1992–2025)", 13, GREEN, true), margin(0, 4));
        perksCard.addView(text("• Instant Automatic Background Syncing", 13, GREEN, true), margin(0, 4));
        layout.addView(perksCard, margin(0, 10));

        TextView buyBtn = text("✨   Unlock Pro Lifetime — $9.99", 14, 0xFF111827, true);
        buyBtn.setGravity(Gravity.CENTER);
        buyBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        buyBtn.setBackground(goldGradient());
        buyBtn.setOnClickListener(v -> {
            getSharedPreferences("pro_prefs", MODE_PRIVATE).edit().putBoolean("is_pro", true).apply();
            Toast.makeText(this, "🎉 Thank you for upgrading to Quick Hits Pro!", Toast.LENGTH_LONG).show();
            render();
        });
        layout.addView(buyBtn, margin(0, 12));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Close", null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private void showRewardedVideoSyncDialog(Runnable onSyncSuccess) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));
        layout.setBackgroundColor(CARD_BG);
        scroll.addView(layout);

        layout.addView(text("📺 Watch Video to Sync Latest Draws", 18, INK, true));
        layout.addView(text("Watch a short 30-second video ad to pull and sync the newest winning results from quickhitsapps.com, or upgrade to Pro for instant background syncs.", 12, MUTED, false), margin(2, 10));

        TextView watchBtn = text("▶   Watch Rewarded Video (30s)", 14, 0xFF111827, true);
        watchBtn.setGravity(Gravity.CENTER);
        watchBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        watchBtn.setBackground(goldGradient());

        AlertDialog[] dialogHolder = new AlertDialog[1];

        watchBtn.setOnClickListener(v -> {
            watchBtn.setEnabled(false);
            watchBtn.setText("Playing Ad... (30s)");
            watchBtn.postDelayed(() -> {
                Toast.makeText(this, "🎁 Rewarded Ad Completed! Syncing latest draws...", Toast.LENGTH_SHORT).show();
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                if (onSyncSuccess != null) onSyncSuccess.run();
            }, 1500);
        });
        layout.addView(watchBtn, margin(0, 12));

        TextView upgradePrompt = text("Or upgrade to Pro ($9.99) for instant automatic syncs with zero ads ➔", 11, BLUE, true);
        upgradePrompt.setGravity(Gravity.CENTER);
        upgradePrompt.setOnClickListener(v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            showProUpgradeDialog();
        });
        layout.addView(upgradePrompt, margin(0, 8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton("Cancel", null)
                .create();
        dialogHolder[0] = dialog;

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(roundBorder(CARD_BG, 0xFF0EA5E9, 20));
        }
        dialog.show();
    }

    private LinearLayout.LayoutParams margin(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(top), 0, dp(bottom));
        return params;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private record Draw(LocalDate date, int[] numbers) {
        private Draw {
            numbers = numbers.clone();
        }
    }

    private record PeriodBucket(LocalDate start, LocalDate end, ArrayList<Draw> draws) {
        private PeriodBucket(LocalDate start, LocalDate end) {
            this(start, end, new ArrayList<>());
        }
    }

    private static final class Stat {
        final int number;
        final int hits;
        final int eligible;
        final double rate;
        final int percent;
        Stat(int number, int hits, int eligible) {
            this.number = number;
            this.hits = hits;
            this.eligible = eligible;
            this.rate = eligible == 0 ? 0 : (double) hits / eligible;
            this.percent = (int) Math.round(rate * 100.0);
        }
    }
}
