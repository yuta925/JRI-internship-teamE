package jp.co.jri.internship.fintech_sample1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class CategoryDetailFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_category_detail, container, false);
        
        v.findViewById(R.id.btnBack).setOnClickListener(view -> {
            getParentFragmentManager().popBackStack();
        });
        
        return v;
    }
}
