package jp.co.jri.internship.fintech_sample1;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import jp.co.jri.internship.fintech_sample1.login.UserData;

/**
 * ユーザーごとの権限レベルを変更する画面。
 * 「全て許可」（権限レベル1）のユーザーのみアクセス可能。
 */
public class UserManageActivity extends AppCompatActivity {

    private static final String[] PERMISSION_LABELS = {
            "1: 全て許可",
            "2: 資金移動のみ不可",
            "3: 閲覧のみ可能"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manage);

        int permissionLevel = getIntent().getIntExtra(Main2Activity.EXTRA_PERMISSION_LEVEL, 1);
        if (permissionLevel != 1) {
            Toast.makeText(this, "この操作は「全て許可」の権限を持つユーザーのみ実行できます", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ユーザー権限管理");
        }

        CsvReader reader = new CsvReader();
        reader.readerUserDataBase(this);

        ListView listView = findViewById(R.id.lvUserPermission);
        UserPermissionAdapter adapter = new UserPermissionAdapter(this, reader.userObjects);
        listView.setAdapter(adapter);
    }

    private static class UserPermissionAdapter extends ArrayAdapter<UserData> {

        UserPermissionAdapter(Context context, List<UserData> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null
                    ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.item_user_permission, parent, false);

            UserData user = getItem(position);
            if (user == null) {
                return view;
            }

            ((TextView) view.findViewById(R.id.tvUserName)).setText(user.getDisplayName());
            ((TextView) view.findViewById(R.id.tvUserId)).setText(user.getUserId());

            Spinner spinner = view.findViewById(R.id.spPermission);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    getContext(), android.R.layout.simple_spinner_item, PERMISSION_LABELS);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);

            // 選択リスナーを外してから初期選択位置を設定することで、
            // 表示時に不要な更新処理が走らないようにする
            spinner.setOnItemSelectedListener(null);
            spinner.setSelection(user.getPermissionLevel() - 1, false);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View selectedView, int spinnerPosition, long id) {
                    int newPermissionLevel = spinnerPosition + 1;
                    if (newPermissionLevel == user.getPermissionLevel()) {
                        return;
                    }
                    CsvReader reader = new CsvReader();
                    reader.updateUserPermission(getContext(), user.getUserId(), newPermissionLevel);
                    Toast.makeText(getContext(),
                            user.getDisplayName() + "さんの権限を変更しました",
                            Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            return view;
        }
    }
}
