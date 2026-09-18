package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionHistoryFragment extends Fragment {

    private static final int RECENT_COUNT = 5;

    private View rootView;
    private List<FintechData> allData;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_transaction_history, container, false);

        // データの読み込み（FintechDataBase.csv → ローカルファイル LocalFintechDateBase.txt 経由）
        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase_v4.txt";
        requireContext().deleteFile(filename);
        boolean localFileExists = requireContext().getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(requireContext(), localFileExists);
        allData = parser.fintechObjects;

        // 「すべて見る」→ 月ごとに切り替えられる別ページへ遷移する
        TextView btViewAll = rootView.findViewById(R.id.btViewAll);
        btViewAll.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TransactionHistoryDetailActivity.class)));

        renderRecentList();
        renderSummary(allData);

        return rootView;
    }

    // 直近5件を新しい順に一覧表示する
    private void renderRecentList() {
        ListView listView = rootView.findViewById(R.id.lvHistoricalData);
        int fromIndex = Math.max(0, allData.size() - RECENT_COUNT);
        List<FintechData> recent = new ArrayList<>(allData.subList(fromIndex, allData.size()));
        Collections.reverse(recent);
        listView.setAdapter(new HistoryAdapter(requireContext(), recent));
    }

    @SuppressLint("DefaultLocale")
    private void renderSummary(List<FintechData> data) {
        int sumIncome = 0;
        int sumExpense = 0;
        int maxAmount = 0;
        String maxTransDate = "";
        String maxSupplier = "";
        String maxContent = "";
        String maxUse = "";

        for (FintechData fdata : data) {
            if (fdata.getAmount() >= 0) {
                sumIncome += fdata.getAmount();
            } else {
                sumExpense += fdata.getAmount();
            }
            if (maxAmount >= fdata.getAmount()) {
                maxAmount = fdata.getAmount();
                maxTransDate = fdata.getTransDate();
                maxSupplier = fdata.getSupplier();
                maxContent = fdata.getContent();
                maxUse = fdata.getUse();
            }
        }

        ((TextView) rootView.findViewById(R.id.tvIncome)).setText(String.format("¥%,d", sumIncome));
        ((TextView) rootView.findViewById(R.id.tvExpense)).setText(String.format("¥%,d", sumExpense));
        ((TextView) rootView.findViewById(R.id.tvMaxTransDate)).setText(maxTransDate);
        ((TextView) rootView.findViewById(R.id.tvMaxSupplier)).setText(maxSupplier);
        ((TextView) rootView.findViewById(R.id.tvMaxContent)).setText(maxContent);
        ((TextView) rootView.findViewById(R.id.tvMaxUse)).setText(maxUse);
        ((TextView) rootView.findViewById(R.id.tvMaxAmount)).setText(String.format("¥%,d", maxAmount));
    }

    private static class HistoryAdapter extends ArrayAdapter<FintechData> {

        HistoryAdapter(android.content.Context context, List<FintechData> items) {
            super(context, 0, items);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null
                    ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.custom_list_layout, parent, false);
            FintechData data = getItem(position);
            ((TextView) view.findViewById(R.id.tvList1)).setText(data.getTransDate());
            ((TextView) view.findViewById(R.id.tvList2)).setText(data.getContent() + "/" + data.getSupplier());
            ((TextView) view.findViewById(R.id.tvList3)).setText(String.format("%,d", data.getAmount()));
            return view;
        }
    }
}
