package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class TransactionHistoryDetailActivity extends AppCompatActivity {

    private static final String MEMO_PREFS = "TransactionMemos";
    private static final String MEMO_KEY_PREFIX = "transaction_";

    private List<FintechData> allData;
    private List<String> months; // 昇順（古い月→新しい月）
    private int currentIndex;
    private MonthlyAdapter monthlyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase_v4.txt";
        boolean localFileExists = getFileStreamPath(filename).exists();
        parser.readerFintechDataBase(this, localFileExists);
        allData = parser.fintechObjects;

        TreeSet<String> monthSet = new TreeSet<>();
        for (FintechData data : allData) {
            monthSet.add(data.getTransDate().substring(0, 7));
        }
        months = new ArrayList<>(monthSet);
        currentIndex = months.size() - 1; // 最新月から表示

        Button btPrev = findViewById(R.id.btPrevMonth);
        Button btNext = findViewById(R.id.btNextMonth);
        btPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                renderMonth();
            }
        });
        btNext.setOnClickListener(v -> {
            if (currentIndex < months.size() - 1) {
                currentIndex++;
                renderMonth();
            }
        });

        renderMonth();
    }

    private void renderMonth() {
        String month = months.get(currentIndex);

        TextView tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvCurrentMonth.setText(month.replace("/", "年") + "月");

        Button btPrev = findViewById(R.id.btPrevMonth);
        Button btNext = findViewById(R.id.btNextMonth);
        btPrev.setEnabled(currentIndex > 0);
        btNext.setEnabled(currentIndex < months.size() - 1);
        btPrev.setAlpha(btPrev.isEnabled() ? 1f : 0.3f);
        btNext.setAlpha(btNext.isEnabled() ? 1f : 0.3f);

        List<FintechData> monthData = new ArrayList<>();
        for (FintechData data : allData) {
            if (data.getTransDate().substring(0, 7).equals(month)) {
                monthData.add(data);
            }
        }
        Collections.reverse(monthData); // 新しい取引が上に来るように

        ListView listView = findViewById(R.id.lvMonthlyHistory);
        monthlyAdapter = new MonthlyAdapter(this, monthData);
        listView.setAdapter(monthlyAdapter);
        listView.setOnItemClickListener((parent, view, position, id) ->
                showMemoDialog(monthData.get(position)));
    }

    private void showMemoDialog(FintechData data) {
        SharedPreferences prefs = getSharedPreferences(MEMO_PREFS, MODE_PRIVATE);
        String key = MEMO_KEY_PREFIX + data.getId();

        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(3);
        input.setText(prefs.getString(key, ""));
        input.setSelection(input.getText().length());

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_memo_title)
                .setView(container)
                .setPositiveButton(R.string.transaction_memo_save, (dialog, which) -> {
                    String memo = input.getText().toString().trim();
                    if (memo.isEmpty()) {
                        prefs.edit().remove(key).apply();
                    } else {
                        prefs.edit().putString(key, memo).apply();
                    }
                    if (monthlyAdapter != null) {
                        monthlyAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static class MonthlyAdapter extends ArrayAdapter<FintechData> {

        private final SharedPreferences memoPrefs;

        MonthlyAdapter(android.content.Context context, List<FintechData> items) {
            super(context, 0, items);
            memoPrefs = context.getSharedPreferences(MEMO_PREFS, Context.MODE_PRIVATE);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            android.view.View view = convertView != null
                    ? convertView
                    : android.view.LayoutInflater.from(getContext()).inflate(R.layout.custom_list_layout, parent, false);
            FintechData data = getItem(position);
            ((TextView) view.findViewById(R.id.tvList1)).setText(data.getTransDate());
            ((TextView) view.findViewById(R.id.tvList2)).setText(data.getContent() + "/" + data.getSupplier());
            ((TextView) view.findViewById(R.id.tvList3)).setText(String.format("%,d", data.getAmount()));
            TextView tvMemo = view.findViewById(R.id.tvTransactionMemo);
            android.view.View memoContainer = view.findViewById(R.id.transactionMemoContainer);
            String memo = memoPrefs.getString(MEMO_KEY_PREFIX + data.getId(), "");
            if (memo.isEmpty()) {
                memoContainer.setVisibility(android.view.View.GONE);
            } else {
                tvMemo.setText(getContext().getString(R.string.transaction_memo_display, memo));
                memoContainer.setVisibility(android.view.View.VISIBLE);
            }
            return view;
        }
    }
}
