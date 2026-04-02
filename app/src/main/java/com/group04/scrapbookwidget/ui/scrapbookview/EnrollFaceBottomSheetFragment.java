package com.group04.scrapbookwidget.ui.scrapbookview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;
import com.group04.scrapbookwidget.data.repository.UserRepository;
import com.group04.scrapbookwidget.ml.FaceEmbeddingManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * EnrollFaceBottomSheetFragment prompts user to provide a selfie for face enrollment.
 * Only shown if user doesn't already have a faceVector in Firestore.
 * Handles camera permission and integrates with FaceEmbeddingManager for ML processing.
 * When user successfully captures a selfie, the extracted face embedding is sent to the server
 * via UserRepository.saveFaceEmbedding() which calls the dedicated `/users/{userId}/enroll-face` endpoint.
 */
@AndroidEntryPoint
public class EnrollFaceBottomSheetFragment extends BottomSheetDialogFragment {
    
    private static final String TAG = "EnrollFaceBottomSheet";
    
    private Button takeSelfieButton;
    private Button skipButton;
    private ProgressBar progressBar;
    private TextView descriptionText;
    
    private OnEnrollmentCompleteListener enrollmentListener;
    
    @Inject
    FaceEmbeddingManager faceEmbeddingManager;
    
    @Inject
    UserRepository userRepository;
    
    @Inject
    FirebaseAuth firebaseAuth;
    
    /**
     * Callback interface for enrollment completion.
     */
    public interface OnEnrollmentCompleteListener {
        /**
         * Called when user successfully completes enrollment.
         * @param faceEmbedding The extracted face embedding (List<Double>)
         */
        void onEnrollmentComplete(List<Double> faceEmbedding);
        
        /**
         * Called when user skips enrollment.
         */
        void onEnrollmentSkipped();
        
        /**
         * Called when enrollment fails.
         * @param errorMessage The error message
         */
        void onEnrollmentFailed(String errorMessage);
    }
    
    public static EnrollFaceBottomSheetFragment newInstance() {
        return new EnrollFaceBottomSheetFragment();
    }
    
    public void setEnrollmentListener(@NonNull OnEnrollmentCompleteListener listener) {
        this.enrollmentListener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_enroll_face_bottomsheet, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "[ENROLL_UI_START] onViewCreated called");
        
        // Initialize views
        takeSelfieButton = view.findViewById(R.id.btn_take_selfie);
        skipButton = view.findViewById(R.id.btn_skip);
        progressBar = view.findViewById(R.id.progress_enrollment);
        descriptionText = view.findViewById(R.id.text_description);
        
        // FaceEmbeddingManager is now injected by Hilt and already initialized
        // Check if injection was successful
        if (faceEmbeddingManager == null) {
            Log.e(TAG, "[ENROLL_UI_ERROR] FaceEmbeddingManager injection failed");
            showError("Failed to initialize face recognition. Please try again.");
            dismiss();
            return;
        }

        if (!faceEmbeddingManager.initialize()) {
            String initializationError = faceEmbeddingManager.getInitializationError();
            Log.e(TAG, "[ENROLL_UI_ERROR] " + initializationError);
            showError(initializationError);
            setUIBusy(true);
            return;
        }
        
        Log.d(TAG, "[ENROLL_UI_READY] FaceEmbeddingManager injected successfully");
        
