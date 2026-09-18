package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class HomeFragment extends Fragment {

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

        if (allData.isEmpty()) {
            tvTotalAssets.setText("¥0");
            tvMoM.setText("");
            tvIncome.setText("¥0");
            tvExpense.setText("¥0");
            tvSavings.setText("¥0");
            return v;
        }

        // データは日付順に並んでいる前提で、末尾が最新の取引＝現在の総資産
        FintechData latest = allData.get(allData.size() - 1);
        int totalAssets = latest.getBalance();
        String currentMonth = latest.getTransDate().substring(0, 7); // "YYYY/MM"
        String previousMonth = previousMonth(currentMonth);

        int sumIncome = 0;
        int sumExpense = 0;
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

        int savings = sumIncome - sumExpense;

        tvTotalAssets.setText(String.format("¥%,d", totalAssets));
        tvIncome.setText(String.format("¥%,d", sumIncome));
        tvExpense.setText(String.format("¥%,d", sumExpense));
        tvSavings.setText(String.format("¥%,d", savings));

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
