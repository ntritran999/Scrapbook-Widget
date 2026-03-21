package com.group04.scrapbookwidget.ui.scrapbookview;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentScrapbookViewBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScrapbookViewFragment extends Fragment {
    private ScrapbookViewModel scrapbookViewModel;
    private String groupId = "", pageId = "";
    public ScrapbookViewFragment() {}

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
           Bundle bundle = getArguments();
           if (bundle != null) {
               groupId = bundle.getString("GROUP_ID");
               pageId = bundle.getString("PAGE_ID");
           }
        }
        else {
            groupId = savedInstanceState.getString("GROUP_ID");
            pageId = savedInstanceState.getString("PAGE_ID");
        }
        scrapbookViewModel = new ViewModelProvider(this).get(ScrapbookViewModel.class);
        scrapbookViewModel.loadScrapbook(groupId, pageId);
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