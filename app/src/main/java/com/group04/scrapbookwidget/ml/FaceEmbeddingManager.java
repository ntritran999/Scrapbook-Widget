package com.group04.scrapbookwidget.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

/**
 * FaceEmbeddingManager handles face detection using ML Kit and face embedding extraction using TFLite MobileFaceNet.
 * All ML operations run on background threads to prevent ANR on the main thread.
 * Converts float[] embeddings to List<Double> for Firestore compatibility.
 */
@Singleton
public class FaceEmbeddingManager {
    
    private static final String TAG = "FaceEmbeddingManager";
    private static final String MODEL_FILE_NAME = "MobileFaceNet.tflite";
    
    // TFLite model configuration
    private static final int INPUT_SIZE = 112; // MobileFaceNet expects 112x112
    private static final int EMBEDDING_SIZE = 192; // MobileFaceNet outputs 192-dim embeddings
    private static final float MEAN_RGB = 128f;
    private static final float STD_RGB = 128f;
    private static final int[] ROTATION_FALLBACKS = new int[]{0, 90, 270, 180};
    
    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    private FaceDetector faceDetector;
    private Interpreter tfliteInterpreter;
    private boolean isInitialized = false;
    @Nullable
    private String initializationError;

    private static final class DetectionResult {
        @NonNull
        final List<Face> faces;
        final int rotation;

        DetectionResult(@NonNull List<Face> faces, int rotation) {
            this.faces = faces;
            this.rotation = rotation;
        }
    }
    
    public interface FaceEmbeddingCallback {
        void onEnrollmentSuccess(List<Double> embedding);
        void onEnrollmentError(String error);
    }
    
    public interface GroupPhotoCallback {
        void onFacesExtracted(List<List<Double>> embeddings);
        void onExtractionError(String error);
    }
    
