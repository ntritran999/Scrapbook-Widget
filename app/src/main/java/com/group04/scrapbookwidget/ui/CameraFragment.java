package com.group04.scrapbookwidget.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.group04.scrapbookwidget.databinding.FragmentCameraBinding;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class CameraFragment extends Fragment {

    private FragmentCameraBinding binding;

    private ImageCapture imageCapture;

    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

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

        // flip button
        binding.btnFlipCamera.setOnClickListener(view -> {
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera();
        });

        // shutter button
        binding.btnShutter.setOnClickListener(view -> {
            takePhoto();
        });

        return binding.getRoot();
    }

    // camera setup
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get(); // return the real object, not promise

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
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // create temporary file to cache photo

        java.io.File photoFile = new java.io.File(
            requireContext().getCacheDir(),
            System.currentTimeMillis() + ".jpg"
        );

        // photo's output setup
        ImageCapture.OutputFileOptions outputOptions =
            new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // take photo
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        String msg = "Error capture image: " + e.getMessage();
                        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Saved at: " + photoFile.getAbsolutePath();
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

                        Bundle bundle = new Bundle();
                        bundle.putString("PHOTO_PATH", photoFile.getAbsolutePath());

                        ImageEditorFragment editorFragment = new ImageEditorFragment();
                        editorFragment.setArguments(bundle);

                        requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(((ViewGroup) getView().getParent()).getId(), editorFragment)
                            .addToBackStack(null)
                            .commit();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}