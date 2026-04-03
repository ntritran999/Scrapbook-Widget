package com.group04.scrapbookwidget.ui.group;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.databinding.FragmentMemoryStoryDialogBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoryStoryDialogFragment extends DialogFragment {
    private static final String ARG_DATE_TEXT = "date_text";
    private static final String ARG_CREATED_ATS = "created_ats";
    private static final String ARG_PHOTO_URLS = "photo_urls";
    private static final String ARG_TAGGED_NAMES = "tagged_names";
    private static final long STORY_INTERVAL_MS = 3000L;

    private FragmentMemoryStoryDialogBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable advanceRunnable = this::advanceStory;
    private ArrayList<String> createdAts;
    private ArrayList<String> photoUrls;
    private ArrayList<String> taggedNames;
    private String dateText;
    private int currentIndex;

    public static MemoryStoryDialogFragment newInstance(@NonNull List<TodayMemory> memories, @NonNull String dateText) {
        MemoryStoryDialogFragment fragment = new MemoryStoryDialogFragment();
        Bundle args = new Bundle();
        ArrayList<String> createdAts = new ArrayList<>();
        ArrayList<String> photoUrls = new ArrayList<>();
        ArrayList<String> taggedNames = new ArrayList<>();
        for (TodayMemory memory : memories) {
            if (memory == null || memory.getPhotoUrl() == null || memory.getPhotoUrl().trim().isEmpty()) {
                continue;
            }
            createdAts.add(normalize(memory.getCreatedAt()));
            photoUrls.add(memory.getPhotoUrl());
            taggedNames.add(joinNames(memory.getTaggedUsernames()));
        }
        args.putString(ARG_DATE_TEXT, dateText);
        args.putStringArrayList(ARG_CREATED_ATS, createdAts);
        args.putStringArrayList(ARG_PHOTO_URLS, photoUrls);
        args.putStringArrayList(ARG_TAGGED_NAMES, taggedNames);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMemoryStoryDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        createdAts = args != null ? args.getStringArrayList(ARG_CREATED_ATS) : new ArrayList<>();
        photoUrls = args != null ? args.getStringArrayList(ARG_PHOTO_URLS) : new ArrayList<>();
        taggedNames = args != null ? args.getStringArrayList(ARG_TAGGED_NAMES) : new ArrayList<>();
        dateText = args != null ? args.getString(ARG_DATE_TEXT) : DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(new Date());

        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt("current_index", 0);
        }

        binding.btnCloseStory.setOnClickListener(v -> dismissAllowingStateLoss());
        setupProgressIndicators();

        if (photoUrls == null || photoUrls.isEmpty()) {
            dismissAllowingStateLoss();
            return;
        }

        showStory(Math.min(currentIndex, photoUrls.size() - 1));
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.2f);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scheduleAdvance();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(advanceRunnable);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(advanceRunnable);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_index", currentIndex);
    }

    private void setupProgressIndicators() {
        binding.progressContainer.removeAllViews();
        if (photoUrls == null || photoUrls.isEmpty()) {
            return;
        }

        float weight = 1f / photoUrls.size();
        for (int i = 0; i < photoUrls.size(); i++) {
            View track = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
            if (i > 0) {
                params.setMarginStart(dpToPx(6));
            }
            track.setLayoutParams(params);
            track.setBackgroundResource(R.drawable.bg_memory_progress_track);
            binding.progressContainer.addView(track);
        }
    }

    private void showStory(int index) {
        if (binding == null || photoUrls == null || index < 0 || index >= photoUrls.size()) {
            return;
        }

        currentIndex = index;
        Glide.with(this)
                .load(photoUrls.get(index))
                .placeholder(R.drawable.account_circle_24)
                .into(binding.ivStoryPhoto);

        String tagged = taggedNames != null && index < taggedNames.size() ? taggedNames.get(index) : "";
        if (TextUtils.isEmpty(tagged)) {
            tagged = getString(R.string.memory_story_tagged_empty);
        }
        String storyCreatedAt = createdAts != null && index < createdAts.size() ? createdAts.get(index) : "";
        if (TextUtils.isEmpty(storyCreatedAt)) {
            storyCreatedAt = dateText;
        }
        binding.tvStoryHeader.setText(getString(R.string.memory_story_meta, storyCreatedAt, tagged));
        binding.tvStoryCounter.setText(String.format(Locale.getDefault(), "%d / %d", index + 1, photoUrls.size()));
        updateProgressIndicators(index);
        scheduleAdvance();
    }

    private void updateProgressIndicators(int activeIndex) {
        for (int i = 0; i < binding.progressContainer.getChildCount(); i++) {
            View child = binding.progressContainer.getChildAt(i);
            child.setBackgroundResource(i <= activeIndex
                    ? R.drawable.bg_memory_progress_fill
                    : R.drawable.bg_memory_progress_track);
        }
    }

    private void scheduleAdvance() {
        handler.removeCallbacks(advanceRunnable);
        if (photoUrls != null && !photoUrls.isEmpty()) {
            handler.postDelayed(advanceRunnable, STORY_INTERVAL_MS);
        }
    }

    private void advanceStory() {
        if (photoUrls == null || currentIndex >= photoUrls.size() - 1) {
            dismissAllowingStateLoss();
            return;
        }
        showStory(currentIndex + 1);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private static String joinNames(@Nullable List<String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        List<String> normalized = new ArrayList<>();
        for (String name : names) {
            if (name != null) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return TextUtils.join(", ", normalized);
    }

    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
