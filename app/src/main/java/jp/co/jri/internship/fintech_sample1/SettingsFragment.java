package jp.co.jri.internship.fintech_sample1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        Button btnRegisterUser = v.findViewById(R.id.btnRegisterUser);
        btnRegisterUser.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            String userId = getActivity().getIntent().getStringExtra(Main2Activity.EXTRA_USER_ID);
            intent.putExtra(Main2Activity.EXTRA_USER_ID, userId);
            startActivity(intent);
        });

        Button btnLogout = v.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(view -> {
            // ログイン画面に戻る
            Intent intent = new Intent(getActivity(), jp.co.jri.internship.fintech_sample1.login.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return v;
    }
}