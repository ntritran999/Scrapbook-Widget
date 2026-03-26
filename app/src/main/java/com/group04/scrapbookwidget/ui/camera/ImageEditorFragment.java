package com.group04.scrapbookwidget.ui.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentImageEditorBinding;

import java.io.File;
import java.io.FileOutputStream;

public class ImageEditorFragment extends Fragment {

    private FragmentImageEditorBinding binding;
    private String photoPath;
    private boolean isMaskApplied = false;
    private boolean isDrawingMode = false;

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

        setupMainTools();
        setupBrushTools();
        setupColorPalette();

        binding.btnSave.setOnClickListener(view -> saveToGallery(photoPath));
        
        binding.btnPaste.setOnClickListener(view -> handlePasteToScrapbook());

        return binding.getRoot();
    }

    private void handlePasteToScrapbook() {
        if (binding.tornPaperFrame.getWidth() <= 0 || binding.tornPaperFrame.getHeight() <= 0) {
            Toast.makeText(getContext(), "Please wait!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Render view to bitmap
            Bitmap bitmap = Bitmap.createBitmap(binding.tornPaperFrame.getWidth(), 
                    binding.tornPaperFrame.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            binding.tornPaperFrame.draw(canvas);

            File outputDir = requireContext().getCacheDir();
            File imageFile = File.createTempFile("pasted_image", ".png", outputDir);
            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            NavController navController = NavHostFragment.findNavController(this);
            
            if (navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() == R.id.imageEditorFragment) {
                
                Bundle bundle = new Bundle();
                bundle.putString("PASTED_IMAGE_PATH", imageFile.getAbsolutePath());
                bundle.putString("GROUP_NAME", "Default Group");

                navController.navigate(R.id.action_imageEditorFragment_to_scrapbookViewFragment, bundle);
            } else {
                Toast.makeText(getContext(), "NAVIGATION ERROR: ", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMainTools() {
        binding.btnToolMask.setOnClickListener(view -> {
            isMaskApplied = !isMaskApplied;
            binding.tornPaperFrame.setMaskEnabled(isMaskApplied);
            updateToolButtonStyles();
        });

        binding.btnToolDraw.setOnClickListener(view -> {
            isDrawingMode = !isDrawingMode;
            binding.drawView.setDrawingEnabled(isDrawingMode);
            
            int visibility = isDrawingMode ? View.VISIBLE : View.GONE;
            binding.brushToolsLayout.setVisibility(visibility);
            binding.colorPaletteLayout.setVisibility(visibility);

            if (isDrawingMode) {
                selectPencil();
            }

            updateToolButtonStyles();
        });
    }

    private void setupBrushTools() {
        binding.btnBrushPencil.setOnClickListener(v -> selectPencil());
        binding.btnBrushEraser.setOnClickListener(v -> selectEraser());
    }

    private void selectPencil() {
        binding.drawView.setBrushType(ScrapbookDrawView.BrushType.PENCIL);
        binding.btnBrushPencil.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        binding.btnBrushPencil.setTextColor(Color.WHITE);
        
        binding.btnBrushEraser.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        binding.btnBrushEraser.setTextColor(Color.BLACK);
        
        binding.colorPaletteLayout.setAlpha(1.0f);
        enableColorPalette(true);
    }

    private void selectEraser() {
        binding.drawView.setBrushType(ScrapbookDrawView.BrushType.ERASER);
        binding.btnBrushEraser.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
        binding.btnBrushEraser.setTextColor(Color.WHITE);
        
        binding.btnBrushPencil.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        binding.btnBrushPencil.setTextColor(Color.BLACK);

        binding.colorPaletteLayout.setAlpha(0.3f);
        enableColorPalette(false);
    }

    private void enableColorPalette(boolean enabled) {
        for (int i = 0; i < binding.colorPaletteLayout.getChildCount(); i++) {
            binding.colorPaletteLayout.getChildAt(i).setEnabled(enabled);
        }
    }

    private void updateToolButtonStyles() {
        binding.btnToolMask.setBackgroundColor(isMaskApplied ? Color.parseColor("#4CAF50") : Color.parseColor("#222222"));
        binding.btnToolDraw.setBackgroundColor(isDrawingMode ? Color.parseColor("#4CAF50") : Color.parseColor("#222222"));
    }

    private void setupColorPalette() {
        binding.colorWhite.setOnClickListener(v -> binding.drawView.changeColor(Color.WHITE));
        binding.colorRed.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#F44336")));
        binding.colorYellow.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#FFEB3B")));
        binding.colorGreen.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#4CAF50")));
        binding.colorBlue.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#2196F3")));
        binding.colorBlack.setOnClickListener(v -> binding.drawView.changeColor(Color.BLACK));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