        // Set up button listeners
        takeSelfieButton.setOnClickListener(v -> {
            Log.d(TAG, "[CAMERA_BUTTON_CLICKED] User clicked take selfie button");
            launchCameraForSelfie();
        });
        skipButton.setOnClickListener(v -> {
            Log.d(TAG, "[ENROLL_SKIPPED] User clicked skip button");
            if (enrollmentListener != null) {
                enrollmentListener.onEnrollmentSkipped();
            }
            dismiss();
        });
    }
    
    /**
     * Launch camera to capture a selfie.
     * Requests camera permission if needed.
     */
    private void launchCameraForSelfie() {
        // Check camera permission
        int permissionCheck = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "[CAMERA_PERMISSION_DENIED] Requesting camera permission...");
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Log.d(TAG, "[CAMERA_PERMISSION_GRANTED] Starting camera");
            startCameraIntent();
        }
    }
    
    /**
     * Start camera intent to capture selfie.
     */
    private void startCameraIntent() {
        Log.d(TAG, "[CAMERA_INTENT_START] Launching camera intent");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraResultLauncher.launch(takePictureIntent);
    }
    
    /**
     * Register activity result launcher for camera permission request.
     */
    private final ActivityResultContracts.RequestPermission requestPermissionContract =
        new ActivityResultContracts.RequestPermission();
    
    private androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher;
    
    /**
     * Register activity result launcher for camera result.
     */
    private androidx.activity.result.ActivityResultLauncher<Intent> cameraResultLauncher;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "[PERMISSION_RESULT] Camera permission granted");
                    startCameraIntent();
                } else {
                    Log.d(TAG, "[PERMISSION_RESULT] Camera permission denied");
                    showError("Camera permission denied");
                }
            }
        );
        
        // Initialize camera result launcher
        cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int resultCode = result.getResultCode();
                Log.d(TAG, "[CAMERA_RESULT] Result code: " + resultCode);
                
                if (resultCode == getActivity().RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getExtras() != null) {
                        Bitmap portrait = (Bitmap) data.getExtras().get("data");
                        if (portrait != null) {
                            Log.d(TAG, "[CAMERA_RESULT_SUCCESS] Captured selfie: " + portrait.getWidth() + "x" + portrait.getHeight());
                            processSelfieImage(portrait);
                        } else {
                            Log.e(TAG, "[CAMERA_RESULT_ERROR] Portrait bitmap is null");
                            showError("Failed to capture selfie");
                        }
                    } else {
                        Log.e(TAG, "[CAMERA_RESULT_ERROR] No data returned from camera");
                        showError("Failed to capture selfie");
                    }
                } else {
                    Log.d(TAG, "[CAMERA_RESULT_CANCELLED] User cancelled camera");
                }
            }
        );
    }
    
    /**
     * Process the captured selfie image for face enrollment.
     */
    private void processSelfieImage(@Nullable Bitmap portrait) {
        if (portrait == null) {
            Log.e(TAG, "[PROCESS_ERROR] Portrait is null");
            showError("Failed to capture image");
            return;
        }
        
        Log.d(TAG, "[PROCESS_START] Processing selfie: " + portrait.getWidth() + "x" + portrait.getHeight());

        if (!faceEmbeddingManager.initialize()) {
            String initializationError = faceEmbeddingManager.getInitializationError();
            Log.e(TAG, "[PROCESS_INIT_ERROR] " + initializationError);
            showError(initializationError);
            if (enrollmentListener != null) {
                enrollmentListener.onEnrollmentFailed(initializationError);
            }
            return;
        }
        
        // Show progress
        setUIBusy(true);
        
        String userId = getCurrentUserId();
        Log.d(TAG, "[PROCESS_USER] User ID: " + userId);
        
        // Call FaceEmbeddingManager to extract and save embedding
        faceEmbeddingManager.enrollUserFace(portrait, userId,
            new FaceEmbeddingManager.FaceEmbeddingCallback() {
                @Override
                public void onEnrollmentSuccess(List<Double> embedding) {
                    setUIBusy(false);
                    Log.d(TAG, "[PROCESS_ML_SUCCESS] Embedding extracted, size: " + (embedding != null ? embedding.size() : 0));
                    saveEmbeddingToFirestore(embedding);
                }
                
                @Override
                public void onEnrollmentError(String error) {
                    setUIBusy(false);
                    Log.e(TAG, "[PROCESS_ML_ERROR] " + error);
                    showError(error);
                    if (enrollmentListener != null) {
                        enrollmentListener.onEnrollmentFailed(error);
                    }
                }
            }
        );
    }
    
    /**
     * Save the extracted embedding to server via dedicated enrollment endpoint.
     * Calls UserRepository.saveFaceEmbedding() which sends to /users/{userId}/enroll-face
     * and stores the faceVector on the backend.
     */
    private void saveEmbeddingToFirestore(@NonNull List<Double> faceEmbedding) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.e(TAG, "[UPLOAD_ERROR] User ID not available");
            showError("User ID not available");
            if (enrollmentListener != null) {
                enrollmentListener.onEnrollmentFailed("User not authenticated");
            }
            return;
        }
        
        setUIBusy(true);
        Log.d(TAG, "[UPLOAD_START] Sending face embedding to server for user: " + userId + ", embedding size: " + faceEmbedding.size());
        
        // Send embedding to server via dedicated endpoint
        userRepository.saveFaceEmbedding(userId, faceEmbedding,
            new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    setUIBusy(false);
                    Log.d(TAG, "[UPLOAD_SUCCESS] Face embedding saved to server successfully for user: " + userId);
                    Toast.makeText(requireContext(), 
                        "Face enrollment complete!", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Notify listener of success
                    if (enrollmentListener != null) {
                        enrollmentListener.onEnrollmentComplete(faceEmbedding);
                    }
                    
                    dismiss();
                }
                
                @Override
                public void onError(Exception error) {
                    setUIBusy(false);
                    Log.e(TAG, "[UPLOAD_ERROR] Failed to save embedding to server: " + error.getMessage(), error);
                    showError("Failed to save enrollment: " + error.getMessage());
                    if (enrollmentListener != null) {
                        enrollmentListener.onEnrollmentFailed(error.getMessage());
                    }
                }
            });
    }
    
    /**
     * Get current user ID from Firebase Auth.
     */
    @Nullable
    private String getCurrentUserId() {
        return firebaseAuth.getCurrentUser() != null ?
               firebaseAuth.getCurrentUser().getUid() : null;
    }
    
    /**
     * Show or hide progress indicator and enable/disable buttons.
     */
    private void setUIBusy(boolean busy) {
        if (progressBar != null) {
            progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        }
        if (takeSelfieButton != null) {
            takeSelfieButton.setEnabled(!busy);
        }
        if (skipButton != null) {
            skipButton.setEnabled(!busy);
        }
    }
    
    /**
     * Show error message to user.
     */
    private void showError(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
