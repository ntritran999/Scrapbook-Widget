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
    
    private FaceEmbeddingManager faceEmbeddingManager;
    private OnEnrollmentCompleteListener enrollmentListener;
    
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
        
        // Initialize views
        takeSelfieButton = view.findViewById(R.id.btn_take_selfie);
        skipButton = view.findViewById(R.id.btn_skip);
        progressBar = view.findViewById(R.id.progress_enrollment);
        descriptionText = view.findViewById(R.id.text_description);
        
        // Initialize FaceEmbeddingManager
        faceEmbeddingManager = new FaceEmbeddingManager(requireContext());
        faceEmbeddingManager.initialize();
        
        // Set up button listeners
        takeSelfieButton.setOnClickListener(v -> launchCameraForSelfie());
        skipButton.setOnClickListener(v -> {
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCameraIntent();
        }
    }
    
    /**
     * Start camera intent to capture selfie.
     */
    private void startCameraIntent() {
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
                    startCameraIntent();
                } else {
                    showError("Camera permission denied");
                }
            }
        );
        
        // Initialize camera result launcher
        cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getExtras() != null) {
                        Bitmap portrait = (Bitmap) data.getExtras().get("data");
                        processSelfieImage(portrait);
                    }
                }
            }
        );
    }
    
    /**
     * Process the captured selfie image for face enrollment.
     */
    private void processSelfieImage(@Nullable Bitmap portrait) {
        if (portrait == null) {
            showError("Failed to capture image");
            return;
        }
        
        // Show progress
        setUIBusy(true);
        
        // Call FaceEmbeddingManager to extract and save embedding
        faceEmbeddingManager.enrollUserFace(portrait, getCurrentUserId(),
            new FaceEmbeddingManager.FaceEmbeddingCallback() {
                @Override
                public void onEnrollmentSuccess(List<Double> embedding) {
                    setUIBusy(false);
                    saveEmbeddingToFirestore(embedding);
                }
                
                @Override
                public void onEnrollmentError(String error) {
                    setUIBusy(false);
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
            showError("User ID not available");
            if (enrollmentListener != null) {
                enrollmentListener.onEnrollmentFailed("User not authenticated");
            }
            return;
        }
        
        setUIBusy(true);
        Log.d(TAG, "Sending face embedding to server for user: " + userId);
        
        // Send embedding to server via dedicated endpoint
        userRepository.saveFaceEmbedding(userId, faceEmbedding,
            new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    setUIBusy(false);
                    Log.d(TAG, "Face embedding saved to server successfully");
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
                    Log.e(TAG, "Failed to save embedding to server", error);
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
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (faceEmbeddingManager != null) {
            faceEmbeddingManager.release();
        }
    }
}
