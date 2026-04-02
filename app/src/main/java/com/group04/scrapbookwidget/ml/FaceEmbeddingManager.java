package com.group04.scrapbookwidget.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    // TFLite model configuration
    private static final int INPUT_SIZE = 112; // MobileFaceNet expects 112x112
    private static final int EMBEDDING_SIZE = 192; // MobileFaceNet outputs 192-dim embeddings
    private static final float MEAN_RGB = 128f;
    private static final float STD_RGB = 128f;
    
    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    private FaceDetector faceDetector;
    private Interpreter tfliteInterpreter;
    private boolean isInitialized = false;
    
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
    public void initialize() {
        if (isInitialized) {
            return;
        }
        
        try {
            // Initialize ML Kit Face Detector with high accuracy
            FaceDetectorOptions highAccuracyOpts = 
                new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();
            
            this.faceDetector = FaceDetection.getClient(highAccuracyOpts);
            
            // Initialize TFLite Interpreter
            initializeTFLite();
            
            isInitialized = true;
            Log.d(TAG, "FaceEmbeddingManager initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize FaceEmbeddingManager", e);
            isInitialized = false;
        }
    }
    
    /**
     * Load and initialize the TFLite model for face embedding extraction.
     * Expected model file: MobileFaceNet.tflite in assets folder.
     */
    private void initializeTFLite() throws IOException {
        try {
            // Load model from assets
            ByteBuffer modelBuffer = loadModelFile("MobileFaceNet.tflite");
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
    private ByteBuffer loadModelFile(String modelName) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(
                context.getAssets().open(modelName))) {
            
            byte[] modelData = new byte[bis.available()];
            bis.read(modelData);
            
            ByteBuffer buffer = ByteBuffer.allocateDirect(modelData.length);
            buffer.put(modelData);
            buffer.rewind();
            return buffer;
        }
    }
    
    /**
     * Enroll user face by extracting embedding from a portrait Bitmap.
     * Runs on background thread. If exactly 1 face is found, extracts embedding.
     * Handles edge cases: null bitmap, no faces, multiple faces.
     */
    public void enrollUserFace(@Nullable Bitmap portrait, @NonNull String userId,
                               @NonNull FaceEmbeddingCallback callback) {
        
        if (!isInitialized) {
            mainHandler.post(() -> callback.onEnrollmentError("FaceEmbeddingManager not initialized"));
            return;
        }
        
        if (portrait == null) {
            mainHandler.post(() -> callback.onEnrollmentError("Portrait bitmap is null"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Detect faces in portrait
                InputImage inputImage = InputImage.fromBitmap(portrait, 0);
                Task<List<Face>> detectionTask = faceDetector.process(inputImage);
                
                // Block on Tasks.await() inside background thread to get detection results
                List<Face> detectedFaces = Tasks.await(detectionTask);

                // Validate exactly 1 face is detected
                if (detectedFaces == null || detectedFaces.isEmpty()) {
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("No face detected in the photo")
                    );
                    return;
                }
                
                if (detectedFaces.size() > 1) {
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("Multiple faces detected. Please provide a clear single selfie")
                    );
                    return;
                }
                
                // Crop the detected face
                Face detectedFace = detectedFaces.get(0);
                Bitmap croppedFace = cropFace(portrait, detectedFace);
                
                if (croppedFace == null) {
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("Failed to crop face from image")
                    );
                    return;
                }
                
                // Extract embedding from cropped face
                float[] embedding = extractEmbedding(croppedFace);
                croppedFace.recycle(); // Clean up
                
                if (embedding == null || embedding.length == 0) {
                    mainHandler.post(() -> 
                        callback.onEnrollmentError("Failed to extract face embedding")
                    );
                    return;
                }
                
                // Convert float[] to List<Double> for Firestore compatibility
                List<Double> embeddingList = floatArrayToDoubleList(embedding);
                
                mainHandler.post(() -> 
                    callback.onEnrollmentSuccess(embeddingList)
                );
                
            } catch (Exception e) {
                Log.e(TAG, "Error during face enrollment", e);
                mainHandler.post(() -> 
                    callback.onEnrollmentError("Enrollment failed: " + e.getMessage())
                );
            }
        });
    }
    
    /**
     * Extract face embeddings from a group photo.
     * Detects ALL faces and returns a List of embeddings (each as List<Double>).
     */
    public void extractFacesFromPhoto(@NonNull Bitmap groupPhoto,
                                       @NonNull GroupPhotoCallback callback) {
        
        if (!isInitialized) {
            mainHandler.post(() -> callback.onExtractionError("FaceEmbeddingManager not initialized"));
            return;
        }
        
        if (groupPhoto == null) {
            mainHandler.post(() -> callback.onExtractionError("Group photo bitmap is null"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Detect all faces in group photo
                InputImage inputImage = InputImage.fromBitmap(groupPhoto, 0);
                Task<List<Face>> detectionTask = faceDetector.process(inputImage);
                
                // Block on Tasks.await() inside background thread
                List<Face> detectedFaces = Tasks.await(detectionTask);

                if (detectedFaces == null || detectedFaces.isEmpty()) {
                    mainHandler.post(() -> 
                        callback.onFacesExtracted(new ArrayList<>()) // Return empty list
                    );
                    return;
                }
                
                // Extract embedding for each detected face
                List<List<Double>> allEmbeddings = new ArrayList<>();
                
                for (Face face : detectedFaces) {
                    try {
                        Bitmap croppedFace = cropFace(groupPhoto, face);
                        
                        if (croppedFace != null) {
                            float[] embedding = extractEmbedding(croppedFace);
                            croppedFace.recycle();
                            
                            if (embedding != null && embedding.length > 0) {
                                allEmbeddings.add(floatArrayToDoubleList(embedding));
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to extract embedding for one face, continuing", e);
                        // Continue with next face
                    }
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
            // Preprocess: resize and normalize
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true);
            
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
            
            return outputEmbedding[0];
            
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
}
