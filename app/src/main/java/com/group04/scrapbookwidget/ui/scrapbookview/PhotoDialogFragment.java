package com.group04.scrapbookwidget.ui.scrapbookview;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.ItemContent;
import com.group04.scrapbookwidget.databinding.FragmentPhotoDialogBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PhotoDialogFragment extends DialogFragment {
    private ViewGroup container_front, container_back;
    private boolean isFront = true;
    private FragmentPhotoDialogBinding binding;
    private PhotoViewModel photoViewModel;
    private static final String GROUP_ID = "groupId";
    private static final String PAGE_ID = "pageId";
    private static final String ITEM_ID = "itemId";
    private String groupId, pageId, itemId;
    public static String TAG = "PhotoDialog";
    public PhotoDialogFragment() {
        // Required empty public constructor
    }

    public static PhotoDialogFragment newInstance(String groupId, String pageId, String itemId) {
        PhotoDialogFragment fragment = new PhotoDialogFragment();
        Bundle args = new Bundle();
        args.putString(GROUP_ID, groupId);
        args.putString(PAGE_ID, pageId);
        args.putString(ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            groupId = bundle.getString(GROUP_ID);
            pageId = bundle.getString(PAGE_ID);
            itemId = bundle.getString(ITEM_ID);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        photoViewModel =  new ViewModelProvider(this).get(PhotoViewModel.class);
        photoViewModel.getItemLiveData().observe(getViewLifecycleOwner(), item -> {
            if (item != null) {
                ItemContent content = item.getContent();
                Glide.with(view).load(content.photoUrl).into(binding.zoomPhoto);
                binding.hiddenText.setText(content.caption);
            }
        });
        photoViewModel.loadItem(groupId, pageId, itemId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_dialog, container, false);
        container_front = binding.photoFront;
        container_back = binding.photoBack;
        binding.container.setOnClickListener(v -> {
            flip_photo();
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void flip_photo() {
        AnimatorSet setIn = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.photo_flip_in);
        AnimatorSet setOut = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.photo_flip_out);

        if (isFront) {
            setIn.setTarget(container_back);
            setOut.setTarget(container_front);
        }
        else {
            setIn.setTarget(container_front);
            setOut.setTarget(container_back);
        }

        setIn.start();
        setOut.start();

        isFront = !isFront;
    }
}