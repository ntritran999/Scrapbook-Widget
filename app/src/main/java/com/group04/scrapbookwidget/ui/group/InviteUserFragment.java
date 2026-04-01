package com.group04.scrapbookwidget.ui.group;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group04.scrapbookwidget.databinding.FragmentInviteUserBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InviteUserFragment extends Fragment {

    private FragmentInviteUserBinding binding;
    private GroupSettingsViewModel viewModel;
    private UserSearchAdapter adapter;
    private String groupId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getString("GROUP_ID");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInviteUserBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(GroupSettingsViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSearch();
        setupObservers();

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new UserSearchAdapter(selectedUser -> {
            // Lấy selectedUser.id theo yêu cầu
            String userId = selectedUser.getId();
            if (userId != null && !userId.isEmpty()) {
                viewModel.inviteUser(groupId, userId, () -> {
                    Toast.makeText(getContext(), "Invitation sent to " + selectedUser.getDisplayName(), Toast.LENGTH_SHORT).show();
                });
            } else {
                Toast.makeText(getContext(), "Selected user has no ID", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSearchResults.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupObservers() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                adapter.setUsers(users);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
