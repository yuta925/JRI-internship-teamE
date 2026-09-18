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
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import com.github.mikephil.charting.formatter.ValueFormatter;
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
        setupCombinedChart(v, 6); // 初期は直近6ヶ月

        // カテゴリ別内訳ボタンの設定
        v.findViewById(R.id.btnCategoryDetail).setOnClickListener(view -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.content_container, new CategoryDetailFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // レンジ切り替えボタンの設定
        MaterialButtonToggleGroup toggleRange = v.findViewById(R.id.toggleRange);
        toggleRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn6Months) {
                    setupCombinedChart(v, 6);
                } else if (checkedId == R.id.btn1Year) {
                    setupCombinedChart(v, 12);
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
        // pieChart.setCenterText("合計支出\n" + String.format(Locale.JAPAN, "%,d", totalExpense) + "円");
        // pieChart.setCenterTextSize(14f);
        // pieChart.setDrawCenterText(true);
        pieChart.setDrawCenterText(false);

        // 右上に合計支出を表示
        TextView tvTotalExpense = v.findViewById(R.id.tvTotalExpense);
        if (tvTotalExpense != null) {
            tvTotalExpense.setText(String.format(Locale.JAPAN, "￥%,d", totalExpense));
        }

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
    private void setupCombinedChart(View v, int monthRange) {
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
        List<Entry> savingsEntries = new ArrayList<>();

        int cumulativeSavings = 0; // 累計貯金額
        for (int i = 0; i < displayMonths.size(); i++) {
            String month = displayMonths.get(i);
            int[] sums = monthly.get(month);
            incomeEntries.add(new BarEntry(i, (float) sums[0] / 10000));
            expenseEntries.add(new BarEntry(i, (float) sums[1] / 10000));
            
            // 累計貯金額を計算 (収入 - 支出)
            cumulativeSavings += (sums[0] - sums[1]);
            savingsEntries.add(new Entry(i + 0.5f, (float) cumulativeSavings / 10000));
        }

        // 棒グラフ（収入・支出）
        BarDataSet incomeSet = new BarDataSet(incomeEntries, "収入");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.finIncome));
        incomeSet.setDrawValues(false);

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "支出");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.finExpense));
        expenseSet.setDrawValues(false);

        float barWidth = 0.3f;
        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(barWidth);

        // 折れ線グラフ（累計貯蓄額）
        LineDataSet savingsSet = new LineDataSet(savingsEntries, "累計貯蓄");
        savingsSet.setColor(ContextCompat.getColor(requireContext(), R.color.finSavings));
        savingsSet.setLineWidth(3f);
        savingsSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.finSavings));
        savingsSet.setCircleRadius(5f);
        savingsSet.setDrawValues(false);
        savingsSet.setDrawFilled(true); // 塗りつぶし有効
        savingsSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.finSavings));
        savingsSet.setFillAlpha(30);
        savingsSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        LineData lineData = new LineData(savingsSet);

        barData.groupBars(0, 0.3f, 0.05f); // groupSpace, barSpace
        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        combinedData.setData(lineData);

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

        CombinedChart combinedChart = v.findViewById(R.id.combinedChart);
        combinedChart.setData(combinedData);
        combinedChart.getDescription().setEnabled(false);
        combinedChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        combinedChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
        combinedChart.getXAxis().setGranularity(1f);
        combinedChart.getXAxis().setCenterAxisLabels(true);
        combinedChart.getAxisRight().setEnabled(false);
        combinedChart.getAxisLeft().setAxisMinimum(0f); // 0以上に固定

        combinedChart.getXAxis().setAxisMinimum(-0.5f);
        combinedChart.getXAxis().setAxisMaximum(displayMonths.size() - 0.5f);

        Legend legend = combinedChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setYOffset(10f);

        combinedChart.animateY(600);
        combinedChart.invalidate();
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
