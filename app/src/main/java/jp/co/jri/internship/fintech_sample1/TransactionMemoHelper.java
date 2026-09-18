package jp.co.jri.internship.fintech_sample1;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

final class TransactionMemoHelper {

    private static final String MEMO_PREFS = "TransactionMemos";
    private static final String MEMO_KEY_PREFIX = "transaction_";

    private TransactionMemoHelper() {
    }

    static void showDialog(Context context, FintechData data, Runnable onSaved) {
        SharedPreferences prefs = context.getSharedPreferences(MEMO_PREFS, Context.MODE_PRIVATE);
        String key = memoKey(data);

        EditText input = new EditText(context);
        input.setSingleLine(false);
        input.setMinLines(3);
        input.setText(prefs.getString(key, ""));
        input.setSelection(input.getText().length());

        int padding = (int) (24 * context.getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(context);
        container.setPadding(padding, 0, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(context)
                .setTitle(R.string.transaction_memo_title)
                .setView(container)
                .setPositiveButton(R.string.transaction_memo_save, (dialog, which) -> {
                    String memo = input.getText().toString().trim();
                    if (memo.isEmpty()) {
                        prefs.edit().remove(key).apply();
                    } else {
                        prefs.edit().putString(key, memo).apply();
                    }
                    if (onSaved != null) {
                        onSaved.run();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    static void bindMemo(View rowView, FintechData data) {
        SharedPreferences prefs = rowView.getContext()
                .getSharedPreferences(MEMO_PREFS, Context.MODE_PRIVATE);
        String memo = prefs.getString(memoKey(data), "");
        View memoContainer = rowView.findViewById(R.id.transactionMemoContainer);
        TextView memoText = rowView.findViewById(R.id.tvTransactionMemo);

        if (memo.isEmpty()) {
            memoContainer.setVisibility(View.GONE);
        } else {
            memoText.setText(rowView.getContext().getString(R.string.transaction_memo_display, memo));
            memoContainer.setVisibility(View.VISIBLE);
        }
    }

    private static String memoKey(FintechData data) {
        return MEMO_KEY_PREFIX + data.getId();
    }
}
