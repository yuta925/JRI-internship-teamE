package jp.co.jri.internship.fintech_sample1;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class Main2Activity extends AppCompatActivity {

    // LoginActivityから連続ログイン日数を受け取るためのIntentキー
    public static final String EXTRA_CONSECUTIVE_LOGIN_DAYS = "extra_consecutive_login_days";

    // ポップアップを表示する連続ログイン日数の間隔（5日ごと）
    private static final int LOGIN_STREAK_MILESTONE = 5;

    // ホーム画面で設定した支出・貯金目標を通知にも使用する
    private static final String PREFS_NAME = "FintechPrefs";
    private static final String KEY_TARGET_BUDGET = "target_budget";
    private static final String KEY_TARGET_SAVINGS = "target_savings";
    private static final String GOAL_CHANNEL_ID = "goal_progress_channel";
    private static final int GOAL_NOTIFICATION_ID = 1001;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    // 特別な画像付きポップアップを表示する連続ログイン日数
    private static final int LOGIN_STREAK_SPECIAL_100 = 100;
    private static final int LOGIN_STREAK_SPECIAL_365 = 365;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // レイアウト（activity_main2.xml）を表示する
        setContentView(R.layout.activity_main2);

        requestNotificationPermissionIfNeeded();

        // 共通ヘッダー（ロゴ）とボトムナビゲーションを使うため、共通のActionBarは非表示にする
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // アダプタ(TapPagerAdapter)を用いてタブ切り替え時のViewPager2の内容表示を制御する
        ViewPager2 pager = findViewById(R.id.pager);
        TapPagerAdapter adapter = new TapPagerAdapter(this);
        pager.setAdapter(adapter);
        pager.setUserInputEnabled(false); // スワイプではなく下部ナビでのみ切り替える

        // ボトムナビゲーションの選択とViewPager2の表示ページを同期する
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            // カテゴリ詳細などのサブ画面が表示されている場合は、それらを閉じてタブを切り替える
            while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate();
            }

            int position;
            if (item.getItemId() == R.id.nav_home) {
                position = 0;
            } else if (item.getItemId() == R.id.nav_analysis) {
                position = 1;
            } else {
                position = 2;
            }
            pager.setCurrentItem(position, false);
            return true;
        });
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int itemId = position == 0 ? R.id.nav_home
                        : position == 1 ? R.id.nav_analysis
                        : R.id.nav_history;
                bottomNav.setSelectedItemId(itemId);
            }
        });

        showLoginStreakPopupIfNeeded();
        notifyGoalProgressOnce();
    }

    // Android 13以降は通知の表示にランタイム権限が必要なため、未許可なら要求する
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // 連続ログイン日数が節目に達していたらお祝いポップアップを表示する
    private void showLoginStreakPopupIfNeeded() {
        int consecutiveLoginDays = getIntent().getIntExtra(EXTRA_CONSECUTIVE_LOGIN_DAYS, 0);
        if (consecutiveLoginDays <= 0) {
            return;
        }

        if (consecutiveLoginDays == LOGIN_STREAK_SPECIAL_365) {
            // 365日達成：特別な画像でお祝い
            showImagePopup(R.drawable.login_bonus_365days,
                    "365日連続ログイン達成！\nおめでとうございます！");
        } else if (consecutiveLoginDays == LOGIN_STREAK_SPECIAL_100) {
            // 100日達成：特別な画像でお祝い
            showImagePopup(R.drawable.login_bonus_100days,
                    "100日連続ログイン達成！");
        } else if (consecutiveLoginDays % LOGIN_STREAK_MILESTONE == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("継続ログイン達成")
                    .setMessage(consecutiveLoginDays + "日連続ログインを達成しました！")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    // ログイン直後（この画面の起動時）に一度だけ、目標に対する現在の達成状況をプッシュ通知する
    private void notifyGoalProgressOnce() {
        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase_v4.txt";
        boolean localFileExists = getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(this, localFileExists);
        List<FintechData> allData = parser.fintechObjects;
        if (allData.isEmpty()) {
            return;
        }

        String currentMonth = allData.get(allData.size() - 1).getTransDate().substring(0, 7);
        int income = 0;
        int expense = 0;
        for (FintechData data : allData) {
            if (data.getTransDate().substring(0, 7).equals(currentMonth)) {
                if (data.getAmount() >= 0) {
                    income += data.getAmount();
                } else {
                    expense += -data.getAmount();
                }
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int targetBudget = prefs.getInt(KEY_TARGET_BUDGET, 0);
        int targetSavings = prefs.getInt(KEY_TARGET_SAVINGS, 0);
        int budgetRate = calculateAchievementRate(expense, targetBudget);
        int savingsRate = calculateAchievementRate(income - expense, targetSavings);

        createGoalNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // 通知権限が未許可の場合は送信しない
            return;
        }

        String message = getString(R.string.goal_notification_text, budgetRate, savingsRate);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, GOAL_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.goal_notification_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setTimeoutAfter(5000);

        NotificationManagerCompat.from(this).notify(GOAL_NOTIFICATION_ID, builder.build());
    }

    private int calculateAchievementRate(int amount, int target) {
        if (target <= 0) {
            return 0;
        }
        return Math.max(0, (int) ((amount * 100.0) / target));
    }

    private void createGoalNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    GOAL_CHANNEL_ID,
                    getString(R.string.goal_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // 画像とメッセージを表示するお祝いポップアップ
    private void showImagePopup(int drawableRes, String message) {
        int padding = (int) (24 * getResources().getDisplayMetrics().density);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(padding, padding, padding, padding);

        int maxImageSize = (int) (220 * getResources().getDisplayMetrics().density);
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(drawableRes);
        imageView.setAdjustViewBounds(true);
        imageView.setMaxWidth(maxImageSize);
        imageView.setMaxHeight(maxImageSize);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(imageParams);
        container.addView(imageView);

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(16f);
        textView.setPadding(0, padding, 0, 0);
        container.addView(textView);

        new AlertDialog.Builder(this)
                .setView(container)
                .setPositiveButton("OK", null)
                .show();
    }
}
