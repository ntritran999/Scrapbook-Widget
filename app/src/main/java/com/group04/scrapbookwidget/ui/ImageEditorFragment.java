package com.group04.scrapbookwidget.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.group04.scrapbookwidget.databinding.FragmentImageEditorBinding;

public class ImageEditorFragment extends Fragment {

    private FragmentImageEditorBinding binding;
    private String photoPath;
    private boolean isMaskApplied = false;

    private boolean saveToGallery(String cachedPhotoPath) {
        if (cachedPhotoPath == null) {
            Toast.makeText(requireContext(), "Cached photo not found!", Toast.LENGTH_SHORT).show();
            return false;
        }

        java.io.File sourceFile = new java.io.File(cachedPhotoPath);
        if (!sourceFile.exists()) return false;

        String fileName = "Scrapbook_" + System.currentTimeMillis() + ".jpg";

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Scrapbook");

        android.net.Uri uri = requireContext().getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (java.io.OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
                 java.io.FileInputStream fis = new java.io.FileInputStream(sourceFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

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

        binding.btnToolMask.setOnClickListener(view -> {
            isMaskApplied = !isMaskApplied;

            binding.imgPreview.setMaskEnabled(isMaskApplied); // ScrapbookMaskView

            if (isMaskApplied) {
                binding.btnToolMask.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); // Màu xanh
            } else {
                binding.btnToolMask.setBackgroundColor(android.graphics.Color.parseColor("#222222")); // Trở về màu xám cũ
            }
        });

        binding.btnSave.setOnClickListener(view -> {
            boolean isSaved = saveToGallery(photoPath);
            if (isSaved) {
                binding.btnSave.setEnabled(false);
                binding.btnSave.setAlpha(0.5f);
            }
        });

        return binding.getRoot();
    }
}