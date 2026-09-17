package jp.co.jri.internship.fintech_sample1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TransactionHistoryFragment extends Fragment {

    private View rootView;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_transaction_history, container, false);

        // 「表示」ボタンをタップしたら検索処理を呼び出す
        rootView.findViewById(R.id.btSearch).setOnClickListener(v -> clickBtSearch());

        // キーボードの制御（フォーカスが外れたときにキーボードを非表示にする）
        EditText etStart = rootView.findViewById(R.id.etStart);
        EditText etEnd = rootView.findViewById(R.id.etEnd);
        etStart.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });
        etEnd.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });

        return rootView;
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    // 「表示」がクリックされたとき
    @SuppressLint("DefaultLocale")
    private void clickBtSearch() {

        // 収支の集計領域を準備する
        int sumIncome = 0;  // 収入の集計領域
        int sumExpense = 0; // 支出の集計領域

        // 最大支出の格納領域を準備する
        int maxAmount = 0;      // 最大支出の金額
        String maxTransDate = null;    // 最大支出の取引日付
        String maxSupplier = null;     // 最大支出の取引先
        String maxContent = null;      // 最大支出の内容
        String maxUse = null;          // 最大支出の用途

        // 対象期間の始まりと終わりを入力する
        EditText etStart = rootView.findViewById(R.id.etStart);
        EditText etEnd = rootView.findViewById(R.id.etEnd);
        String startDate = etStart.getText().toString();
        String endDate = etEnd.getText().toString();

        // ボタン押下時にフォーカスを外してキーボードを閉じる
        etStart.clearFocus();
        etEnd.clearFocus();

        // 表示用のList(fintechDataList)を用意する
        List<FintechData> fintechDataList = new ArrayList<>();

        // データの読み込み（FintechDataBase.csv → ローカルファイル LocalFintechDateBase.txt 経由）
        CsvReader parser = new CsvReader();
        String filename = "LocalFintechDateBase.txt";
        requireContext().deleteFile(filename);
        File file = requireContext().getFileStreamPath(filename);
        parser.readerFintechDataBase(requireContext(), file.exists());

        // 読み込んだデータ(parser.fintechObjects)を1件ずつ取り出し、対象期間で絞り込んで
        // 表示用のList(fintechDataList)に入れる
        for (FintechData fdata : parser.fintechObjects) {
            if (fdata.getTransDate().compareTo(startDate) >= 0) {
                if (fdata.getTransDate().compareTo(endDate) <= 0) {
                    fintechDataList.add(fdata);
                    if (fdata.getAmount() >= 0) {
                        sumIncome = sumIncome + fdata.getAmount();
                    } else {
                        sumExpense = sumExpense + fdata.getAmount();
                    }
                    if (maxAmount >= fdata.getAmount()) {
                        maxAmount = fdata.getAmount();
                        maxTransDate = fdata.getTransDate();
                        maxSupplier = fdata.getSupplier();
                        maxContent = fdata.getContent();
                        maxUse = fdata.getUse();
                    }
                }
            }
        }

        // Adapterに表示用のList(fintechDataList)を受け渡す
        List<Map<String, ?>> listData = fintechDataToMapList(fintechDataList);
        SimpleAdapter adapter = new SimpleAdapter(
                requireContext(),
                listData,
                R.layout.custom_list_layout,
                new String[]{"transDate", "content", "amount"},
                new int[]{R.id.tvList1, R.id.tvList2, R.id.tvList3}
        );

        ListView lvHistoricalData = rootView.findViewById(R.id.lvHistoricalData);
        lvHistoricalData.setAdapter(adapter);

        TextView tvIncome = rootView.findViewById(R.id.tvIncome);
        tvIncome.setText(String.format("%,d", sumIncome));

        TextView tvExpense = rootView.findViewById(R.id.tvExpense);
        tvExpense.setText(String.format("%,d", sumExpense));

        TextView tvMaxTransDate = rootView.findViewById(R.id.tvMaxTransDate);
        TextView tvMaxSupplier = rootView.findViewById(R.id.tvMaxSupplier);
        TextView tvMaxContent = rootView.findViewById(R.id.tvMaxContent);
        TextView tvMaxUse = rootView.findViewById(R.id.tvMaxUse);
        TextView tvMaxAmount = rootView.findViewById(R.id.tvMaxAmount);

        tvMaxTransDate.setText(maxTransDate);
        tvMaxSupplier.setText(maxSupplier);
        tvMaxContent.setText(maxContent);
        tvMaxUse.setText(maxUse);
        tvMaxAmount.setText(String.format("%,d", maxAmount));
    }

    private List<Map<String, ?>> fintechDataToMapList(List<FintechData> fintechDataList) {
        List<Map<String, ?>> data = new ArrayList<>();
        for (FintechData fintechData : fintechDataList) {
            data.add(fintechDataToMap(fintechData));
        }
        return data;
    }

    @SuppressLint("DefaultLocale")
    private Map<String, ?> fintechDataToMap(FintechData fintechData) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", fintechData.getId());
        map.put("transDate", fintechData.getTransDate());
        map.put("transTime", fintechData.getTransTime());
        map.put("service", fintechData.getService());
        map.put("category", fintechData.getCategory());
        map.put("supplier", fintechData.getSupplier());
        map.put("content", fintechData.getContent());
        map.put("use", fintechData.getUse());
        map.put("amount", String.format("%,d", fintechData.getAmount()));
        map.put("balance", String.format("%,d", fintechData.getBalance()));
        return map;
    }
}
