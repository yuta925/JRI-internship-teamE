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
    public static final String EXTRA_PERMISSION_LEVEL = "extra_permission_level";
    public static final String EXTRA_USER_ID = "extra_user_id";

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

        // 共通のActionBarは非表示にする
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 権限レベルの取得
        int permissionLevel = getIntent().getIntExtra(EXTRA_PERMISSION_LEVEL, 1);

        // アダプタ(TapPagerAdapter)を用いてタブ切り替え時のViewPager2の内容表示を制御する
        ViewPager2 pager = findViewById(R.id.pager);
        TapPagerAdapter adapter = new TapPagerAdapter(this, permissionLevel);
        pager.setAdapter(adapter);
        pager.setUserInputEnabled(false); // スワイプではなくボトムナビでのみ切り替える

        // ボトムナビゲーションの設定
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // レベル3のユーザーなら「取引履歴」をメニューから隠す
        if (permissionLevel == 3) {
            bottomNav.getMenu().findItem(R.id.nav_history).setVisible(false);
        }

        // ボトムナビゲーションの選択とViewPager2の同期
        bottomNav.setOnItemSelectedListener(item -> {
            int position;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                position = 0;
            } else if (itemId == R.id.nav_analysis) {
                position = 1;
            } else if (itemId == R.id.nav_history) {
                position = 2;
            } else if (itemId == R.id.nav_settings) {
                position = (permissionLevel == 3) ? 2 : 3;
            } else {
                position = 0;
            }
            pager.setCurrentItem(position, false);
            return true;
        });

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                int itemId;
                if (position == 0) {
                    itemId = R.id.nav_home;
                } else if (position == 1) {
                    itemId = R.id.nav_analysis;
                } else if (position == 2) {
                    itemId = (permissionLevel == 3) ? R.id.nav_settings : R.id.nav_history;
                } else {
                    itemId = R.id.nav_settings;
                }
                bottomNav.setSelectedItemId(itemId);
            }
        });

        showLoginStreakPopupIfNeeded();
        notifyGoalProgressOnce();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void showLoginStreakPopupIfNeeded() {
        int consecutiveLoginDays = getIntent().getIntExtra(EXTRA_CONSECUTIVE_LOGIN_DAYS, 0);
        if (consecutiveLoginDays <= 0) {
            return;
        }

        if (consecutiveLoginDays == LOGIN_STREAK_SPECIAL_365) {
            showImagePopup(R.drawable.login_bonus_365days,
                    "365日連続ログイン達成！\nおめでとうございます！");
        } else if (consecutiveLoginDays == LOGIN_STREAK_SPECIAL_100) {
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