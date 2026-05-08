package com.group04.scrapbookwidget.ui.camera;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentCameraBinding;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;

    private ImageCapture imageCapture;

    private Camera camera;

    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    private int flashMode = ImageCapture.FLASH_MODE_OFF;

    private SharedPreferences preferences;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Permission request denied.", Toast.LENGTH_SHORT).show();
                }
            }); // listener for a request-permission dialog on closing, INPUT: RequestPermission(), OUTpuT: boolean result

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        
        // Use COMPATIBLE mode to use TextureView, which is more reliable for UI overlays on some devices
        binding.viewFinder.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);

        // Check if this app is granted CAMERA permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            // if not granted, show popup request
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        binding.btnFlipCamera.setOnClickListener(view -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        // shutter button
        binding.btnShutter.setOnClickListener(view -> {
            takePhoto();
        });

        // flash button
        binding.btnFlash.setOnClickListener(view -> {
            if (!hasFlashUnit()) {
                flashMode = ImageCapture.FLASH_MODE_OFF;
                updateFlashButtonState();
                Toast.makeText(requireContext(), "Flash is not available on this camera.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (flashMode == ImageCapture.FLASH_MODE_OFF) {
                flashMode = ImageCapture.FLASH_MODE_ON;
            } else {
                flashMode = ImageCapture.FLASH_MODE_OFF;
            }

            applyFlashMode();
            updateFlashButtonState();
        });

        preferences = requireContext().getSharedPreferences("TMP_USER_SESSION", Context.MODE_PRIVATE);
        return binding.getRoot();
    }

    // camera setup
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // viewFinder setup
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                // imageCapture setup
                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(flashMode)
                        .build();

                // front/back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                syncFlashForCurrentCamera();

                // pinch listener
                ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(requireContext(),
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(@NonNull ScaleGestureDetector detector) {
                            if (camera != null) {
                                float currentZoomRatio = Objects.requireNonNull(camera.getCameraInfo().getZoomState().getValue()).getZoomRatio();
                                float delta = detector.getScaleFactor();
                                camera.getCameraControl().setZoomRatio(currentZoomRatio * delta);
                            }
                            return true;
                        }
                    });

                // zooming
                binding.viewFinder.setOnTouchListener((view, event) -> {
                    scaleGestureDetector.onTouchEvent(event);
                    return true;
                });
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        // Sound effect
        MediaPlayer mediaPlayer = MediaPlayer.create(requireContext(), R.raw.camera_shutter_sound);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        }

        if (imageCapture == null) return;

        applyFlashMode();

        File photoFile = new File(requireContext().getCacheDir(), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Toast.makeText(requireContext(), "Error capture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        if (getView() == null) return;

                        Bundle bundle = new Bundle();
                        bundle.putString("PHOTO_PATH", photoFile.getAbsolutePath());

                        boolean hasGroup = preferences.getBoolean("HAS_GROUP", false);
                        if (hasGroup) {
                            Navigation.findNavController(requireView())
                                    .navigate(R.id.action_cameraFragment_to_imageEditorFragment, bundle);
                        }
                        else {
                            Snackbar.make(binding.layoutControls, "Hãy tạo nhóm để tiếp tục.", Snackbar.LENGTH_SHORT)
                                    .setAction("Tới cài đặt", v -> {
                                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                                                .navigate(R.id.action_homeFragment_to_settingFragment);
                                    })
                                    .show();
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @VisibleForTesting
    public int getFlashMode() {
        return flashMode;
    }

    @VisibleForTesting
    public Camera getCamera() {
        return camera;
    }

    private void applyFlashMode() {
        if (imageCapture != null) {
            imageCapture.setFlashMode(flashMode);
        }
    }

    private void syncFlashForCurrentCamera() {
        if (!hasFlashUnit()) {
            flashMode = ImageCapture.FLASH_MODE_OFF;
        }

        applyFlashMode();
        updateFlashButtonState();
    }

    private boolean hasFlashUnit() {
        return camera != null && camera.getCameraInfo().hasFlashUnit();
    }

    private void updateFlashButtonState() {
        if (binding == null) {
            return;
        }

        boolean flashAvailable = hasFlashUnit();
        binding.btnFlash.setEnabled(flashAvailable);
        binding.btnFlash.setAlpha(flashAvailable ? 1f : 0.5f);
        binding.btnFlash.setColorFilter(
                flashMode == ImageCapture.FLASH_MODE_ON ? Color.parseColor("#FFC107") : Color.WHITE
        );
    }
}
