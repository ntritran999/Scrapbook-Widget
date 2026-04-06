package com.group04.scrapbookwidget.ui.group;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentGroupSettingsBinding;
import com.group04.scrapbookwidget.ui.CompactGroupListViewModel;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GroupSettingsFragment extends Fragment {

    private FragmentGroupSettingsBinding binding;
    private GroupSettingsViewModel viewModel;
    private CompactGroupListViewModel sharedViewModel;
    private String groupId;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getString("GROUP_ID");
        }

        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                startCrop(uri);
            }
        });

        cropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                final Uri resultUri = UCrop.getOutput(result.getData());
                if (resultUri != null) {
                    viewModel.uploadGroupAvatar(groupId, new File(resultUri.getPath()));
                }
            } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                final Throwable cropError = UCrop.getError(result.getData());
                String message = (cropError != null) ? cropError.getMessage() : "Unknown error";
                Toast.makeText(requireContext(), "Crop error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupSettingsBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(GroupSettingsViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(CompactGroupListViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (groupId != null) {
            viewModel.loadGroupDetails(groupId);
        }

        setupObservers();
        setupClickListeners();
        setupPopupListeners();
    }

    private void setupObservers() {
        viewModel.getGroup().observe(getViewLifecycleOwner(), group -> {
            if (group != null) {
                if (group.getAvatarUrl() != null) {
                    Glide.with(this)
                            .load(group.getAvatarUrl())
                            .placeholder(R.drawable.account_circle_24)
                            .circleCrop()
                            .into(binding.imgGroupAvatar);
                }
                // Refresh the group list in messages screen
                sharedViewModel.refresh();
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        binding.btnLeaveGroup.getRoot().setOnClickListener(v -> showLeaveGroupDialog());

        binding.btnMemberList.getRoot().setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("GROUP_ID", groupId);
            Navigation.findNavController(v).navigate(R.id.action_groupSettingsFragment_to_memberListFragment, args);
        });

        binding.btnInviteUsers.getRoot().setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("GROUP_ID", groupId);
            Navigation.findNavController(v).navigate(R.id.action_groupSettingsFragment_to_inviteUserFragment, args);
        });

        binding.btnShareInviteLink.getRoot().setOnClickListener(v ->
                viewModel.getInviteLink(groupId, new GroupSettingsViewModel.InviteLinkCallback() {
                    @Override
                    public void onSuccess(String inviteLink) {
                        shareInviteLink(inviteLink);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                }));

        // Show popups instead of dialogs
        binding.tvGroupName.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.isAdmin().getValue())) {
                showEditNamePopup();
            }
        });
        binding.btnChangeGroupName.getRoot().setOnClickListener(v -> showEditNamePopup());

        binding.imgGroupAvatar.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.isAdmin().getValue())) {
                openImagePicker();
            }
        });
        binding.btnChangeGroupAvatar.getRoot().setOnClickListener(v -> openImagePicker());
        
        binding.btnManageInvitations.getRoot().setOnClickListener(v -> {
            Toast.makeText(getContext(), "Manage invitations not implemented yet", Toast.LENGTH_SHORT).show();
        });
    }

    private void shareInviteLink(String inviteLink) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.invite_link_share_message, inviteLink));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_invite_link)));
    }

    private void setupPopupListeners() {
        // Name Popup
        binding.btnCancelName.setOnClickListener(v -> hidePopups());
        binding.btnUpdateName.setOnClickListener(v -> {
            String newName = binding.etGroupName.getText().toString().trim();
            if (!newName.isEmpty()) {
                viewModel.updateGroupName(groupId, newName);
                hidePopups();
            } else {
                Toast.makeText(getContext(), "Group name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Overlay click to dismiss
        binding.overlayLayout.setOnClickListener(v -> hidePopups());
        
        // Prevent clicks inside cards from dismissing the overlay
        binding.cardEditName.setOnClickListener(v -> {});
        binding.cardEditAvatar.setOnClickListener(v -> {});
    }

    private void openImagePicker() {
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void startCrop(@NonNull Uri uri) {
        Context context = requireContext();
        String destinationFileName = "group_avatar_crop_" + UUID.randomUUID().toString() + ".jpg";
        File cacheFile = new File(context.getCacheDir(), destinationFileName);
        Uri destinationUri = Uri.fromFile(cacheFile);

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setCompressionQuality(80);
        
        options.setToolbarColor(ContextCompat.getColor(context, R.color.black));
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.black));
        // Using common color resource if locket_yellow is not found, but EditProfile used it.
        // Assuming locket_yellow exists as it's used in EditProfileFragment.
        options.setActiveControlsWidgetColor(ContextCompat.getColor(context, R.color.locket_yellow));
        options.setToolbarWidgetColor(ContextCompat.getColor(context, R.color.white));

        Intent uCropIntent = UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1)
                .withOptions(options)
                .getIntent(context);
        
        cropLauncher.launch(uCropIntent);
    }

    private void showEditNamePopup() {
        binding.overlayLayout.setVisibility(View.VISIBLE);
        binding.cardEditName.setVisibility(View.VISIBLE);
        binding.cardEditAvatar.setVisibility(View.GONE);
        
        String currentName = viewModel.getGroup().getValue() != null ? viewModel.getGroup().getValue().getGroupName() : "";
        binding.etGroupName.setText(currentName);
        binding.etGroupName.requestFocus();
    }

    private void hidePopups() {
        binding.overlayLayout.setVisibility(View.GONE);
        binding.cardEditName.setVisibility(View.GONE);
        binding.cardEditAvatar.setVisibility(View.GONE);
    }

    private void showLeaveGroupDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.leave_group)
                .setMessage(R.string.leave_group_warning)
                .setPositiveButton(R.string.leave_group, (dialog, which) -> {
                    viewModel.leaveGroup(groupId, () -> {
                        Toast.makeText(getContext(), "Left group", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).popBackStack(R.id.homeFragment, true);
                        Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
                    });
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
