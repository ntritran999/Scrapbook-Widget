package com.group04.scrapbookwidget.ui.camera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentImageEditorBinding;
import com.group04.scrapbookwidget.ml.FaceEmbeddingManager;
import com.group04.scrapbookwidget.ui.scrapbookview.CaptionInputDialogFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ImageEditorFragment extends Fragment {
    private static final String FACE_EMBEDDINGS_KEY = "FACE_EMBEDDINGS";
    private static final String PHOTO_PATH_KEY = "PHOTO_PATH";
    private static final String ORIGINAL_PHOTO_PATH_KEY = "ORIGINAL_PHOTO_PATH";
    private static final String DRAFT_PREF_NAME = "IMAGE_EDITOR_DRAFT";
    private static final String DRAFT_SOURCE_KEY = "DRAFT_SOURCE_PATH";
    private static final String DRAFT_PATH_KEY = "DRAFT_PATH";
    private static final String DRAFT_CAPTION_KEY = "DRAFT_CAPTION";

    private FragmentImageEditorBinding binding;
    private String photoPath;
    private String originalPhotoPath;
    private boolean isMaskApplied = false;
    private boolean isDrawingMode = false;
    private static final String PREF_NAME = "tmp_pref";
    
    private String groupId = "";
    private String pageId = "";
    private String currentCaption = "";
    private String pendingImagePath = "";
    
    @Inject
    FaceEmbeddingManager faceEmbeddingManager;
    
    private boolean isExtractingFaces = false;
    private boolean hasStartedFaceExtraction = false;
    private boolean shouldNavigateAfterFaceExtraction = false;
    private ArrayList<ArrayList<Double>> pendingFaceEmbeddings;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private boolean saveToGallery(String cachedPhotoPath) {
        if (cachedPhotoPath == null) {
            Toast.makeText(requireContext(), "Cached photo not found!", Toast.LENGTH_SHORT).show();
            return false;
        }

        java.io.File sourceFile = new java.io.File(cachedPhotoPath);
        if (!sourceFile.exists()) return false;

        String fileName = "Scrapbook_" + System.currentTimeMillis() + ".png";

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
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
                Snackbar.make(binding.container, "Saved to gallery.", Snackbar.LENGTH_SHORT).show();
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
            originalPhotoPath = photoPath;
        }
        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString(PHOTO_PATH_KEY, photoPath);
            originalPhotoPath = savedInstanceState.getString(ORIGINAL_PHOTO_PATH_KEY, originalPhotoPath);
            pendingImagePath = savedInstanceState.getString("PASTED_IMAGE_PATH", "");
            currentCaption = savedInstanceState.getString("CAPTION", "");
            Serializable serializedEmbeddings = savedInstanceState.getSerializable(FACE_EMBEDDINGS_KEY);
            if (serializedEmbeddings instanceof ArrayList<?>) {
                //noinspection unchecked
                pendingFaceEmbeddings = (ArrayList<ArrayList<Double>>) serializedEmbeddings;
            }
        }

        restoreDraftForCurrentPhoto();
        
        groupId = requireActivity().getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE)
                .getString("CURRENT_GROUP_ID", "");
        pageId = requireActivity().getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE)
                .getString("CURRENT_PAGE_ID", "");
        connectivityManager = ContextCompat.getSystemService(requireContext(), ConnectivityManager.class);

        // FaceEmbeddingManager is injected by Hilt and already initialized
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageEditorBinding.inflate(inflater, container, false);

        if (photoPath != null) {
            binding.imgPreview.setImageURI(Uri.parse(photoPath));
        }

        binding.imagePreviewOverlay.setBackgroundColor(Color.BLACK);

        boolean[] isDevelopingPolaroid = {true};
        new Thread(() -> {
            int duration = 2_000;
            int steps = 10;
            int interval = duration / steps;
            float alpha = 1.0f / steps;
            for (int i = 0; i < steps; i++) {
                int color = Color.argb(1 - alpha, 1, 1, 1);
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.imagePreviewOverlay.setBackgroundColor(color);
                    }
                });

                alpha += 1.0f / steps;
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    if (binding != null) {
                        Snackbar.make(binding.container, "Please try again later.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
            isDevelopingPolaroid[0] = false;
        }).start();

        setupMainTools();
        setupBrushTools();
        setupColorPalette();

        binding.btnSave.setOnClickListener(view -> {
            if (!isDevelopingPolaroid[0]) {
                String exportPath = cacheCurrentEditorState("saved_image", false);
                if (exportPath != null) {
                    saveToGallery(exportPath);
                }
            }
        });
        
        binding.btnPaste.setOnClickListener(view -> {
            if (!isDevelopingPolaroid[0]) {
                handlePasteToScrapbook();
            }
        });

        binding.btnBack.setOnClickListener(view -> {
            clearDraftState();
            Navigation.findNavController(requireView()).navigate(R.id.cameraFragment);
        });

        refreshPasteActionState();
        startFaceExtractionIfNeeded();

        return binding.getRoot();
    }

    private void handlePasteToScrapbook() {
        if (!isOnline()) {
            cacheCurrentEditorState("offline_editor_draft", true);
            Snackbar.make(
                    binding.container,
                    "You're offline. The edited photo is saved locally, but sticking it into the scrapbook needs internet.",
                    Snackbar.LENGTH_LONG
            ).show();
            refreshPasteActionState();
            return;
        }

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

            bitmap.recycle();

            // Store the image path and show caption dialog
            pendingImagePath = imageFile.getAbsolutePath();
            showCaptionInputDialog();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "ERROR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCaptionInputDialog() {
        CaptionInputDialogFragment captionDialog = CaptionInputDialogFragment.newInstance();
        captionDialog.setOnCaptionConfirmedListener(new CaptionInputDialogFragment.OnCaptionConfirmedListener() {
            @Override
            public void onCaptionConfirmed(String caption) {
                currentCaption = caption;
                continueToScrapbookWhenFaceExtractionReady();
            }

            @Override
            public void onCaptionCancelled() {
                currentCaption = "";
                continueToScrapbookWhenFaceExtractionReady();
            }
        });
        captionDialog.show(getChildFragmentManager(), "CaptionInputDialog");
    }

    private void startFaceExtractionIfNeeded() {
        if (hasStartedFaceExtraction || pendingFaceEmbeddings != null) {
            return;
        }

        String faceSourcePath = (originalPhotoPath != null && !originalPhotoPath.isEmpty())
                ? originalPhotoPath
                : photoPath;
        if (faceSourcePath == null || faceSourcePath.isEmpty()) {
            return;
        }

        // Respect user setting: skip face embedding extraction when AI features disabled
        try {
            boolean aiEnabled = requireActivity().getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)
                    .getBoolean("AI_FEATURES_ENABLED", true);
            if (!aiEnabled) {
                // Do not start background extraction when disabled
                android.util.Log.d("ImageEditorFragment", "AI features disabled - skipping face extraction");
                hasStartedFaceExtraction = true;
                isExtractingFaces = false;
                return;
            }
        } catch (Exception e) {
            // If preference access fails, default to enabled behavior (do nothing)
        }

        hasStartedFaceExtraction = true;
        isExtractingFaces = true;

        Bitmap sourceBitmap = loadBitmapForFaceExtraction(faceSourcePath);
        if (sourceBitmap == null) {
            android.util.Log.e("ImageEditorFragment", "Could not decode source photo for face extraction");
            return;
        }

        faceEmbeddingManager.extractFacesFromPhoto(sourceBitmap, new FaceEmbeddingManager.GroupPhotoCallback() {
            @Override
            public void onFacesExtracted(List<List<Double>> embeddings) {
                sourceBitmap.recycle();
                pendingFaceEmbeddings = toSerializableEmbeddings(embeddings);
                android.util.Log.d("ImageEditorFragment", "onFacesExtracted: faces=" + (embeddings != null ? embeddings.size() : "null"));
                finishFaceExtraction();
            }

            @Override
            public void onExtractionError(String error) {
                sourceBitmap.recycle();
                pendingFaceEmbeddings = null;
                android.util.Log.e("ImageEditorFragment", "extractFacesFromPhoto failed: " + error);
                finishFaceExtraction();
            }
        });
    }

    private void continueToScrapbookWhenFaceExtractionReady() {
        if (!hasStartedFaceExtraction) {
            startFaceExtractionIfNeeded();
        }

        if (isExtractingFaces) {
            shouldNavigateAfterFaceExtraction = true;
            Toast.makeText(getContext(), "Analyzing faces...", Toast.LENGTH_SHORT).show();
            return;
        }

        navigateToScrapbook();
    }

    private void finishFaceExtraction() {
        isExtractingFaces = false;
        if (shouldNavigateAfterFaceExtraction) {
            shouldNavigateAfterFaceExtraction = false;
            navigateToScrapbook();
        }
    }

    private void navigateToScrapbook() {
        NavController navController = NavHostFragment.findNavController(this);
        
        if (navController.getCurrentDestination() != null && 
            navController.getCurrentDestination().getId() == R.id.imageEditorFragment) {
            
            Bundle bundle = new Bundle();
            bundle.putString("PASTED_IMAGE_PATH", pendingImagePath);
            bundle.putString("GROUP_ID", groupId);
            bundle.putString("PAGE_ID", pageId);
            bundle.putString("CAPTION", currentCaption);
            bundle.putSerializable(FACE_EMBEDDINGS_KEY, pendingFaceEmbeddings);

            android.util.Log.d("ImageEditorFragment", "Navigate with - groupId: " + groupId + ", pageId: " + pageId + ", caption: " + currentCaption);

            clearDraftState();
            navController.navigate(R.id.action_imageEditorFragment_to_scrapbookViewFragment, bundle);
        } else {
            Toast.makeText(getContext(), "NAVIGATION ERROR: ", Toast.LENGTH_SHORT).show();
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
        binding.btnBrushPencil.setBackgroundResource(R.drawable.bg_editor_chip_active);
        binding.btnBrushPencil.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        
        binding.btnBrushEraser.setBackgroundResource(R.drawable.bg_editor_chip);
        binding.btnBrushEraser.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        
        binding.colorPaletteLayout.setAlpha(1.0f);
        enableColorPalette(true);
    }

    private void selectEraser() {
        binding.drawView.setBrushType(ScrapbookDrawView.BrushType.ERASER);
        binding.btnBrushEraser.setBackgroundResource(R.drawable.bg_editor_chip_active);
        binding.btnBrushEraser.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        
        binding.btnBrushPencil.setBackgroundResource(R.drawable.bg_editor_chip);
        binding.btnBrushPencil.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        binding.colorPaletteLayout.setAlpha(0.3f);
        enableColorPalette(false);
    }

    private void enableColorPalette(boolean enabled) {
        for (int i = 0; i < binding.colorPaletteLayout.getChildCount(); i++) {
            binding.colorPaletteLayout.getChildAt(i).setEnabled(enabled);
        }
    }

    private void updateToolButtonStyles() {
        binding.btnToolMask.setBackgroundResource(isMaskApplied
                ? R.drawable.bg_editor_chip_active
                : R.drawable.bg_editor_chip);
        binding.btnToolDraw.setBackgroundResource(isDrawingMode
                ? R.drawable.bg_editor_chip_active
                : R.drawable.bg_editor_chip);
        binding.btnToolMask.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        binding.btnToolDraw.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }

    private void setupColorPalette() {
        binding.colorWhite.setOnClickListener(v -> binding.drawView.changeColor(Color.WHITE));
        binding.colorRed.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#F44336")));
        binding.colorYellow.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#FFEB3B")));
        binding.colorGreen.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#4CAF50")));
        binding.colorBlue.setOnClickListener(v -> binding.drawView.changeColor(Color.parseColor("#2196F3")));
        binding.colorBlack.setOnClickListener(v -> binding.drawView.changeColor(Color.BLACK));
    }

    @Nullable
    private Bitmap loadBitmapForFaceExtraction(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Uri uri = Uri.parse(path);
            Bitmap bitmap;
            if (uri.getScheme() != null) {
                try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                    bitmap = inputStream != null ? BitmapFactory.decodeStream(inputStream, null, options) : null;
                }
            } else {
                bitmap = BitmapFactory.decodeFile(path, options);
            }

            if (bitmap == null) {
                return null;
            }

            int exifRotation = readExifRotation(path);
            return exifRotation == 0 ? bitmap : rotateBitmap(bitmap, exifRotation);
        } catch (Exception e) {
            android.util.Log.e("ImageEditorFragment", "loadBitmapForFaceExtraction failed: " + e.getMessage(), e);
            return null;
        }
    }

    private int readExifRotation(@NonNull String path) {
        try {
            Uri uri = Uri.parse(path);
            ExifInterface exifInterface;
            if (uri.getScheme() != null) {
                try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                    if (inputStream == null) {
                        return 0;
                    }
                    exifInterface = new ExifInterface(inputStream);
                }
            } else {
                try (InputStream inputStream = new FileInputStream(path)) {
                    exifInterface = new ExifInterface(inputStream);
                }
            }

            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            android.util.Log.w("ImageEditorFragment", "Could not read EXIF rotation for face extraction", e);
            return 0;
        }
    }

    @NonNull
    private Bitmap rotateBitmap(@NonNull Bitmap bitmap, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.getWidth(),
                bitmap.getHeight(),
                matrix,
                true
        );
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    @Nullable
    private ArrayList<ArrayList<Double>> toSerializableEmbeddings(@Nullable List<List<Double>> embeddings) {
        if (embeddings == null) {
            return null;
        }

        ArrayList<ArrayList<Double>> serializableEmbeddings = new ArrayList<>();
        for (List<Double> embedding : embeddings) {
            serializableEmbeddings.add(embedding != null ? new ArrayList<>(embedding) : new ArrayList<>());
        }
        return serializableEmbeddings;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        String cachedStatePath = cacheCurrentEditorState("editor_state", true);
        if (cachedStatePath != null) {
            photoPath = cachedStatePath;
        }
        outState.putString(PHOTO_PATH_KEY, photoPath);
        outState.putString(ORIGINAL_PHOTO_PATH_KEY, originalPhotoPath);
        outState.putString("PASTED_IMAGE_PATH", pendingImagePath);
        outState.putString("CAPTION", currentCaption);
        outState.putSerializable(FACE_EMBEDDINGS_KEY, pendingFaceEmbeddings);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPasteActionState();
    }

    @Override
    public void onStart() {
        super.onStart();
        registerNetworkCallback();
    }

    @Override
    public void onStop() {
        cacheCurrentEditorState("editor_state", true);
        unregisterNetworkCallback();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Nullable
    private String cacheCurrentEditorState(@NonNull String filePrefix, boolean persistDraft) {
        if (binding == null || binding.tornPaperFrame.getWidth() <= 0 || binding.tornPaperFrame.getHeight() <= 0) {
            return null;
        }

        try {
            Bitmap bitmap = Bitmap.createBitmap(
                    binding.tornPaperFrame.getWidth(),
                    binding.tornPaperFrame.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            binding.tornPaperFrame.draw(canvas);

            File draftFile = File.createTempFile(filePrefix, ".png", requireContext().getCacheDir());
            try (FileOutputStream out = new FileOutputStream(draftFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            bitmap.recycle();

            photoPath = draftFile.getAbsolutePath();
            if (persistDraft) {
                persistDraftState(photoPath);
            }
            return photoPath;
        } catch (Exception e) {
            android.util.Log.e("ImageEditorFragment", "cacheCurrentEditorState failed", e);
            return null;
        }
    }

    private void persistDraftState(@NonNull String draftPath) {
        SharedPreferences preferences = requireContext().getSharedPreferences(DRAFT_PREF_NAME, Context.MODE_PRIVATE);
        String previousDraftPath = preferences.getString(DRAFT_PATH_KEY, "");
        if (previousDraftPath != null && !previousDraftPath.isEmpty() && !previousDraftPath.equals(draftPath)) {
            File previousDraftFile = new File(previousDraftPath);
            if (previousDraftFile.exists()) {
                // Replace the old cached snapshot so one editor session keeps a single draft file.
                //noinspection ResultOfMethodCallIgnored
                previousDraftFile.delete();
            }
        }

        preferences.edit()
                .putString(DRAFT_SOURCE_KEY, originalPhotoPath)
                .putString(DRAFT_PATH_KEY, draftPath)
                .putString(DRAFT_CAPTION_KEY, currentCaption)
                .apply();
    }

    private void restoreDraftForCurrentPhoto() {
        if (originalPhotoPath == null || originalPhotoPath.isEmpty()) {
            return;
        }

        SharedPreferences draftPreferences = requireContext().getSharedPreferences(DRAFT_PREF_NAME, Context.MODE_PRIVATE);
        String draftSourcePath = draftPreferences.getString(DRAFT_SOURCE_KEY, "");
        String draftPath = draftPreferences.getString(DRAFT_PATH_KEY, "");
        if (!originalPhotoPath.equals(draftSourcePath) || draftPath == null || draftPath.isEmpty()) {
            return;
        }

        File draftFile = new File(draftPath);
        if (!draftFile.exists()) {
            clearDraftState();
            return;
        }

        photoPath = draftPath;
        currentCaption = draftPreferences.getString(DRAFT_CAPTION_KEY, currentCaption);
    }

    private void clearDraftState() {
        SharedPreferences preferences = requireContext().getSharedPreferences(DRAFT_PREF_NAME, Context.MODE_PRIVATE);
        String draftPath = preferences.getString(DRAFT_PATH_KEY, "");
        if (draftPath != null && !draftPath.isEmpty()) {
            File draftFile = new File(draftPath);
            if (draftFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                draftFile.delete();
            }
        }

        preferences.edit()
                .remove(DRAFT_SOURCE_KEY)
                .remove(DRAFT_PATH_KEY)
                .remove(DRAFT_CAPTION_KEY)
                .apply();
    }

    private void refreshPasteActionState() {
        if (binding == null) {
            return;
        }

        boolean online = isOnline();
        binding.btnPaste.setText(online ? "Stick Here" : "Need Internet");
        binding.btnPaste.setAlpha(online ? 1f : 0.85f);
    }

    private boolean isOnline() {
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void registerNetworkCallback() {
        if (connectivityManager == null || networkCallback != null) {
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                postRefreshPasteActionState();
            }

            @Override
            public void onLost(@NonNull Network network) {
                postRefreshPasteActionState();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                postRefreshPasteActionState();
            }
        };

        try {
            connectivityManager.registerNetworkCallback(
                    new NetworkRequest.Builder()
                            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .build(),
                    networkCallback
            );
        } catch (Exception e) {
            android.util.Log.w("ImageEditorFragment", "registerNetworkCallback failed", e);
            networkCallback = null;
        }
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager == null || networkCallback == null) {
            return;
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception e) {
            android.util.Log.w("ImageEditorFragment", "unregisterNetworkCallback failed", e);
        } finally {
            networkCallback = null;
        }
    }

    private void postRefreshPasteActionState() {
        if (binding == null) {
            return;
        }

        binding.getRoot().post(this::refreshPasteActionState);
    }
}
