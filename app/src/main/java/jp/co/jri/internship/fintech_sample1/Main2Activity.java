package jp.co.jri.internship.fintech_sample1;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class Main2Activity extends AppCompatActivity {

    private static final String[] TAB_TITLES = {"ホーム", "分析", "取引履歴"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // レイアウト（activity_main2.xml）を表示する
        setContentView(R.layout.activity_main2);

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
    }
}
