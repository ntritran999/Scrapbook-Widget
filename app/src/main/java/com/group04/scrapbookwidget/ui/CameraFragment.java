package com.group04.scrapbookwidget.ui;

import android.Manifest;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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

    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

    private int flashMode = ImageCapture.FLASH_MODE_OFF;

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
            if (flashMode == ImageCapture.FLASH_MODE_OFF) {
                binding.btnFlash.setColorFilter(Color.parseColor("#FFC107"));
                flashMode = ImageCapture.FLASH_MODE_ON;
            } else {
                binding.btnFlash.setColorFilter(Color.WHITE);
                flashMode = ImageCapture.FLASH_MODE_OFF;
            }
        });

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
                imageCapture = new ImageCapture.Builder().build();

                // front/back camera
                CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build();

                cameraProvider.unbindAll(); // avoid memory leak
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

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

                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_cameraFragment_to_imageEditorFragment, bundle);
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
}
