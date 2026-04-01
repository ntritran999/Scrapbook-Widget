package com.group04.scrapbookwidget.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentGroupBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GroupFragment extends Fragment {

    private FragmentGroupBinding binding;
    private GroupSettingsViewModel settingsViewModel;
    private ChatViewModel chatViewModel;
    private ChatMessageAdapter adapter;
    private String groupId;
    private String groupName;

    @Inject
    FirebaseAuth auth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getString("GROUP_ID");
            groupName = getArguments().getString("GROUP_NAME", "Chat");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(GroupSettingsViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupTopBar();
        setupRecyclerView();
        setupInput();
        setupObservers();
        
        if (groupId != null) {
            settingsViewModel.loadGroupDetails(groupId);
            chatViewModel.initChat(groupId);
        }
    }

    private void setupTopBar() {
        binding.chatTopBar.tvTitle.setText(groupName != null ? groupName : "Chat");
        binding.chatTopBar.btnBack.setVisibility(View.VISIBLE);
        binding.chatTopBar.ivGroupAvatar.setVisibility(View.VISIBLE);

        binding.chatTopBar.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.chatTopBar.btnSettings.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("GROUP_ID", groupId);
            Navigation.findNavController(v).navigate(R.id.action_chatDetailFragment_to_groupSettingsFragment, args);
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(auth.getUid());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        binding.rvChat.setLayoutManager(layoutManager);
        binding.rvChat.setAdapter(adapter);

        adapter.setOnMessageVisibleListener(message -> {
            chatViewModel.markMessageAsSeen(message.getId());
        });

        adapter.setOnResendClickListener(message -> {
            chatViewModel.resendMessage(message);
        });
    }

    private void setupInput() {
        binding.btnSend.setOnClickListener(v -> {
            String content = binding.etMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                chatViewModel.sendMessage(content);
                binding.etMessage.setText("");
            }
        });
    }

    private void setupObservers() {
        settingsViewModel.getGroup().observe(getViewLifecycleOwner(), group -> {
            if (group != null && group.getId().equals(groupId)) {
                binding.chatTopBar.tvTitle.setText(group.getGroupName());
                if (group.getAvatarUrl() != null && !group.getAvatarUrl().isEmpty()) {
                    Glide.with(this)
                            .load(group.getAvatarUrl())
                            .placeholder(R.drawable.account_circle_24)
                            .circleCrop()
                            .into(binding.chatTopBar.ivGroupAvatar);
                }
            }
        });

        chatViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    binding.rvChat.smoothScrollToPosition(messages.size() - 1);
                }
            }
        });

        chatViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                // Handle error
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
