package com.group04.scrapbookwidget.ui.group;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.databinding.FragmentMemberListBinding;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MemberListFragment extends Fragment {

    private static final String TAG = "MemberListFragment";
    private FragmentMemberListBinding binding;
    private GroupSettingsViewModel viewModel;
    private MemberAdapter adapter;
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
        binding = FragmentMemberListBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(GroupSettingsViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupObservers();

        if (groupId != null) {
            Log.d(TAG, "Loading members for group: " + groupId);
            viewModel.loadMembers(groupId);
            // Also ensure group details are loaded to get the owner ID and admin status
            viewModel.loadGroupDetails(groupId);
        }

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new MemberAdapter(user -> showRemoveMemberDialog(user));
        binding.rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMembers.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getMembers().observe(getViewLifecycleOwner(), members -> {
            Log.d(TAG, "Members updated: " + (members != null ? members.size() : "null"));
            updateAdapter();
        });
        viewModel.getGroup().observe(getViewLifecycleOwner(), group -> {
            Log.d(TAG, "Group updated: " + (group != null ? group.getGroupName() : "null"));
            updateAdapter();
        });
        viewModel.isAdmin().observe(getViewLifecycleOwner(), isAdmin -> {
            Log.d(TAG, "Admin status updated: " + isAdmin);
            updateAdapter();
        });
    }

    private void updateAdapter() {
        List<User> members = viewModel.getMembers().getValue();
        Group group = viewModel.getGroup().getValue();
        
        if (members != null && group != null) {
            Log.d(TAG, "Updating adapter with " + members.size() + " members. Owner ID: " + group.getCreatedBy());
            adapter.setMembers(members, 
                    group.getCreatedBy(), 
                    Boolean.TRUE.equals(viewModel.isAdmin().getValue()));
        }
    }

    private void showRemoveMemberDialog(User user) {
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) displayName = user.getEmail();
        if (displayName == null || displayName.isEmpty()) displayName = "this member";

        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + displayName + " from the group?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    viewModel.removeMember(groupId, user.getId());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