    public FaceEmbeddingManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.faceDetector = null;
        this.tfliteInterpreter = null;
    }
    
    /**
     * Initialize ML Kit face detector and TFLite interpreter.
     * This must be called before using the manager.
     */
    public synchronized boolean initialize() {
        if (isInitialized) {
            Log.d(TAG, "[INIT_SKIP] FaceEmbeddingManager already initialized");
            return true;
        }
        
        Log.d(TAG, "[INIT_START] Initializing FaceEmbeddingManager...");
        
        try {
            // Initialize ML Kit Face Detector with high accuracy
            Log.d(TAG, "[INIT_FACE_DETECTOR] Setting up ML Kit Face Detector with high accuracy");
            FaceDetectorOptions highAccuracyOpts = 
                new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();
            
            this.faceDetector = FaceDetection.getClient(highAccuracyOpts);
            Log.d(TAG, "[INIT_FACE_DETECTOR_OK] Face Detector initialized successfully");
            
            // Initialize TFLite Interpreter
            Log.d(TAG, "[INIT_TFLITE] Loading TFLite model...");
            initializeTFLite();
            Log.d(TAG, "[INIT_TFLITE_OK] TFLite Interpreter initialized successfully");
            
            initializationError = null;
            isInitialized = true;
            Log.d(TAG, "[INIT_SUCCESS] FaceEmbeddingManager initialized successfully");
            return true;
            
        } catch (Exception e) {
            initializationError = buildInitializationErrorMessage(e);
            Log.e(TAG, "[INIT_ERROR] Failed to initialize FaceEmbeddingManager: " + initializationError, e);
            isInitialized = false;
            return false;
        }
    }
    
    /**
     * Load and initialize the TFLite model for face embedding extraction.
     * Expected model file: MobileFaceNet.tflite in assets folder.
     */
    private void initializeTFLite() throws IOException {
        try {
            // Load model from assets
            ByteBuffer modelBuffer = loadModelFile(MODEL_FILE_NAME);
            this.tfliteInterpreter = new Interpreter(modelBuffer);
            Log.d(TAG, "TFLite Interpreter initialized");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load TFLite model", e);
            throw e;
        }
    }
    
    /**
     * Load model file from assets into a ByteBuffer.
     */
    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            return fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getDeclaredLength()
            );
        }
    }
    
    /**
     * Enroll user face by extracting embedding from a portrait Bitmap.
     * Runs on background thread. If exactly 1 face is found, extracts embedding.
     * Handles edge cases: null bitmap, no faces, multiple faces.
     */
    public void enrollUserFace(@Nullable Bitmap portrait, @NonNull String userId,
                               @NonNull FaceEmbeddingCallback callback) {
        
        Log.d(TAG, "[ENROLL_START] User: " + userId + ", Portrait size: " + (portrait != null ? portrait.getWidth() + "x" + portrait.getHeight() : "null"));
        
        if (!initialize()) {
            String errorMessage = getInitializationError();
            Log.e(TAG, "[ENROLL_ERROR] " + errorMessage);
            mainHandler.post(() -> callback.onEnrollmentError(errorMessage));
            return;
        }
        
        if (portrait == null) {
            Log.e(TAG, "[ENROLL_ERROR] Portrait bitmap is null");
            mainHandler.post(() -> callback.onEnrollmentError("Portrait bitmap is null"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                long detectionStartTime = System.currentTimeMillis();
                Log.d(TAG, "[DETECT_START] Starting face detection...");

                DetectionResult detectionResult = detectFacesWithRotationFallback(portrait);
                List<Face> detectedFaces = detectionResult.faces;
                long detectionDuration = System.currentTimeMillis() - detectionStartTime;
                Log.d(TAG, "[DETECT_COMPLETE] Detection took " + detectionDuration + "ms, rotation=" + detectionResult.rotation);

                // Validate exactly 1 face is detected
                if (detectedFaces == null || detectedFaces.isEmpty()) {
                    Log.e(TAG, "[DETECT_ERROR] No face detected in the photo");
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("No face detected in the photo")
                    );
                    return;
                }
                
                Log.d(TAG, "[DETECT_SUCCESS] Found " + detectedFaces.size() + " face(s)");
                
                if (detectedFaces.size() > 1) {
                    Log.e(TAG, "[DETECT_ERROR] Multiple faces detected: " + detectedFaces.size());
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("Multiple faces detected. Please provide a clear single selfie")
                    );
                    return;
                }
                
                // Crop the detected face
                Face detectedFace = detectedFaces.get(0);
                Log.d(TAG, "[CROP_START] Cropping detected face...");
                Bitmap portraitForExtraction = detectionResult.rotation == 0
                        ? portrait
                        : rotateBitmap(portrait, detectionResult.rotation);
                Bitmap croppedFace = cropFace(portraitForExtraction, detectedFace);
                
                if (croppedFace == null) {
                    if (portraitForExtraction != portrait) {
                        portraitForExtraction.recycle();
                    }
                    Log.e(TAG, "[CROP_ERROR] Failed to crop face from image");
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("Failed to crop face from image")
                    );
                    return;
                }
                
                Log.d(TAG, "[CROP_SUCCESS] Face cropped successfully: " + croppedFace.getWidth() + "x" + croppedFace.getHeight());
                
                // Extract embedding from cropped face
                Log.d(TAG, "[EMBEDDING_START] Extracting face embedding...");
                long embeddingStartTime = System.currentTimeMillis();
                float[] embedding = extractEmbedding(croppedFace);
                if (portraitForExtraction != portrait) {
                    portraitForExtraction.recycle();
                }
                long embeddingDuration = System.currentTimeMillis() - embeddingStartTime;
                croppedFace.recycle(); // Clean up
                Log.d(TAG, "[EMBEDDING_COMPLETE] Embedding extraction took " + embeddingDuration + "ms");
                
                if (!isEmbeddingValid(embedding)) {
                    Log.e(TAG, "[EMBEDDING_ERROR] Failed to extract face embedding");
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("Failed to extract face embedding")
                    );
                    return;
                }
                
                Log.d(TAG, "[EMBEDDING_SUCCESS] Extracted embedding with dimension: " + embedding.length);
                
                // Convert float[] to List<Double> for Firestore compatibility
                Log.d(TAG, "[CONVERT_START] Converting embedding to List<Double>...");
                List<Double> embeddingList = floatArrayToDoubleList(embedding);
                Log.d(TAG, "[CONVERT_SUCCESS] Converted to List<Double> size: " + embeddingList.size());
                
                Log.d(TAG, "[ENROLL_SUCCESS] Face enrollment successful for user: " + userId);
                mainHandler.post(() -> 
                    callback.onEnrollmentSuccess(embeddingList)
                );
                
            } catch (Exception e) {
                Log.e(TAG, "[ENROLL_EXCEPTION] Error during face enrollment: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    String errorMsg = "Enrollment failed: " + e.getMessage();
                    Log.e(TAG, "[ENROLL_CALLBACK_ERROR] " + errorMsg);
                    callback.onEnrollmentError(errorMsg);
                });
            }
        });
    }
    
    /**
     * Extract face embeddings from a group photo.
     * Detects ALL faces and returns a List of embeddings (each as List<Double>).
     */
    public void extractFacesFromPhoto(@NonNull Bitmap groupPhoto,
                                       @NonNull GroupPhotoCallback callback) {
        
        if (!initialize()) {
            String errorMessage = getInitializationError();
            mainHandler.post(() -> callback.onExtractionError(errorMessage));
            return;
        }
        
        if (groupPhoto == null) {
            mainHandler.post(() -> callback.onExtractionError("Group photo bitmap is null"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                DetectionResult detectionResult = detectFacesWithRotationFallback(groupPhoto);
                List<Face> detectedFaces = detectionResult.faces;

                if (detectedFaces == null || detectedFaces.isEmpty()) {
                    mainHandler.post(() -> 
                        callback.onFacesExtracted(new ArrayList<>()) // Return empty list
                    );
                    return;
                }
                
                // Extract embedding for each detected face
                List<List<Double>> allEmbeddings = new ArrayList<>();
                Bitmap extractionBitmap = detectionResult.rotation == 0
                        ? groupPhoto
                        : rotateBitmap(groupPhoto, detectionResult.rotation);
                int skippedFaces = 0;
                
                for (Face face : detectedFaces) {
                    try {
                        Bitmap croppedFace = cropFace(extractionBitmap, face);
                        
                        if (croppedFace != null) {
                            float[] embedding = extractEmbedding(croppedFace);
                            croppedFace.recycle();
                            
                            if (isEmbeddingValid(embedding)) {
                                allEmbeddings.add(floatArrayToDoubleList(embedding));
                            } else {
                                skippedFaces++;
                                Log.w(TAG, "[GROUP_EMBEDDING_SKIP] Invalid embedding for one detected face");
                            }
                        } else {
                            skippedFaces++;
                            Log.w(TAG, "[GROUP_CROP_SKIP] Failed to crop one detected face");
                        }
                    } catch (Exception e) {
                        skippedFaces++;
                        Log.w(TAG, "Failed to extract embedding for one face, continuing", e);
                    }
                }

                if (extractionBitmap != groupPhoto) {
                    extractionBitmap.recycle();
                }

                if (allEmbeddings.isEmpty() && !detectedFaces.isEmpty()) {
                    String errorMessage = "Faces were detected, but embedding extraction failed for all of them.";
                    Log.e(TAG, "[GROUP_EMBEDDING_ERROR] " + errorMessage + " skippedFaces=" + skippedFaces);
                    mainHandler.post(() -> callback.onExtractionError(errorMessage));
                    return;
                }
                
                mainHandler.post(() -> 
                    callback.onFacesExtracted(allEmbeddings)
                );
                
            } catch (Exception e) {
                Log.e(TAG, "Error extracting faces from group photo", e);
                mainHandler.post(() -> 
                    callback.onExtractionError("Extraction failed: " + e.getMessage())
                );
            }
        });
    }

    @NonNull
    private DetectionResult detectFacesWithRotationFallback(@NonNull Bitmap sourceBitmap) throws Exception {
        for (int rotation : ROTATION_FALLBACKS) {
            Bitmap workingBitmap = rotation == 0 ? sourceBitmap : rotateBitmap(sourceBitmap, rotation);
            try {
                InputImage inputImage = InputImage.fromBitmap(workingBitmap, 0);
                Task<List<Face>> detectionTask = faceDetector.process(inputImage);
                List<Face> detectedFaces = Tasks.await(detectionTask);
                if (detectedFaces != null && !detectedFaces.isEmpty()) {
                    Log.d(TAG, "[DETECT_ROTATION_SUCCESS] rotation=" + rotation + ", faces=" + detectedFaces.size());
                    return new DetectionResult(detectedFaces, rotation);
                }
                Log.d(TAG, "[DETECT_ROTATION_EMPTY] rotation=" + rotation);
            } finally {
                if (workingBitmap != sourceBitmap) {
                    workingBitmap.recycle();
                }
            }
        }
        return new DetectionResult(new ArrayList<>(), 0);
    }
    
    /**
     * Crop face from bitmap using ML Kit Face object.
     * Handles boundary edge cases to prevent IllegalArgumentException.
     */
    @Nullable
    private Bitmap cropFace(@NonNull Bitmap sourceBitmap, @NonNull Face face) {
        try {
            Rect boundingBox = face.getBoundingBox();

            // Add margin to crop box (10% padding)
            float marginX = (boundingBox.right - boundingBox.left) * 0.1f;
            float marginY = (boundingBox.bottom - boundingBox.top) * 0.1f;

            int left = Math.max(0, (int) (boundingBox.left - marginX));
            int top = Math.max(0, (int) (boundingBox.top - marginY));
            int width = Math.min(sourceBitmap.getWidth() - left,
                    (int) (boundingBox.width() + 2 * marginX));
            int height = Math.min(sourceBitmap.getHeight() - top,
                    (int) (boundingBox.height() + 2 * marginY));

            // Validate crop dimensions
            if (width <= 0 || height <= 0 || left < 0 || top < 0 ||
                    (left + width) > sourceBitmap.getWidth() ||
                    (top + height) > sourceBitmap.getHeight()) {
                Log.w(TAG, "Invalid crop dimensions, using tight bounding box");

                left = Math.max(0, boundingBox.left);
                top = Math.max(0, boundingBox.top);
                width = Math.min(sourceBitmap.getWidth() - left, boundingBox.width());
                height = Math.min(sourceBitmap.getHeight() - top, boundingBox.height());
            }

            if (width <= 0 || height <= 0) {
                Log.e(TAG, "Failed to create valid crop region");
                return null;
            }

            return Bitmap.createBitmap(sourceBitmap, left, top, width, height);

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to crop face from bitmap", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while cropping face", e);
            return null;
        }
    }

    /**
     * Extract face embedding from a cropped face bitmap using TFLite.
     * Preprocesses bitmap: resize to 112x112, normalize, convert to ByteBuffer.
     */
    @Nullable
    private float[] extractEmbedding(@NonNull Bitmap faceBitmap) {
        try {
            if (tfliteInterpreter == null) {
                Log.e(TAG, "[EMBEDDING_ERROR] TFLite interpreter is null");
                return null;
            }

            Bitmap inputBitmap = faceBitmap.getConfig() == Bitmap.Config.ARGB_8888
                    ? faceBitmap
                    : faceBitmap.copy(Bitmap.Config.ARGB_8888, false);

            // Preprocess: resize and normalize
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, INPUT_SIZE, INPUT_SIZE, true);
            if (inputBitmap != faceBitmap) {
                inputBitmap.recycle();
            }
            
            // Create ByteBuffer for TFLite input (FLOAT32 format)
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(
                4 * INPUT_SIZE * INPUT_SIZE * 3 // 4 bytes for float, 3 channels (RGB)
            );
            inputBuffer.order(ByteOrder.nativeOrder());
            
            // Extract RGB pixels and normalize
            int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
            resizedBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
            resizedBitmap.recycle();
            
            for (int pixel : pixels) {
                // Extract RGB channels
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                // Normalize: (pixel - 128) / 128
                inputBuffer.putFloat((r - MEAN_RGB) / STD_RGB);
                inputBuffer.putFloat((g - MEAN_RGB) / STD_RGB);
                inputBuffer.putFloat((b - MEAN_RGB) / STD_RGB);
            }
            
            inputBuffer.rewind();
            
            // Create output buffer for embeddings
            float[][] outputEmbedding = new float[1][EMBEDDING_SIZE];
            
            // Run TFLite inference
            tfliteInterpreter.run(inputBuffer, outputEmbedding);
            
            float[] normalizedEmbedding = l2Normalize(outputEmbedding[0]);
            return isEmbeddingValid(normalizedEmbedding) ? normalizedEmbedding : null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting embedding from face bitmap", e);
            return null;
        }
    }
    
    /**
     * Convert float[] to List<Double> for Firestore compatibility.
     */
    @NonNull
    private List<Double> floatArrayToDoubleList(@NonNull float[] floatArray) {
        List<Double> doubleList = new ArrayList<>(floatArray.length);
        for (float value : floatArray) {
            doubleList.add((double) value);
        }
        return doubleList;
    }
    
    /**
     * Release resources and close TFLite interpreter.
     * Call this when the manager is no longer needed (e.g., in onDestroy).
     */
    public void release() {
        try {
            if (tfliteInterpreter != null) {
                tfliteInterpreter.close();
                tfliteInterpreter = null;
            }
            if (faceDetector != null) {
                faceDetector.close();
                faceDetector = null;
            }
            if (executorService != null) {
                executorService.shutdown();
            }
            isInitialized = false;
            initializationError = null;
            Log.d(TAG, "FaceEmbeddingManager released");
        } catch (Exception e) {
            Log.e(TAG, "Error releasing FaceEmbeddingManager", e);
        }
    }
    
    /**
     * Check if manager is initialized.
     */
    public boolean isReady() {
        return isInitialized;
    }

    private boolean isEmbeddingValid(@Nullable float[] embedding) {
        if (embedding == null || embedding.length != EMBEDDING_SIZE) {
            return false;
        }

        boolean hasNonZeroValue = false;
        for (float value : embedding) {
            if (!Float.isFinite(value)) {
                return false;
            }
            if (Math.abs(value) > 1e-6f) {
                hasNonZeroValue = true;
            }
        }
        return hasNonZeroValue;
    }

    @NonNull
    private float[] l2Normalize(@NonNull float[] embedding) {
        float magnitude = 0f;
        for (float value : embedding) {
            magnitude += value * value;
        }
        magnitude = (float) Math.sqrt(magnitude);

        if (magnitude <= 1e-12f) {
            return embedding;
        }

        float[] normalized = new float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            normalized[i] = embedding[i] / magnitude;
        }
        return normalized;
    }

    @NonNull
    private Bitmap rotateBitmap(@NonNull Bitmap sourceBitmap, int degrees) {
        if (degrees == 0) {
            return sourceBitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(
                sourceBitmap,
                0,
                0,
                sourceBitmap.getWidth(),
                sourceBitmap.getHeight(),
                matrix,
                true
        );
    }

    @NonNull
    public String getInitializationError() {
        return initializationError != null
                ? initializationError
                : "Face embedding is unavailable right now.";
    }

    @NonNull
    private String buildInitializationErrorMessage(@NonNull Exception exception) {
        if (exception instanceof IOException) {
            return "Face embedding model not found. Add " + MODEL_FILE_NAME + " to app/src/main/assets.";
        }

        String detail = exception.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            return "Failed to initialize face embedding.";
        }
        return "Failed to initialize face embedding: " + detail;
    }
}
