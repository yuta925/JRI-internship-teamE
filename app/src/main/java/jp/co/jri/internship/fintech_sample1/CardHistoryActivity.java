package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

public class CardHistoryActivity extends AppCompatActivity {

    private List<FintechData> cardData;
    private List<String> months;
    private int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        findViewById(R.id.btnBackCardHistory).setOnClickListener(v -> finish());

        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase_v4.txt";
        parser.readerFintechDataBase(this, getFileStreamPath(filename).exists());

        cardData = new ArrayList<>();
        TreeSet<String> monthSet = new TreeSet<>();
        for (FintechData data : parser.fintechObjects) {
            if ("クレジットカード".equals(data.getCategory()) && data.getAmount() < 0) {
                cardData.add(data);
                monthSet.add(data.getTransDate().substring(0, 7));
            }
        }

        months = new ArrayList<>(monthSet);
        currentIndex = months.size() - 1;

        Button btPrev = findViewById(R.id.btPrevCardMonth);
        Button btNext = findViewById(R.id.btNextCardMonth);
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
        if (months.isEmpty()) {
            findViewById(R.id.btPrevCardMonth).setEnabled(false);
            findViewById(R.id.btNextCardMonth).setEnabled(false);
            ((TextView) findViewById(R.id.tvCardCurrentMonth)).setText(R.string.home_card_no_usage);
            ((TextView) findViewById(R.id.tvCardMonthlyTotal))
                    .setText("¥0");
            return;
        }

        String month = months.get(currentIndex);
        ((TextView) findViewById(R.id.tvCardCurrentMonth))
                .setText(month.replace("/", "年") + "月");

        Button btPrev = findViewById(R.id.btPrevCardMonth);
        Button btNext = findViewById(R.id.btNextCardMonth);
        btPrev.setEnabled(currentIndex > 0);
        btNext.setEnabled(currentIndex < months.size() - 1);
        btPrev.setAlpha(btPrev.isEnabled() ? 1f : 0.3f);
        btNext.setAlpha(btNext.isEnabled() ? 1f : 0.3f);

        int monthlyTotal = 0;
        List<FintechData> monthData = new ArrayList<>();
        for (FintechData data : cardData) {
            if (data.getTransDate().substring(0, 7).equals(month)) {
                monthData.add(data);
                monthlyTotal -= data.getAmount();
            }
        }
        Collections.reverse(monthData);

        ((TextView) findViewById(R.id.tvCardMonthlyTotal))
                .setText(String.format("¥%,d", monthlyTotal));
        ((ListView) findViewById(R.id.lvCardMonthlyHistory))
                .setAdapter(new CardMonthlyAdapter(this, monthData));
    }

    private static class CardMonthlyAdapter extends ArrayAdapter<FintechData> {

        CardMonthlyAdapter(android.content.Context context, List<FintechData> items) {
            super(context, 0, items);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public android.view.View getView(
                int position,
                android.view.View convertView,
                android.view.ViewGroup parent) {
            android.view.View view = convertView != null
                    ? convertView
                    : android.view.LayoutInflater.from(getContext())
                            .inflate(R.layout.custom_list_layout, parent, false);
            FintechData data = getItem(position);
            ((TextView) view.findViewById(R.id.tvList1)).setText(data.getTransDate());
            ((TextView) view.findViewById(R.id.tvList2))
                    .setText(data.getContent() + "/" + data.getSupplier());
            ((TextView) view.findViewById(R.id.tvList3))
                    .setText(String.format("¥%,d", -data.getAmount()));
            return view;
        }
    }
}
