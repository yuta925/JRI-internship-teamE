package jp.co.jri.internship.fintech_sample1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import jp.co.jri.internship.fintech_sample1.login.UserData;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ユーザ追加登録");
        }

        com.google.android.material.textfield.TextInputLayout tilUserId = findViewById(R.id.tilUserId);
        com.google.android.material.textfield.TextInputLayout tilDisplayName = findViewById(R.id.tilDisplayName);
        com.google.android.material.textfield.TextInputLayout tilPassword = findViewById(R.id.tilPassword);
        com.google.android.material.textfield.TextInputLayout tilPasswordConfirm = findViewById(R.id.tilPasswordConfirm);

        EditText etUserId = findViewById(R.id.etUserId);
        EditText etDisplayName = findViewById(R.id.etDisplayName);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        RadioGroup rgPermission = findViewById(R.id.rgPermission);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        // エンターキー（完了）が押された時の動作
        etPasswordConfirm.setOnEditorActionListener((v, actionId, event) -> {
            btnSubmit.performClick();
            return true;
        });

        btnSubmit.setOnClickListener(v -> {
            // エラー表示を一旦リセット
            tilUserId.setError(null);
            tilDisplayName.setError(null);
            tilPassword.setError(null);
            tilPasswordConfirm.setError(null);

            String userId = etUserId.getText().toString().trim();
            String displayName = etDisplayName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String passwordConfirm = etPasswordConfirm.getText().toString().trim();

            boolean hasError = false;

            // 1. 入力チェック
            if (userId.isEmpty()) {
                tilUserId.setError("ユーザーIDを入力してください");
                hasError = true;
            }
            if (displayName.isEmpty()) {
                tilDisplayName.setError("表示名を入力してください");
                hasError = true;
            }

            // 2. パスワード長さチェック (6文字以上)
            if (password.length() < 6) {
                tilPassword.setError("パスワードは6文字以上で入力してください");
                hasError = true;
            }

            // 3. パスワード一致チェック
            if (!password.equals(passwordConfirm)) {
                tilPasswordConfirm.setError("パスワードが一致しません");
                hasError = true;
            }

            if (hasError) return;

            // 4. ID重複チェック
            CsvReader reader = new CsvReader();
            reader.readerUserDataBase(this);
            for (UserData user : reader.userObjects) {
                if (user.getUserId().equals(userId)) {
                    tilUserId.setError("このユーザーIDは既に登録されています");
                    return;
                }
            }

            // 4. 権限レベルの取得
            int permissionLevel = 1;
            int checkedId = rgPermission.getCheckedRadioButtonId();
            if (checkedId == R.id.rbLevel2) {
                permissionLevel = 2;
            } else if (checkedId == R.id.rbLevel3) {
                permissionLevel = 3;
            }

            // 5. 保存
            String parentUserId = getIntent().getStringExtra(Main2Activity.EXTRA_USER_ID);
            if (parentUserId == null) parentUserId = "none";
            
            UserData newUser = new UserData(userId, password, displayName, permissionLevel, parentUserId);
            reader.appendUser(this, newUser);

            Toast.makeText(this, "登録が完了しました", Toast.LENGTH_SHORT).show();
            finish(); // 画面を閉じる
        });
    }
}