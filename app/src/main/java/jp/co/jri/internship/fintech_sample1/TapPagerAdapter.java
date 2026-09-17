package jp.co.jri.internship.fintech_sample1;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TapPagerAdapter extends FragmentStateAdapter {

    public TapPagerAdapter(FragmentActivity activity) {
        super(activity);
    }

    // 指定されたタブの位置（position）に対応するタブページ（Fragment）を作成する
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new AnalysisFragment();
            default:
                return new TransactionHistoryFragment();
        }
    }

    // タブの数を返す
    @Override
    public int getItemCount() {
        return 3;
    }
}
