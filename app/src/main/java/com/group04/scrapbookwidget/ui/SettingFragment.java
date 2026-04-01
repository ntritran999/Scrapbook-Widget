package com.group04.scrapbookwidget.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Invitation;
import com.group04.scrapbookwidget.databinding.FragmentSettingBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingFragment extends Fragment {

    private FragmentSettingBinding binding;
    private SettingViewModel viewModel;
    private GroupInvitationAdapter invitationAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(SettingViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupRecyclerView();
        setupObservers();
        setupClickListeners();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        invitationAdapter = new GroupInvitationAdapter(new GroupInvitationAdapter.OnInvitationActionListener() {
            @Override
            public void onAccept(Invitation invitation) {
                viewModel.acceptInvitation(invitation.getGroupId());
            }

            @Override
            public void onDecline(Invitation invitation) {
                viewModel.declineInvitation(invitation.getGroupId());
            }
        });
        binding.rvInvitations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvInvitations.setAdapter(invitationAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void setupObservers() {
        viewModel.getLoggedOut().observe(getViewLifecycleOwner(), loggedOut -> {
            if (Boolean.TRUE.equals(loggedOut)) {
                clearSessionAndRestart();
            }
        });

        viewModel.getAccountDeleted().observe(getViewLifecycleOwner(), deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                clearSessionAndRestart();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.account_circle_24)
                        .circleCrop()
                        .into(binding.imgProfile);
            }
        });

        viewModel.getInvitations().observe(getViewLifecycleOwner(), invitations -> {
            invitationAdapter.setInvitations(invitations);
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Avatar click -> Show full screen image using ConstraintLayout overlay
        binding.imgProfile.setOnClickListener(v -> {
            String url = (viewModel.getCurrentUser().getValue() != null) ? viewModel.getCurrentUser().getValue().getAvatarUrl() : null;
            if (url != null && !url.isEmpty()) {
                binding.fullScreenOverlay.setVisibility(View.VISIBLE);
                Glide.with(this).load(url).into(binding.imgFullScreen);
            }
        });

        // Hide overlay when clicked
        binding.fullScreenOverlay.setOnClickListener(v -> {
            binding.fullScreenOverlay.setVisibility(View.GONE);
        });

        // Edit Profile navigation
        binding.btnEditProfile.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_settingFragment_to_editProfileFragment);
        });

        // Create Group navigation (Replaced Rollcall)
        binding.btnCreateGroup.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_settingFragment_to_createGroupFragment);
        });

        if (binding.btnEditName != null) {
            binding.btnEditName.getRoot().setOnClickListener(v -> {
                Navigation.findNavController(v).navigate(R.id.action_settingFragment_to_editProfileFragment);
            });
        }

        if (binding.btnDeleteAccount != null) {
            binding.btnDeleteAccount.getRoot().setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.delete_account)
                        .setMessage(R.string.delete_account_confirmation)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> viewModel.deleteAccount())
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            });
        }

        if (binding.btnSignOut != null) {
            binding.btnSignOut.getRoot().setOnClickListener(v -> viewModel.signOut());
        }
    }

    private void clearSessionAndRestart() {
        // Clear local session preferences
        SharedPreferences preferences = requireContext().getSharedPreferences("TMP_USER_SESSION", Context.MODE_PRIVATE);
        preferences.edit().clear().apply();

        // Update widget
        AppWidget.updateWidgetNow(requireContext());

        // Navigate back to Login screen and clear navigation stack
        NavHostFragment navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.authLoginFragment, null, new androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.app_nav, true)
                    .build());
        } else {
            // Fallback: Restart Activity if NavHostFragment not found
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
