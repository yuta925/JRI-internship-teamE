package jp.co.jri.internship.fintech_sample1;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class Main2Activity extends AppCompatActivity {

    private static final String[] TAB_TITLES = {"ホーム", "分析", "取引履歴"};

    // LoginActivityから連続ログイン日数を受け取るためのIntentキー
    public static final String EXTRA_CONSECUTIVE_LOGIN_DAYS = "extra_consecutive_login_days";

    // ポップアップを表示する連続ログイン日数の間隔（5日ごと）
    private static final int LOGIN_STREAK_MILESTONE = 5;

    // 貯金目標（現状は固定値。目標設定機能が未実装のため月ごとの目標額として仮置きしている）
    private static final int SAVINGS_GOAL = 100000;
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

        // 各タブに独自のヘッダーがあるため、共通のActionBarは非表示にする
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // アダプタ(TapPagerAdapter)を用いてタブ切り替え時のViewPager2の内容表示を制御する
        ViewPager2 pager = findViewById(R.id.pager);
        TapPagerAdapter adapter = new TapPagerAdapter(this);
        pager.setAdapter(adapter);

        // TabLayoutとViewPager2を関連付ける（押下されたタブと内容表示を関連付ける）
        TabLayout tabs = findViewById(R.id.tab_layout);
        new TabLayoutMediator(
                tabs,
                pager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();

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
        String filename = "LocalFintechDateBase.txt";
        boolean localFileExists = getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(this, localFileExists);
        List<FintechData> allData = parser.fintechObjects;
        if (allData.isEmpty()) {
            return;
        }

        String currentMonth = allData.get(allData.size() - 1).getTransDate().substring(0, 7);
        int savings = 0;
        for (FintechData data : allData) {
            if (data.getTransDate().substring(0, 7).equals(currentMonth)) {
                savings += data.getAmount();
            }
        }

        int achievementRate = Math.min(100, Math.max(0, savings * 100 / SAVINGS_GOAL));

        createGoalNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // 通知権限が未許可の場合は送信しない
            return;
        }

        String message = getString(R.string.goal_notification_text, achievementRate);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, GOAL_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.goal_notification_title))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setTimeoutAfter(5000);

        NotificationManagerCompat.from(this).notify(GOAL_NOTIFICATION_ID, builder.build());
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
