package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class TransactionHistoryDetailActivity extends AppCompatActivity {

    private List<FintechData> allData;
    private List<String> months; // 昇順（古い月→新しい月）
    private int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase.txt";
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
        listView.setAdapter(new MonthlyAdapter(this, monthData));
    }

    private static class MonthlyAdapter extends ArrayAdapter<FintechData> {

        MonthlyAdapter(android.content.Context context, List<FintechData> items) {
            super(context, 0, items);
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
            return view;
        }
    }
}
