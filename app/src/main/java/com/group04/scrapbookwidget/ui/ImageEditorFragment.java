package com.group04.scrapbookwidget.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.group04.scrapbookwidget.databinding.FragmentImageEditorBinding;

public class ImageEditorFragment extends Fragment {

    private FragmentImageEditorBinding binding;
    private String photoPath;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            photoPath = getArguments().getString("PHOTO_PATH");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageEditorBinding.inflate(inflater, container, false);

        if (photoPath != null) {
            binding.imgPreview.setImageURI(Uri.parse(photoPath));
        }

        final boolean[] isMaskApplied = {false};

        binding.btnToolMask.setOnClickListener(view -> {
            isMaskApplied[0] = !isMaskApplied[0];

            binding.imgPreview.setMaskEnabled(isMaskApplied[0]); // ScrapbookMaskView

            if (isMaskApplied[0]) {
                binding.btnToolMask.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); // Màu xanh
            } else {
                binding.btnToolMask.setBackgroundColor(android.graphics.Color.parseColor("#222222")); // Trở về màu xám cũ
            }
        });

        return binding.getRoot();
    }
}