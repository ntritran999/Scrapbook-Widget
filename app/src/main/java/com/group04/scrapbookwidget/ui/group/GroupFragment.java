package com.group04.scrapbookwidget.ui.group;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.databinding.FragmentGroupBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GroupFragment extends Fragment {
    private static final String MEMORY_TAG = "MemoryDebug";
    private FragmentGroupBinding binding;
    private GroupSettingsViewModel settingsViewModel;
    private ChatViewModel chatViewModel;
    private ChatMessageAdapter adapter;
    private SmartReplyAdapter smartReplyAdapter;
    private String groupId;
    private String groupName;
    private boolean isMemoryBannerDismissed = false;
    private List<TodayMemory> currentMemories = new ArrayList<>();
    private boolean suppressSmartReplyHide;

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

        if (savedInstanceState != null) {
            isMemoryBannerDismissed = savedInstanceState.getBoolean("memory_banner_dismissed", false);
        }

        setupTopBar();
        setupRecyclerView();
        setupInput();
        setupMemoryBanner();
        setupObservers();
        setupKeyboardHandling();
        
        if (groupId != null) {
            settingsViewModel.loadGroupDetails(groupId);
            chatViewModel.initChat(groupId);
        }
    }

    private void setupKeyboardHandling() {
        // Handle keyboard appearance for Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.inputLayout, (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Apply bottom padding based on keyboard height
            // When keyboard is hidden, we keep system bar padding (navigation bar)
            int bottomPadding = Math.max(ime.bottom, systemBars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomPadding);
            
            return insets;
        });
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

        smartReplyAdapter = new SmartReplyAdapter(this::applySuggestedReply);
        LinearLayoutManager smartReplyLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        binding.rvSmartReplies.setLayoutManager(smartReplyLayoutManager);
        binding.rvSmartReplies.setAdapter(smartReplyAdapter);

        adapter.setOnMessageVisibleListener(message -> {
            chatViewModel.markMessageAsSeen(message.getId());
        });

        adapter.setOnResendClickListener(message -> {
            chatViewModel.resendMessage(message);
        });

        binding.rvChat.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                binding.rvChat.postDelayed(() -> {
                    if (binding != null && adapter.getItemCount() > 0) {
                        binding.rvChat.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }, 100);
            }
        });
    }

    private void setupInput() {
        binding.btnSend.setOnClickListener(v -> {
            String content = binding.etMessage.getText().toString().trim();
            if (!content.isEmpty()) {
                chatViewModel.sendMessage(content);
                binding.etMessage.setText("");
                hideSmartReplies();
                chatViewModel.clearSuggestedReplies();
            }
        });

        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressSmartReplyHide) {
                    return;
                }
                if (s != null && s.length() > 0) {
                    hideSmartReplies();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupMemoryBanner() {
        binding.btnCloseMemoryBanner.setOnClickListener(v -> {
            isMemoryBannerDismissed = true;
            renderMemoryBanner();
        });

        binding.memoryBannerCard.setOnClickListener(v -> {
            if (currentMemories != null && !currentMemories.isEmpty()) {
                MemoryStoryDialogFragment.newInstance(currentMemories, getTodayDateText())
                        .show(getChildFragmentManager(), "memory_story_dialog");
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

        chatViewModel.getMessageWrappers().observe(getViewLifecycleOwner(), wrappers -> {
            if (wrappers != null && !wrappers.isEmpty()) {
                int oldSize = adapter.getItemCount();
                boolean isAtBottom = isLastItemVisible();
                
                Message lastMessage = wrappers.get(wrappers.size() - 1).message;
                boolean isOwnMessage = auth.getUid() != null && auth.getUid().equals(lastMessage.getCreatedBy());

                adapter.submitList(wrappers, () -> {
                    if (binding == null) return;
                    if (oldSize == 0) {
                        // Initial load
                        binding.rvChat.scrollToPosition(adapter.getItemCount() - 1);
                    } else if (isOwnMessage || isAtBottom) {
                        // Scroll to bottom if we sent the message, or if we were already at the bottom
                        binding.rvChat.scrollToPosition(adapter.getItemCount() - 1);
                    }
                });

                // Generate replies only for new messages and if input is empty
                if (wrappers.size() > oldSize && (binding.etMessage.getText() == null || binding.etMessage.getText().length() == 0)) {
                    List<Message> messages = new ArrayList<>();
                    for (MessageWrapper w : wrappers) messages.add(w.message);
                    chatViewModel.generateReplies(messages, auth.getUid());
                }
            }
        });

        chatViewModel.getSuggestedReplies().observe(getViewLifecycleOwner(), suggestions -> {
            if (suggestions == null || suggestions.isEmpty()) {
                smartReplyAdapter.setSuggestions(new ArrayList<>());
                binding.rvSmartReplies.setVisibility(View.GONE);
                return;
            }

            smartReplyAdapter.setSuggestions(suggestions);
            binding.rvSmartReplies.setVisibility(View.VISIBLE);
        });

        chatViewModel.getTodayMemories().observe(getViewLifecycleOwner(), memories -> {
            currentMemories = filterValidMemories(memories);
            if (!currentMemories.isEmpty()) {
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

        chatViewModel.getMarkAsSeenResponse().observe(getViewLifecycleOwner(), seenBy -> {
            if (seenBy != null && seenBy.getUserId().equals(auth.getUid())) {
                settingsViewModel.updateUnreadCount(groupId, seenBy.getUnreadCount());
            }
        });
    }

    private boolean isLastItemVisible() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.rvChat.getLayoutManager();
        if (layoutManager != null && adapter.getItemCount() > 0) {
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            // Scroll if within last 2 items
            return lastVisiblePosition >= adapter.getItemCount() - 2;
        }
        return true;
    }

    private void renderMemoryBanner() {
        boolean shouldShow = !isMemoryBannerDismissed && currentMemories != null && !currentMemories.isEmpty();
        binding.memoryBannerCard.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private String getTodayDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
        return sdf.format(new Date());
    }

    private List<TodayMemory> filterValidMemories(@Nullable List<TodayMemory> memories) {
        List<TodayMemory> validMemories = new ArrayList<>();
        if (memories == null) return validMemories;
        for (TodayMemory memory : memories) {
            if (memory != null && memory.getPhotoUrl() != null && !memory.getPhotoUrl().trim().isEmpty()) {
                validMemories.add(memory);
            }
        }
        return validMemories;
    }

    private void applySuggestedReply(String text) {
        if (binding == null || text == null) return;
        suppressSmartReplyHide = true;
        binding.etMessage.setText(text);
        binding.etMessage.setSelection(text.length());
        suppressSmartReplyHide = false;
        hideSmartReplies();
        chatViewModel.clearSuggestedReplies();
    }

    private void applyWindowInsetsToBottom(View view, WindowInsetsCompat insets) {
        Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
        Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        int bottom = Math.max(imeInsets.bottom, systemBarsInsets.bottom);
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottom);
    }

    private void hideSmartReplies() {
        if (binding != null) binding.rvSmartReplies.setVisibility(View.GONE);
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
