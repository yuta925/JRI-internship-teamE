package jp.co.jri.internship.fintech_sample1.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import jp.co.jri.internship.fintech_sample1.Main2Activity;
import jp.co.jri.internship.fintech_sample1.R;

public class LoginActivity extends AppCompatActivity {

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
    }

    // ログイン認証が成功したとき
    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        Intent intent = new Intent(this, Main2Activity.class);  // インテントの作成
        startActivity(intent);                                 // 画面遷移

        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show(); // トーストを画面表示してユーザーへ通知
    }

    // ログイン認証が失敗したとき
    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show(); // トーストを画面表示してユーザーへ通知
    }
}