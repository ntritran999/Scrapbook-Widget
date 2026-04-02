package com.group04.scrapbookwidget.ui.group.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.group04.scrapbookwidget.databinding.FragmentCreateGroupBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateGroupFragment extends Fragment {

    private FragmentCreateGroupBinding binding;
    private CreateGroupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateGroupBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(CreateGroupViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupObservers();
        setupClickListeners();

        return binding.getRoot();
    }

    private void setupObservers() {
        viewModel.getCreatedGroup().observe(getViewLifecycleOwner(), group -> {
            if (group != null) {
                Toast.makeText(requireContext(), "Group created: " + group.getGroupName(), Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnCreate.setOnClickListener(v -> viewModel.createGroup());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
