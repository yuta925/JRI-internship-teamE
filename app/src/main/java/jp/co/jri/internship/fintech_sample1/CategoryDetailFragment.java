package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CategoryDetailFragment extends Fragment {

    private List<FintechData> allData;
    private LinearLayout llCategoryButtons;
    private ListView lvCategoryTransactions;
    private TextView tvSelectedCategoryTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_category_detail, container, false);
        
        llCategoryButtons = v.findViewById(R.id.llCategoryButtons);
        lvCategoryTransactions = v.findViewById(R.id.lvCategoryTransactions);
        tvSelectedCategoryTitle = v.findViewById(R.id.tvSelectedCategoryTitle);

        v.findViewById(R.id.btnBack).setOnClickListener(view -> {
            getParentFragmentManager().popBackStack();
        });

        loadData();
        setupCategoryButtons();
        
        return v;
    }

    private void loadData() {
        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase_v4.txt";
        boolean localFileExists = requireContext().getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(requireContext(), localFileExists);
        allData = parser.fintechObjects;
    }

    private void setupCategoryButtons() {
        if (allData == null || allData.isEmpty()) return;

        Set<String> categories = new LinkedHashSet<>();
        // 優先順位をつけてカテゴリを並べる
        String[] priorityCategories = {"食費", "交通費", "趣味", "衣服・美容", "その他", "収入"};
        for (String pc : priorityCategories) {
            for (FintechData data : allData) {
                if (data.getUse().equals(pc)) {
                    categories.add(pc);
                    break;
                }
            }
        }
        // その他のカテゴリを追加
        for (FintechData data : allData) {
            categories.add(data.getUse());
        }

        for (String category : categories) {
            Button btn = new Button(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            btn.setLayoutParams(params);
            btn.setText(category);
            btn.setAllCaps(false);
            
            // スタイル設定 (SMBCトーン)
            btn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.finHeader));
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.finHeaderText));

            btn.setOnClickListener(v -> filterByCategory(category));
            llCategoryButtons.addView(btn);
        }

        // 初期表示
        if (!categories.isEmpty()) {
            filterByCategory(categories.iterator().next());
        }
    }

    private void filterByCategory(String category) {
        tvSelectedCategoryTitle.setText(category + " の取引明細");
        List<FintechData> filteredData = new ArrayList<>();
        for (FintechData data : allData) {
            if (data.getUse().equals(category)) {
                filteredData.add(data);
            }
        }
        // 新しい順に表示
        Collections.reverse(filteredData);
        lvCategoryTransactions.setAdapter(new TransactionAdapter(requireContext(), filteredData));
    }

    private static class TransactionAdapter extends ArrayAdapter<FintechData> {
        TransactionAdapter(android.content.Context context, List<FintechData> items) {
            super(context, 0, items);
        }

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView != null
                    ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.custom_list_layout, parent, false);
            
            FintechData data = getItem(position);
            if (data != null) {
                ((TextView) view.findViewById(R.id.tvList1)).setText(data.getTransDate());
                ((TextView) view.findViewById(R.id.tvList2)).setText(data.getSupplier() + " (" + data.getContent() + ")");
                
                TextView tvAmount = view.findViewById(R.id.tvList3);
                tvAmount.setText(String.format("¥%,d", data.getAmount()));
                if (data.getAmount() < 0) {
                    tvAmount.setTextColor(ContextCompat.getColor(getContext(), R.color.finNegative));
                } else {
                    tvAmount.setTextColor(ContextCompat.getColor(getContext(), R.color.finPositive));
                }
            }
            return view;
        }
    }
}
