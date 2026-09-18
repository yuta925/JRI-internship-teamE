package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class HomeFragment extends Fragment {

    private static final String PREFS_NAME = "FintechPrefs";
    private static final String KEY_TARGET_BUDGET = "target_budget";
    private static final String KEY_TARGET_SAVINGS = "target_savings";

    // 貯金目標達成率に応じたキャラクター切り替えのしきい値
    private static final int SAVINGS_CHARACTER_RATE_EXCELLENT = 300;
    private static final int SAVINGS_CHARACTER_RATE_GOOD = 100;
    private static final int SAVINGS_CHARACTER_RATE_LOW = 50;

    // 支出目標達成率に応じたキャラクター切り替えのしきい値（使いすぎていないほど良いため貯金と逆向き）
    private static final int BUDGET_CHARACTER_RATE_GOOD = 50;
    private static final int BUDGET_CHARACTER_RATE_BAD = 100;
    private static final int BUDGET_CHARACTER_RATE_WORSE = 300;

    private TextView tvTargetBudget;
    private TextView tvAchievementRate;
    private ProgressBar pbAchievement;
    private ImageView ivBudgetCharacter;

    private TextView tvTargetSavings;
    private TextView tvSavingsAchievementRate;
    private ProgressBar pbSavingsAchievement;
    private ImageView ivSavingsCharacter;

    private int sumExpense = 0;
    private int currentSavings = 0;

    @SuppressLint("DefaultLocale")
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        setupLoginBonus(v);
        setupTotalPoints(v);

        // CSVから取引データを読み込む
        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase_v4.txt";
        boolean localFileExists = requireContext().getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(requireContext(), localFileExists);
        List<FintechData> allData = parser.fintechObjects;

        TextView tvTotalAssets = v.findViewById(R.id.tvTotalAssets);
        TextView tvMoM = v.findViewById(R.id.tvMoM);
        TextView tvIncome = v.findViewById(R.id.tvIncome);
        TextView tvExpense = v.findViewById(R.id.tvExpense);
        TextView tvSavings = v.findViewById(R.id.tvSavings);

        // 新しく追加したUIコンポーネント
        tvTargetBudget = v.findViewById(R.id.tvTargetBudget);
        tvAchievementRate = v.findViewById(R.id.tvAchievementRate);
        pbAchievement = v.findViewById(R.id.pbAchievement);
        ivBudgetCharacter = v.findViewById(R.id.ivBudgetCharacter);
        Button btnSetTarget = v.findViewById(R.id.btnSetTarget);

        tvTargetSavings = v.findViewById(R.id.tvTargetSavings);
        tvSavingsAchievementRate = v.findViewById(R.id.tvSavingsAchievementRate);
        pbSavingsAchievement = v.findViewById(R.id.pbSavingsAchievement);
        ivSavingsCharacter = v.findViewById(R.id.ivSavingsCharacter);
        Button btnSetTargetSavings = v.findViewById(R.id.btnSetTargetSavings);

        if (allData.isEmpty()) {
            tvTotalAssets.setText("¥0");
            tvMoM.setText("");
            tvIncome.setText("¥0");
            tvExpense.setText("¥0");
            tvSavings.setText("¥0");
            updateTargetDisplay();
            return v;
        }

        // データは日付順に並んでいる前提で、末尾が最新の取引＝現在の総資産
        FintechData latest = allData.get(allData.size() - 1);
        int totalAssets = latest.getBalance();
        String currentMonth = latest.getTransDate().substring(0, 7); // "YYYY/MM"
        String previousMonth = previousMonth(currentMonth);

        int sumIncome = 0;
        sumExpense = 0;
        Integer previousMonthEndBalance = null;

        for (FintechData data : allData) {
            String month = data.getTransDate().substring(0, 7);
            if (month.equals(currentMonth)) {
                if (data.getAmount() >= 0) {
                    sumIncome += data.getAmount();
                } else {
                    sumExpense += -data.getAmount();
                }
            }
            if (month.equals(previousMonth)) {
                // 前月内で最後に現れた残高を前月末残高とする
                previousMonthEndBalance = data.getBalance();
            }
        }

        currentSavings = sumIncome - sumExpense;

        tvTotalAssets.setText(String.format("¥%,d", totalAssets));
        tvIncome.setText(String.format("¥%,d", sumIncome));
        tvExpense.setText(String.format("¥%,d", sumExpense));
        tvSavings.setText(String.format("¥%,d", currentSavings));

        if (previousMonthEndBalance != null && previousMonthEndBalance != 0) {
            int diff = totalAssets - previousMonthEndBalance;
            double percent = (diff * 100.0) / previousMonthEndBalance;
            String arrow = diff >= 0 ? "▲" : "▼";
            String sign = diff >= 0 ? "+" : "";
            tvMoM.setText(String.format("前月比 %s%s¥%,d (%s%.1f%%)", arrow, sign, diff, sign, percent));
            tvMoM.setTextColor(getResources().getColor(
                    diff >= 0 ? R.color.finPositive : R.color.finNegative));
        } else {
            tvMoM.setText("");
        }

        // 目標額の表示と達成率の更新
        updateTargetDisplay();

        // 目標設定ボタンの処理
        btnSetTarget.setOnClickListener(view -> showTargetInputDialog());
        btnSetTargetSavings.setOnClickListener(view -> showTargetSavingsInputDialog());

        return v;
    }

    // 本日のログインボーナスを表示する（連続ログイン日数はMain2ActivityがLoginActivityから受け取ったIntentのExtraを流用）
    private void setupLoginBonus(View v) {
        int consecutiveDays = requireActivity().getIntent()
                .getIntExtra(Main2Activity.EXTRA_CONSECUTIVE_LOGIN_DAYS, 0);

        View cvLoginBonus = v.findViewById(R.id.cvLoginBonus);
        if (consecutiveDays <= 0) {
            cvLoginBonus.setVisibility(View.GONE);
            return;
        }

        int points = LoginBonusUtil.calcPoints(consecutiveDays);
        TextView tvLoginBonusMessage = v.findViewById(R.id.tvLoginBonusMessage);
        tvLoginBonusMessage.setText(getString(R.string.home_login_bonus_message, points));
        cvLoginBonus.setVisibility(View.VISIBLE);
    }

    // 総ポイント数を表示する（この機能導入前の総ポイント数は0として扱う）
    @SuppressLint("DefaultLocale")
    private void setupTotalPoints(View v) {
        int totalPoints = LoginBonusUtil.getTotalPoints(requireContext());
        TextView tvTotalPoints = v.findViewById(R.id.tvTotalPoints);
        tvTotalPoints.setText(String.format("%,dpt", totalPoints));
    }

    // 目標額の表示と達成率の計算・更新
    @SuppressLint("DefaultLocale")
    private void updateTargetDisplay() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // 出費目標の更新
        int targetBudget = prefs.getInt(KEY_TARGET_BUDGET, 0);
        tvTargetBudget.setText(String.format("¥%,d", targetBudget));

        if (targetBudget > 0) {
            int rate = (int) ((sumExpense * 100.0) / targetBudget);
            pbAchievement.setProgress(Math.min(rate, 100));
            tvAchievementRate.setText(String.format("%d%%", rate));

            if (rate > 100) {
                tvAchievementRate.setTextColor(getResources().getColor(R.color.finNegative));
            } else {
                tvAchievementRate.setTextColor(getResources().getColor(R.color.finTextPrimary));
            }

            updateBudgetCharacter(rate);
        } else {
            pbAchievement.setProgress(0);
            tvAchievementRate.setText("0%");
            tvAchievementRate.setTextColor(getResources().getColor(R.color.finTextPrimary));
            ivBudgetCharacter.setVisibility(View.GONE);
        }

        // 貯金目標の更新
        int targetSavings = prefs.getInt(KEY_TARGET_SAVINGS, 0);
        tvTargetSavings.setText(String.format("¥%,d", targetSavings));

        if (targetSavings > 0) {
            int rate = (int) ((currentSavings * 100.0) / targetSavings);
            pbSavingsAchievement.setProgress(Math.min(Math.max(rate, 0), 100));
            tvSavingsAchievementRate.setText(String.format("%d%%", rate));

            if (rate >= 100) {
                tvSavingsAchievementRate.setTextColor(getResources().getColor(R.color.finPositive));
            } else {
                tvSavingsAchievementRate.setTextColor(getResources().getColor(R.color.finTextPrimary));
            }

            updateSavingsCharacter(rate);
        } else {
            pbSavingsAchievement.setProgress(0);
            tvSavingsAchievementRate.setText("0%");
            tvSavingsAchievementRate.setTextColor(getResources().getColor(R.color.finTextPrimary));
            ivSavingsCharacter.setVisibility(View.GONE);
        }
    }

    // 貯金目標の達成率に応じてキャラクター画像を切り替える
    private void updateSavingsCharacter(int rate) {
        int drawableRes;
        if (rate >= SAVINGS_CHARACTER_RATE_EXCELLENT) {
            drawableRes = R.drawable.character_savings_excellent;
        } else if (rate >= SAVINGS_CHARACTER_RATE_GOOD) {
            drawableRes = R.drawable.character_savings_good;
        } else if (rate >= SAVINGS_CHARACTER_RATE_LOW) {
            drawableRes = R.drawable.character_savings_low;
        } else {
            drawableRes = R.drawable.character_savings_bad;
        }
        ivSavingsCharacter.setImageResource(drawableRes);
        ivSavingsCharacter.setVisibility(View.VISIBLE);
    }

    // 支出目標の達成率に応じてキャラクター画像を切り替える（使いすぎていないほど良いキャラになる）
    private void updateBudgetCharacter(int rate) {
        int drawableRes;
        if (rate >= BUDGET_CHARACTER_RATE_WORSE) {
            drawableRes = R.drawable.character_budget_worse;
        } else if (rate >= BUDGET_CHARACTER_RATE_BAD) {
            drawableRes = R.drawable.character_budget_bad;
        } else if (rate >= BUDGET_CHARACTER_RATE_GOOD) {
            drawableRes = R.drawable.character_budget_good;
        } else {
            drawableRes = R.drawable.character_budget_excellent;
        }
        ivBudgetCharacter.setImageResource(drawableRes);
        ivBudgetCharacter.setVisibility(View.VISIBLE);
    }

    // 目標金額入力ダイアログの表示（出費）
    private void showTargetInputDialog() {
        showInputDialog(R.string.target_amount_input_title, KEY_TARGET_BUDGET);
    }

    // 目標金額入力ダイアログの表示（貯金）
    private void showTargetSavingsInputDialog() {
        showInputDialog(R.string.target_savings_input_title, KEY_TARGET_SAVINGS);
    }

    // 汎用入力ダイアログ
    private void showInputDialog(int titleResId, String prefKey) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_target_input, null);
        EditText etTarget = dialogView.findViewById(R.id.etTarget);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentTarget = prefs.getInt(prefKey, 0);
        if (currentTarget > 0) {
            etTarget.setText(String.valueOf(currentTarget));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String input = etTarget.getText().toString();
                    if (!input.isEmpty()) {
                        int newTarget = Integer.parseInt(input);
                        prefs.edit().putInt(prefKey, newTarget).apply();
                        updateTargetDisplay();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // "YYYY/MM" の前月を計算する（年またぎを考慮）
    private String previousMonth(String yearMonth) {
        int year = Integer.parseInt(yearMonth.substring(0, 4));
        int month = Integer.parseInt(yearMonth.substring(5, 7));
        month -= 1;
        if (month == 0) {
            month = 12;
            year -= 1;
        }
        return String.format("%04d/%02d", year, month);
    }
}
