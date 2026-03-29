package com.group04.scrapbookwidget.ui.scrapbookview;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.databinding.FragmentGroupSelectionDialogBinding;
import com.group04.scrapbookwidget.ui.CompactGroupListViewModel;
import com.group04.scrapbookwidget.ui.adapter.GroupSelectionRecyclerAdapter;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GroupSelectionDialogFragment extends DialogFragment {
    private static final String TMP_PREF_NAME = "TMP_USER_SESSION";
    private static final String TAG = "GroupSelectionDialog";

    private FragmentGroupSelectionDialogBinding binding;
    private CompactGroupListViewModel viewModel;
    private GroupSelectionRecyclerAdapter adapter;
    private OnGroupSelectedListener listener;

    public interface OnGroupSelectedListener {
        void onGroupSelected(Group group);
    }

    public void setOnGroupSelectedListener(OnGroupSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use Material3 dark theme with full screen dialog
        setStyle(DialogFragment.STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dark);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set dialog dimensions to 70% of screen height and match parent width
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.view.WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.7);
            getDialog().getWindow().setAttributes(params);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_selection_dialog, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CompactGroupListViewModel.class);
        
        setupRecyclerView();
        setupObservers();
        
        // Load user's groups
        String userId = requireActivity()
                .getSharedPreferences(TMP_PREF_NAME, Activity.MODE_PRIVATE)
                .getString("USER_ID", "");
        
        if (!userId.isEmpty()) {
            viewModel.loadGroupList(userId);
        }
    }

    private void setupRecyclerView() {
        adapter = new GroupSelectionRecyclerAdapter(
                requireContext(),
                new ArrayList<>(),
                group -> {
                    if (listener != null) {
                        listener.onGroupSelected(group);
                    }
                    dismiss();
                }
        );
        binding.groupRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getGroupsLiveData().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null && !groups.isEmpty()) {
                binding.loadingContainer.setVisibility(View.GONE);
                binding.errorContainer.setVisibility(View.GONE);
                binding.groupRecyclerView.setVisibility(View.VISIBLE);
                adapter.updateGroups(groups);
            } else if (groups != null && groups.isEmpty()) {
                binding.loadingContainer.setVisibility(View.GONE);
                binding.groupRecyclerView.setVisibility(View.GONE);
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorMessage.setText(R.string.no_groups_found);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.loadingContainer.setVisibility(View.GONE);
                binding.groupRecyclerView.setVisibility(View.GONE);
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.errorMessage.setText(error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
