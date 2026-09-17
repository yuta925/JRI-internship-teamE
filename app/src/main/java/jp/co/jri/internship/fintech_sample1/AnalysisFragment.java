package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
//コメント
public class AnalysisFragment extends Fragment {

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
        List<FintechData> allData = parser.fintechObjects;

        if (allData.isEmpty()) {
            return v;
        }

        String currentMonth = allData.get(allData.size() - 1).getTransDate().substring(0, 7);

        setupPieChart(v, allData, currentMonth);
        setupBarChart(v, allData);

        return v;
    }

    // 今月の支出を用途分類ごとに集計して円グラフを表示する
    private void setupPieChart(View v, List<FintechData> allData, String currentMonth) {
        TextView tvPieTitle = v.findViewById(R.id.tvPieTitle);
        tvPieTitle.setText("今月の支出内訳（" + currentMonth + "）");

        // 用途分類ごとの支出合計を集計（登場順を保持）
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
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : expenseByUse.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            colors.add(colorForCategory(entry.getKey()));
        }

        PieChart pieChart = v.findViewById(R.id.pieChart);

        if (entries.isEmpty()) {
            pieChart.setNoDataText("今月の支出データがありません");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setEntryLabelColor(Color.DKGRAY);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieChart.animateY(600);
        pieChart.invalidate();
    }

    // 月ごとの収入・支出を棒グラフで表示する
    private void setupBarChart(View v, List<FintechData> allData) {
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

        List<String> months = new ArrayList<>(monthly.keySet());
        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < months.size(); i++) {
            int[] sums = monthly.get(months.get(i));
            incomeEntries.add(new BarEntry(i, sums[0]));
            expenseEntries.add(new BarEntry(i, sums[1]));
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "収入");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.finIncome));
        BarDataSet expenseSet = new BarDataSet(expenseEntries, "支出");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.finExpense));

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(barWidth);
        barData.setValueTextSize(10f);

        BarChart barChart = v.findViewById(R.id.barChart);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setCenterAxisLabels(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getXAxis().setAxisMinimum(0);
        barChart.getXAxis().setAxisMaximum(0 + barChart.getBarData().getGroupWidth(groupSpace, barSpace) * months.size());
        barChart.groupBars(0, groupSpace, barSpace);
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
