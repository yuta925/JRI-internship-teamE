package jp.co.jri.internship.fintech_sample1;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TapPagerAdapter extends FragmentStateAdapter {

    private final int permissionLevel;

    public TapPagerAdapter(FragmentActivity activity, int permissionLevel) {
        super(activity);
        this.permissionLevel = permissionLevel;
    }

    // 指定されたタブの位置（position）に対応するタブページ（Fragment）を作成する
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // レベル3の場合、インデックス2（取引履歴）を飛ばして設定画面にする
        if (permissionLevel == 3) {
            switch (position) {
                case 0: return new HomeFragment();
                case 1: return new AnalysisFragment();
                default: return new SettingsFragment();
            }
        }

        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new AnalysisFragment();
            case 2:
                return new TransactionHistoryFragment();
            default:
                return new SettingsFragment();
        }
    }

    // タブの数を返す
    @Override
    public int getItemCount() {
        // レベル3なら「取引履歴」を抜いた3つ、それ以外は4つ
        return (permissionLevel == 3) ? 3 : 4;
    }
}
