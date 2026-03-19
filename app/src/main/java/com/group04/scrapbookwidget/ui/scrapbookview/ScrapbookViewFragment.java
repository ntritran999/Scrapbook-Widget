package com.group04.scrapbookwidget.ui.scrapbookview;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentScrapbookViewBinding;

public class ScrapbookViewFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public ScrapbookViewFragment() {}
    public static ScrapbookViewFragment newInstance(String param1, String param2) {
        ScrapbookViewFragment fragment = new ScrapbookViewFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        FragmentScrapbookViewBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_scrapbook_view, container, false);
        binding.cameraBtn.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_scrapbookViewFragment_to_cameraFragment);
        });
        return binding.getRoot();
    }
}