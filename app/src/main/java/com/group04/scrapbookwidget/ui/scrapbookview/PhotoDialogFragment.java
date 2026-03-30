package com.group04.scrapbookwidget.ui.scrapbookview;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.ItemContent;
import com.group04.scrapbookwidget.databinding.FragmentPhotoDialogBinding;

import dagger.hilt.android.AndroidEntryPoint;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

@AndroidEntryPoint
public class PhotoDialogFragment extends DialogFragment {
    private ViewGroup container_front, container_back;
    private boolean isFront = true;
    private FragmentPhotoDialogBinding binding;
    private PhotoViewModel photoViewModel;
    private GestureDetector gestureDetector;
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
                Glide.with(view).load(content.photoUrl).into(binding.backSilhouette);
                binding.backSilhouette.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
                String caption = content.caption != null ? content.caption : "No caption";
                binding.hiddenText.setText(caption);
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
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.7f);
            }
        }
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