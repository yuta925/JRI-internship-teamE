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
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
        String filename = "LocalFintechDateBase_v4.txt";
        boolean localFileExists = requireContext().getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(requireContext(), localFileExists);
        allData = parser.fintechObjects;

        if (allData.isEmpty()) {
            return v;
        }

        // 全データの月を取得し、最初と最後の月を特定
        List<String> allMonths = new ArrayList<>();
        for (FintechData data : allData) {
            allMonths.add(data.getTransDate().substring(0, 7));
        }
        Collections.sort(allMonths);
        String minMonth = allMonths.get(0);
        String maxMonth = allMonths.get(allMonths.size() - 1);

        // minMonthからmaxMonthまでの全ての月をリストに追加（データがない月も含める）
        monthList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM", Locale.JAPAN);
        try {
            java.util.Date startDate = sdf.parse(minMonth);
            java.util.Date endDate = sdf.parse(maxMonth);
            if (startDate != null && endDate != null) {
                Calendar start = Calendar.getInstance();
                start.setTime(startDate);
                Calendar end = Calendar.getInstance();
                end.setTime(endDate);

                while (!start.after(end)) {
                    monthList.add(sdf.format(start.getTime()));
                    start.add(Calendar.MONTH, 1);
                }
            }
        } catch (ParseException e) {
            // 解析エラー時は従来通りデータのある月のみ
            TreeMap<String, Integer> monthsMap = new TreeMap<>();
            for (String m : allMonths) monthsMap.put(m, 0);
            monthList = new ArrayList<>(monthsMap.keySet());
        }

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
        setupCharts(v, 6); // 初期は直近6ヶ月

        // レンジ切り替えボタンの設定
        MaterialButtonToggleGroup toggleRange = v.findViewById(R.id.toggleRange);
        toggleRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn6Months) {
                    setupCharts(v, 6);
                } else if (checkedId == R.id.btn1Year) {
                    setupCharts(v, 12);
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
            pieChart.setNoDataTextColor(Color.GRAY);
            pieChart.invalidate();

            TextView tvNoData = new TextView(requireContext());
            tvNoData.setText("今月の支出データはありません");
            tvNoData.setTextColor(Color.GRAY);
            tvNoData.setPadding(0, 20, 0, 0);
            legendContainer.addView(tvNoData);
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

    // 月ごとの収入・支出・貯蓄をグラフで表示する
    private void setupCharts(View v, int monthRange) {
        // 月ごとに収入・支出を集計
        Map<String, int[]> monthly = new TreeMap<>();
        for (FintechData data : allData) {
            String month = data.getTransDate().substring(0, 7);
            int[] sums = monthly.computeIfAbsent(month, k -> new int[2]);
            if (data.getAmount() >= 0) {
                sums[0] += data.getAmount();
            } else {
                sums[1] += -data.getAmount();
            }
        }

        // 表示範囲の決定 (monthList を基準にする)
        int start = Math.max(0, monthList.size() - monthRange);
        List<String> displayMonths = monthList.subList(start, monthList.size());

        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        List<Entry> savingsEntries = new ArrayList<>();

        // 累計貯蓄の計算（表示範囲外からの累積を考慮）
        int cumulativeSavings = 0;
        for (int i = 0; i < monthList.size(); i++) {
            String month = monthList.get(i);
            int[] sums = monthly.getOrDefault(month, new int[2]);
            cumulativeSavings += (sums[0] - sums[1]);

            if (i >= start) {
                float chartIdx = (float) (i - start);
                incomeEntries.add(new BarEntry(chartIdx, (float) sums[0] / 10000));
                expenseEntries.add(new BarEntry(chartIdx, (float) sums[1] / 10000));
                // 折れ線は棒グラフの中央に合わせるため +0.5f
                savingsEntries.add(new Entry(chartIdx + 0.5f, (float) cumulativeSavings / 10000));
            }
        }

        // X軸のラベルを "M月" 形式に変換し、年度の範囲を特定
        List<String> xLabels = new ArrayList<>();
        int minYear = 9999;
        int maxYear = 0;
        for (String monthStr : displayMonths) {
            String[] parts = monthStr.split("/");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            xLabels.add(month + "月");

            if (year < minYear) minYear = year;
            if (year > maxYear) maxYear = year;
        }

        // 年度範囲の表示を更新
        TextView tvBarYearRange = v.findViewById(R.id.tvBarYearRange);
        if (minYear == maxYear) {
            tvBarYearRange.setText(minYear + "年");
        } else {
            tvBarYearRange.setText(minYear + "年 - " + maxYear + "年");
        }

        // 1. 棒グラフの設定（収入・支出）
        BarChart barChart = v.findViewById(R.id.barChart);
        BarDataSet incomeSet = new BarDataSet(incomeEntries, "収入");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.finIncome));
        incomeSet.setValueTextSize(9f);
        incomeSet.setDrawValues(true); // 常時表示

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "支出");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.finExpense));
        expenseSet.setValueTextSize(9f);
        expenseSet.setDrawValues(true); // 常時表示

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.JAPAN, "%.1f", value);
            }
        });
        barData.setBarWidth(0.35f);
        barChart.setData(barData);
        barChart.groupBars(0f, 0.2f, 0.05f);

        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setCenterAxisLabels(true);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setAxisMinimum(0f);
        barChart.getXAxis().setAxisMaximum(displayMonths.size());
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        barChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        // タップ時のイベントリスナー
        barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // 常時表示のため何もしない（円グラフを変化させない仕様を維持）
            }

            @Override
            public void onNothingSelected() {}
        });

        barChart.animateY(600);
        barChart.invalidate();

        // 2. 折れ線グラフの設定（累計貯蓄額）
        LineChart lineChart = v.findViewById(R.id.lineChart);
        LineDataSet savingsSet = new LineDataSet(savingsEntries, "累計貯蓄");
        savingsSet.setColor(ContextCompat.getColor(requireContext(), R.color.finSavings));
        savingsSet.setLineWidth(3f);
        savingsSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.finSavings));
        savingsSet.setCircleRadius(5f);
        savingsSet.setDrawValues(true); // 常時表示
        savingsSet.setValueTextSize(9f);
        savingsSet.setDrawFilled(true);
        savingsSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.finSavings));
        savingsSet.setFillAlpha(30);
        savingsSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        savingsSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.JAPAN, "%.1f", value);
            }
        });

        LineData lineData = new LineData(savingsSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setCenterAxisLabels(true);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setAxisMinimum(0f);
        lineChart.getXAxis().setAxisMaximum(displayMonths.size());
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        lineChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        // タップ時のイベントリスナー
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // 常時表示のため何もしない（円グラフを変化させない仕様を維持）
            }

            @Override
            public void onNothingSelected() {}
        });

        lineChart.animateY(600);
        lineChart.invalidate();
    }

    private int colorForCategory(String use) {
        int colorRes;
        switch (use) {
            case "食費":
                colorRes = R.color.finCatFood;
                break;
            case "趣味":
                colorRes = R.color.finCatHobby;
                break;
            case "交通費":
                colorRes = R.color.finCatTransport;
                break;
            case "衣服・美容":
                colorRes = R.color.finCatFashion;
                break;
            default:
                colorRes = R.color.finCatOther;
                break;
        }
        return ContextCompat.getColor(requireContext(), colorRes);
    }
}