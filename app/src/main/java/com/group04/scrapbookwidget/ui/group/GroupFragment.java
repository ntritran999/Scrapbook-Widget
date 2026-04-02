package com.group04.scrapbookwidget.ui.group;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
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
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.databinding.FragmentGroupBinding;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GroupFragment extends Fragment {
    private static final String MEMORY_TAG = "MemoryDebug";
    private FragmentGroupBinding binding;
    private GroupSettingsViewModel settingsViewModel;
    private ChatViewModel chatViewModel;
    private ChatMessageAdapter adapter;
    private String groupId;
    private String groupName;
    private boolean isMemoryBannerDismissed = false;
    private List<TodayMemory> currentMemories = new ArrayList<>();

    @Inject
    FirebaseAuth auth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(MEMORY_TAG, "[CREATE] GroupFragment created");
        if (getArguments() != null) {
            groupId = getArguments().getString("GROUP_ID");
            groupName = getArguments().getString("GROUP_NAME", "Chat");
            Log.d(MEMORY_TAG, "[CREATE] args.groupId=" + groupId + ", args.groupName=" + groupName);
        } else {
            Log.w(MEMORY_TAG, "[CREATE] No arguments passed to GroupFragment");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(MEMORY_TAG, "[CREATE_VIEW] Creating GroupFragment view");
        binding = FragmentGroupBinding.inflate(inflater, container, false);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(GroupSettingsViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        Log.d(MEMORY_TAG, "[CREATE_VIEW] ViewModels initialized");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(MEMORY_TAG, "[VIEW_CREATED] groupId=" + groupId + ", authUid=" + auth.getUid());

        if (savedInstanceState != null) {
            isMemoryBannerDismissed = savedInstanceState.getBoolean("memory_banner_dismissed", false);
            Log.d(MEMORY_TAG, "[STATE] restored memory_banner_dismissed=" + isMemoryBannerDismissed);
        }

        setupTopBar();
        setupRecyclerView();
        setupInput();
        setupMemoryBanner();
        setupObservers();
        
        if (groupId != null) {
            Log.d(MEMORY_TAG, "[VIEW_CREATED] initChat(groupId=" + groupId + ")");
            settingsViewModel.loadGroupDetails(groupId);
            chatViewModel.initChat(groupId);
        } else {
            Log.e(MEMORY_TAG, "[VIEW_CREATED] groupId is null, cannot initialize chat");
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

    private void setupMemoryBanner() {
        binding.btnCloseMemoryBanner.setOnClickListener(v -> {
            Log.d(MEMORY_TAG, "[BANNER_DISMISSED] User closed memory banner");
            isMemoryBannerDismissed = true;
            renderMemoryBanner();
        });

        binding.memoryBannerCard.setOnClickListener(v -> {
            if (currentMemories == null || currentMemories.isEmpty()) {
                Log.w(MEMORY_TAG, "[BANNER_CLICKED] No memories available");
                return;
            }

            Log.d(MEMORY_TAG, "[BANNER_CLICKED] Opening memory story dialog with " + currentMemories.size() + " memories");
            MemoryStoryDialogFragment.newInstance(currentMemories, getTodayDateText())
                    .show(getChildFragmentManager(), "memory_story_dialog");
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

        chatViewModel.getTodayMemories().observe(getViewLifecycleOwner(), memories -> {
            int totalReceived = memories != null ? memories.size() : 0;
            Log.d(MEMORY_TAG, "[OBSERVER_RECEIVED] memoryCountFromApi=" + totalReceived);
            currentMemories = filterValidMemories(memories);
            int validCount = currentMemories.size();
            Log.d(MEMORY_TAG, "[FILTERED] validMemoryCount=" + validCount);
            if (!currentMemories.isEmpty()) {
                Log.d(MEMORY_TAG, "[SETTING_PREVIEW] Loading banner thumbnail from first valid memory");
                binding.tvMemoryBannerTitle.setText(getString(R.string.memory_on_date, getTodayDateText()));
                TodayMemory previewMemory = currentMemories.get(0);
                Glide.with(this)
                        .load(previewMemory.getPhotoUrl())
                        .placeholder(R.drawable.account_circle_24)
                        .centerCrop()
                        .into(binding.ivMemoryThumbnail);
            }
            renderMemoryBanner();
        });

        chatViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.w(MEMORY_TAG, "[ERROR_LIVEDATA] " + error);
            }
        });
    }

    private void renderMemoryBanner() {
        boolean shouldShow = !isMemoryBannerDismissed && currentMemories != null && !currentMemories.isEmpty();
        String reason = "";
        if (isMemoryBannerDismissed) {
            reason = "user dismissed";
        } else if (currentMemories == null) {
            reason = "memories is null";
        } else if (currentMemories.isEmpty()) {
            reason = "no valid memories";
        } else {
            reason = "all conditions met";
        }
        Log.d(MEMORY_TAG, "[RENDER_BANNER] show=" + shouldShow + ", reason=" + reason + ", memoryCount=" + (currentMemories != null ? currentMemories.size() : "null"));
        binding.memoryBannerCard.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private String getTodayDateText() {
        return DateFormat.getMediumDateFormat(requireContext()).format(new Date());
    }

    private List<TodayMemory> filterValidMemories(@Nullable List<TodayMemory> memories) {
        List<TodayMemory> validMemories = new ArrayList<>();
        if (memories == null) {
            return validMemories;
        }

        for (TodayMemory memory : memories) {
            if (memory != null && memory.getPhotoUrl() != null && !memory.getPhotoUrl().trim().isEmpty()) {
                validMemories.add(memory);
            } else {
                Log.w(MEMORY_TAG, "[FILTER_DROP] Ignored memory because photoUrl was null/empty");
            }
        }
        return validMemories;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("memory_banner_dismissed", isMemoryBannerDismissed);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
