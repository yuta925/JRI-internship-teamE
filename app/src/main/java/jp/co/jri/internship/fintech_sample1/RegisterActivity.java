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
            getSupportActionBar().setTitle("ユーザー登録");
        }

        EditText etUserId = findViewById(R.id.etUserId);
        EditText etDisplayName = findViewById(R.id.etDisplayName);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        RadioGroup rgPermission = findViewById(R.id.rgPermission);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String userId = etUserId.getText().toString().trim();
            String displayName = etDisplayName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String passwordConfirm = etPasswordConfirm.getText().toString().trim();

            // 1. 入力チェック
            if (userId.isEmpty() || displayName.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "全ての項目を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. パスワード一致チェック
            if (!password.equals(passwordConfirm)) {
                Toast.makeText(this, "パスワードが一致しません", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. ID重複チェック
            CsvReader reader = new CsvReader();
            reader.readerUserDataBase(this);
            for (UserData user : reader.userObjects) {
                if (user.getUserId().equals(userId)) {
                    Toast.makeText(this, "このユーザーIDは既に登録されています", Toast.LENGTH_SHORT).show();
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
            UserData newUser = new UserData(userId, password, displayName, permissionLevel);
            reader.appendUser(this, newUser);

            Toast.makeText(this, "登録が完了しました", Toast.LENGTH_SHORT).show();
            finish(); // 画面を閉じる
        });
    }
}