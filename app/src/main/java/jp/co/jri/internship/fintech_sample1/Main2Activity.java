package jp.co.jri.internship.fintech_sample1;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Main2Activity extends AppCompatActivity {

    private static final String[] TAB_TITLES = {"ホーム", "分析", "取引履歴"};

    // LoginActivityから連続ログイン日数を受け取るためのIntentキー
    public static final String EXTRA_CONSECUTIVE_LOGIN_DAYS = "extra_consecutive_login_days";

    // ポップアップを表示する連続ログイン日数の間隔（5日ごと）
    private static final int LOGIN_STREAK_MILESTONE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // レイアウト（activity_main2.xml）を表示する
        setContentView(R.layout.activity_main2);

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
    }

    // 連続ログイン日数が節目(5日ごと)に達していたらお祝いポップアップを表示する
    private void showLoginStreakPopupIfNeeded() {
        int consecutiveLoginDays = getIntent().getIntExtra(EXTRA_CONSECUTIVE_LOGIN_DAYS, 0);
        if (consecutiveLoginDays > 0 && consecutiveLoginDays % LOGIN_STREAK_MILESTONE == 0) {
            new AlertDialog.Builder(this)
                    .setTitle("継続ログイン達成")
                    .setMessage(consecutiveLoginDays + "日連続ログインを達成しました！")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}
