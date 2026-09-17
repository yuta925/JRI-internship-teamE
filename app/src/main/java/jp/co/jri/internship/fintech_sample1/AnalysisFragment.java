package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
//コメント
public class AnalysisFragment extends Fragment {

    private List<FintechData> allData;
    private List<String> monthList;
    private int currentMonthIndex;

    @SuppressLint("DefaultLocale")
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_analysis, container, false);

        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase.txt";
        boolean localFileExists = requireContext().getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(requireContext(), localFileExists);
        allData = parser.fintechObjects;

        if (allData.isEmpty()) {
            return v;
        }

        // 月のリストを作成（重複排除・ソート）
        TreeMap<String, Integer> monthsMap = new TreeMap<>();
        for (FintechData data : allData) {
            monthsMap.put(data.getTransDate().substring(0, 7), 0);
        }
        monthList = new ArrayList<>(monthsMap.keySet());
        currentMonthIndex = monthList.size() - 1; // 初期表示は最新月

        // 月切り替えボタンの設定
        ImageButton btnPrev = v.findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = v.findViewById(R.id.btnNextMonth);

        btnPrev.setOnClickListener(view -> {
            if (currentMonthIndex > 0) {
                currentMonthIndex--;
                updatePieChart(v);
            }
        });

        btnNext.setOnClickListener(view -> {
            if (currentMonthIndex < monthList.size() - 1) {
                currentMonthIndex++;
                updatePieChart(v);
            }
        });

        updatePieChart(v);
        setupBarChart(v, 6); // 初期は直近6ヶ月

        // レンジ切り替えボタンの設定
        MaterialButtonToggleGroup toggleRange = v.findViewById(R.id.toggleRange);
        toggleRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn6Months) {
                    setupBarChart(v, 6);
                } else if (checkedId == R.id.btn1Year) {
                    setupBarChart(v, 12);
                }
            }
        });

        return v;
    }

    // 指定された月の支出を用途分類ごとに集計して円グラフを表示する
    private void updatePieChart(View v) {
        String currentMonth = monthList.get(currentMonthIndex);
        TextView tvHeaderTitle = v.findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("分析（" + currentMonth + "）");

        // 前後ボタンの有効/無効切り替え
        ImageButton btnPrev = v.findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = v.findViewById(R.id.btnNextMonth);
        btnPrev.setEnabled(currentMonthIndex > 0);
        btnNext.setEnabled(currentMonthIndex < monthList.size() - 1);
        btnPrev.setAlpha(currentMonthIndex > 0 ? 1.0f : 0.3f);
        btnNext.setAlpha(currentMonthIndex < monthList.size() - 1 ? 1.0f : 0.3f);

        // 用途分類ごとの支出合計を集計
        int totalExpense = 0;
        Map<String, Integer> expenseByUse = new LinkedHashMap<>();
        for (FintechData data : allData) {
            if (!data.getTransDate().substring(0, 7).equals(currentMonth)) {
                continue;
            }
            if (data.getAmount() >= 0) {
                continue; // 収入は支出内訳に含めない
            }
            String use = data.getUse();
            int abs = -data.getAmount();
            expenseByUse.put(use, expenseByUse.getOrDefault(use, 0) + abs);
            totalExpense += abs;
        }

        PieChart pieChart = v.findViewById(R.id.pieChart);
        LinearLayout legendContainer = v.findViewById(R.id.llLegendContainer);
        legendContainer.removeAllViews();

        if (expenseByUse.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("この月の支出データがありません");
            pieChart.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Map.Entry<String, Integer> entry : expenseByUse.entrySet()) {
            String category = entry.getKey();
            int amount = entry.getValue();
            float percent = (float) amount / totalExpense * 100f;
            int color = colorForCategory(category);

            entries.add(new PieEntry(amount, category));
            colors.add(color);

            // 右側の凡例セクションにアイテムを追加
            View itemView = inflater.inflate(R.layout.item_analysis_category, legendContainer, false);
            itemView.findViewById(R.id.viewCategoryColor).setBackgroundColor(color);
            ((TextView) itemView.findViewById(R.id.tvCategoryName)).setText(category);
            ((TextView) itemView.findViewById(R.id.tvCategoryAmount)).setText(String.format(Locale.JAPAN, "%,d円", amount));
            ((TextView) itemView.findViewById(R.id.tvCategoryPercent)).setText(String.format(Locale.JAPAN, "(%.1f%%)", percent));
            legendContainer.addView(itemView);
        }

        // 円グラフ中央に合計金額を表示
        pieChart.setCenterText("合計支出\n" + String.format(Locale.JAPAN, "%,d", totalExpense) + "円");
        pieChart.setCenterTextSize(14f);
        pieChart.setDrawCenterText(true);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false); // グラフ上の数値を非表示（右側に表示するため）
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setDrawEntryLabels(false); // グラフ上のラベルを非表示
        pieChart.getLegend().setEnabled(false); // 標準の凡例を非表示（自作したものを使うため）
        pieChart.animateY(600);
        pieChart.invalidate();
    }

    // 月ごとの収入・支出を棒グラフで表示する
    private void setupBarChart(View v, int monthRange) {
        // 月ごとに収入・支出を集計（月順にソート）
        Map<String, int[]> monthly = new TreeMap<>(); // {月: [収入, 支出]}
        for (FintechData data : allData) {
            String month = data.getTransDate().substring(0, 7);
            int[] sums = monthly.computeIfAbsent(month, k -> new int[2]);
            if (data.getAmount() >= 0) {
                sums[0] += data.getAmount();
            } else {
                sums[1] += -data.getAmount();
            }
        }

        List<String> allMonths = new ArrayList<>(monthly.keySet());
        // 直近 monthRange 分の月を抽出
        int start = Math.max(0, allMonths.size() - monthRange);
        List<String> displayMonths = allMonths.subList(start, allMonths.size());

        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < displayMonths.size(); i++) {
            String month = displayMonths.get(i);
            int[] sums = monthly.get(month);
            incomeEntries.add(new BarEntry(i, (float) sums[0] / 10000));
            expenseEntries.add(new BarEntry(i, (float) sums[1] / 10000));
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "収入");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.finIncome));
        incomeSet.setDrawValues(false); // 棒の上の数値を非表示

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "支出");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.finExpense));
        expenseSet.setDrawValues(false); // 棒の上の数値を非表示

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(barWidth);

        BarChart barChart = v.findViewById(R.id.barChart);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(displayMonths));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setCenterAxisLabels(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);

        // Y軸のフォーマッタ（万単位）
        barChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.JAPAN, "%.0f", value);
            }
        });

        barChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMaximum(displayMonths.size());
        barChart.groupBars(0, groupSpace, barSpace);

        // 凡例の設定（上部中央）
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setYOffset(10f);

        barChart.animateY(600);
        barChart.invalidate();
    }

    private int colorForCategory(String use) {
        int colorRes;
        switch (use) {
            case "生活費":
                colorRes = R.color.finCatLiving;
                break;
            case "遊興費":
                colorRes = R.color.finCatFun;
                break;
            case "投資":
                colorRes = R.color.finCatInvest;
                break;
            case "貯蓄":
                colorRes = R.color.finCatSavings;
                break;
            default:
                colorRes = R.color.finCatOther;
                break;
        }
        return ContextCompat.getColor(requireContext(), colorRes);
    }
}
