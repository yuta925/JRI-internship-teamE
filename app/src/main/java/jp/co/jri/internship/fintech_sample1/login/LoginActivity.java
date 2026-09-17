package jp.co.jri.internship.fintech_sample1.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import jp.co.jri.internship.fintech_sample1.Main2Activity;
import jp.co.jri.internship.fintech_sample1.R;

public class LoginActivity extends AppCompatActivity {

    // ▼▼▼ デバッグ用: 連続ログイン日数を強制的に上書きするための設定（確認後は削除すること） ▼▼▼
    private static final String DEBUG_PREFS_NAME = "debug_prefs";
    private static final String KEY_FORCED_STREAK = "forced_consecutive_login_days";
    // ▲▲▲ デバッグ用ここまで ▲▲▲

    private LoginViewModel loginViewModel;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // レイアウト（activity_login.xml）を表示する
        setContentView(R.layout.activity_login);

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);
        mContext = this;

        final EditText userIdEditText = findViewById(R.id.userId);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        // 入力条件チェック結果に応じて表示を変更
        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid()); // ログインボタンをグレーアウト
            if (loginFormState.getUserIdError() != null) {
                userIdEditText.setError(getString(loginFormState.getUserIdError())); // テキストボックスのエラー処理
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError())); // テキストボックスのエラー処理
            }
        });

        // ログイン結果に応じて処理を振り分け
        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);

            if (loginResult.getError() != null) {
                showLoginFailed(loginResult.getError());
                return; // エラー時は処理を終了し、ログイン画面に留まる
            }

            if (loginResult.getSuccess() != null) {
                updateUiWithUser(loginResult.getSuccess());
                setResult(Activity.RESULT_OK);
                finish(); // 成功時のみActivityを終了
            }
        });

        // テキストボックスの内容が変更されたとき、入力条件を満たしている状態かチェック
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(userIdEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        userIdEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);

        // Enterが入力されたとき
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginViewModel.login(userIdEditText.getText().toString(),
                        passwordEditText.getText().toString(),
                        mContext);
            }
            return false;
        });

        // ログインボタンがクリックされたとき
        loginButton.setOnClickListener(v -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            loginViewModel.login(userIdEditText.getText().toString(),
                    passwordEditText.getText().toString(),
                    mContext);
        });

        // ▼▼▼ デバッグ用: ログインボタン長押しで連続ログイン日数の強制設定ダイアログを表示（確認後は削除すること） ▼▼▼
        loginButton.setOnLongClickListener(v -> {
            showDebugForceStreakDialog();
            return true;
        });
        // ▲▲▲ デバッグ用ここまで ▲▲▲
    }

    // ▼▼▼ デバッグ用: 連続ログイン日数を強制的に指定するダイアログ（確認後は削除すること） ▼▼▼
    private void showDebugForceStreakDialog() {
        SharedPreferences prefs = getSharedPreferences(DEBUG_PREFS_NAME, MODE_PRIVATE);
        int currentForced = prefs.getInt(KEY_FORCED_STREAK, -1);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("例：5（空欄で解除）");
        if (currentForced >= 0) {
            input.setText(String.valueOf(currentForced));
        }

        new AlertDialog.Builder(this)
                .setTitle("【デバッグ】連続ログイン日数を強制設定")
                .setMessage("次回ログイン成功時に、実際の計算値の代わりにここで指定した日数を使用します。空欄で「解除」を押すと通常の計算に戻ります。")
                .setView(input)
                .setPositiveButton("設定", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    SharedPreferences.Editor editor = prefs.edit();
                    if (text.isEmpty()) {
                        editor.remove(KEY_FORCED_STREAK);
                        Toast.makeText(this, "【デバッグ】強制設定を解除しました", Toast.LENGTH_SHORT).show();
                    } else {
                        editor.putInt(KEY_FORCED_STREAK, Integer.parseInt(text));
                        Toast.makeText(this, "【デバッグ】連続ログイン日数を" + text + "日に強制設定しました", Toast.LENGTH_SHORT).show();
                    }
                    editor.apply();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }
    // ▲▲▲ デバッグ用ここまで ▲▲▲

    // ログイン認証が成功したとき
    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();

        // ▼▼▼ デバッグ用: 強制設定があれば実際の計算値の代わりに使用（確認後は削除すること） ▼▼▼
        int actualStreak = model.getConsecutiveLoginDays();
        int forcedStreak = getSharedPreferences(DEBUG_PREFS_NAME, MODE_PRIVATE).getInt(KEY_FORCED_STREAK, -1);
        int streakToUse = forcedStreak >= 0 ? forcedStreak : actualStreak;
        // ▲▲▲ デバッグ用ここまで ▲▲▲

        Intent intent = new Intent(this, Main2Activity.class);  // インテントの作成
        intent.putExtra(Main2Activity.EXTRA_CONSECUTIVE_LOGIN_DAYS, streakToUse);
        startActivity(intent);                                 // 画面遷移

        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show(); // トーストを画面表示してユーザーへ通知

        // ▼▼▼ デバッグ用: LoginHistory.csvへの記録確認用の一時表示（確認後は削除すること） ▼▼▼
        Toast.makeText(getApplicationContext(),
                "【デバッグ】最終ログイン日時: " + model.getLastLoginDateTime()
                        + " / 実際の連続ログイン: " + actualStreak + "日"
                        + (forcedStreak >= 0 ? " / 強制値: " + forcedStreak + "日" : ""),
                Toast.LENGTH_LONG).show();
        // ▲▲▲ デバッグ用ここまで ▲▲▲
    }

    // ログイン認証が失敗したとき
    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show(); // トーストを画面表示してユーザーへ通知
    }
}