package com.group04.scrapbookwidget.ui;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentPhotoDialogBinding;

public class PhotoDialogFragment extends DialogFragment {
    private ViewGroup container_front, container_back;
    private boolean isFront = true;
    public static String TAG = "PhotoDialog";
    public PhotoDialogFragment() {
        // Required empty public constructor
    }

    public static PhotoDialogFragment newInstance(String param1, String param2) {
        PhotoDialogFragment fragment = new PhotoDialogFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentPhotoDialogBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_dialog, container, false);
        container_front = binding.photoFront;
        container_back = binding.photoBack;
        binding.container.setOnClickListener(v -> {
            flip_photo();
        });
        return binding.getRoot();
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