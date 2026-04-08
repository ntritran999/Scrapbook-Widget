package com.group04.scrapbookwidget.ui.scrapbookview;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentBackgroundSelectionDialogBinding;
import com.group04.scrapbookwidget.ui.adapter.BackgroundSelectionRecyclerAdapter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BackgroundSelectionDialogFragment extends DialogFragment {
    public static final String TAG = "BackgroundSelectionDialog";
    private FragmentBackgroundSelectionDialogBinding binding;
    private BackgroundSelectionRecyclerAdapter adapter;
    private BackgroundSelectionViewModel viewModel;

    private OnBackgroundSelectListener listener;
    public interface OnBackgroundSelectListener {
        void onBackgroundSelected(String url);
    }

    public void setOnBackgroundSelectListener(OnBackgroundSelectListener listener) {
        this.listener = listener;
    }
    private String selectedUrl = "";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_background_selection_dialog, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BackgroundSelectionViewModel.class);

        setupRecyclerView();
        setupObservers();

        viewModel.loadUrls();
    }
    private void setupRecyclerView() {
        adapter = new BackgroundSelectionRecyclerAdapter(
                requireContext(),
                new String[0],
                url -> {
                    selectedUrl = url;
                    dismiss();
                }
        );
        binding.groupRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getImageUrlsLiveData().observe(getViewLifecycleOwner(), urls -> {
            if (urls == null) return;

            if (!urls.isEmpty()) {
                binding.loadingContainer.setVisibility(View.GONE);
                binding.groupRecyclerView.setVisibility(View.VISIBLE);
                adapter.updateBackgrounds(urls.toArray(new String[0]));
            } else {
                binding.loadingContainer.setVisibility(View.GONE);
                binding.groupRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null && selectedUrl != null) {
            listener.onBackgroundSelected(selectedUrl);
        }
    }
}
