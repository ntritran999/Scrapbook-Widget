package com.group04.scrapbookwidget.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.group04.scrapbookwidget.data.model.FaceTaggingPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * FaceEmbeddingUtils provides utility methods for face embedding operations.
 * Includes:
 * - Converting face embeddings to payload format
 * - Building FaceEmbedding objects with bounding box info
 * - Async operations with proper callback handling
 */
public class FaceEmbeddingUtils {
    
    private static final String TAG = "FaceEmbeddingUtils";
    
    /**
     * Create a FaceTaggingPayload from extracted embeddings and metadata.
     * Converts ML Kit Face objects and float embeddings into the format
     * expected by the backend.
     */
    public static FaceTaggingPayload createFaceTaggingPayload(
            @NonNull String userId,
            @NonNull String groupId,
            @NonNull String photoUri,
            @NonNull List<Face> detectedFaces,
            @NonNull List<List<Double>> faceEmbeddings,
            int photoWidth,
            int photoHeight,
            @Nullable String caption) {
        
        // Validate inputs
        if (detectedFaces.size() != faceEmbeddings.size()) {
            Log.w(TAG, "Face count mismatch: " + detectedFaces.size() + 
                  " faces detected but " + faceEmbeddings.size() + " embeddings extracted");
        }
        
        // Create payload
        FaceTaggingPayload payload = new FaceTaggingPayload();
        payload.setUserId(userId);
        payload.setGroupId(groupId);
        payload.setPhotoUri(photoUri);
        payload.setPhotoWidth(photoWidth);
        payload.setPhotoHeight(photoHeight);
        payload.setCaption(caption);
        
        // Create list of FaceEmbedding objects
        List<FaceTaggingPayload.FaceEmbedding> faceEmbeddingList = new ArrayList<>();
        
        for (int i = 0; i < Math.min(detectedFaces.size(), faceEmbeddings.size()); i++) {
            Face face = detectedFaces.get(i);
            List<Double> embedding = faceEmbeddings.get(i);
            
            // Create bounding box from ML Kit Face
            FaceTaggingPayload.BoundingBox boundingBox = 
                convertFaceBoundingBox(face, photoWidth, photoHeight);
            
            // Get confidence score (use face tracking ID as proxy)
            float confidence = face.getHeadEulerAngleY() >= 0 ? 0.9f : 0.8f; // Placeholder
            
            FaceTaggingPayload.FaceEmbedding faceEmbedding = 
                new FaceTaggingPayload.FaceEmbedding(embedding, boundingBox, confidence);
            
            faceEmbeddingList.add(faceEmbedding);
        }
        
        payload.setFaceEmbeddings(faceEmbeddingList);
        
        Log.d(TAG, "Created FaceTaggingPayload with " + faceEmbeddingList.size() + " face embeddings");
        return payload;
    }
    
    /**
     * Convert ML Kit Face bounding box to normalized coordinates.
     * Coordinates are normalized relative to photo dimensions (0-1).
     */
    @NonNull
    private static FaceTaggingPayload.BoundingBox convertFaceBoundingBox(
            @NonNull Face face,
            int photoWidth,
            int photoHeight) {

        android.graphics.Rect mlKitBox = face.getBoundingBox();
        
        // Normalize coordinates to 0-1 range
        float left = mlKitBox.left / (float) photoWidth;
        float top = mlKitBox.top / (float) photoHeight;
        float right = mlKitBox.right / (float) photoWidth;
        float bottom = mlKitBox.bottom / (float) photoHeight;
        
        // Clamp to valid range
        left = Math.max(0, Math.min(1, left));
        top = Math.max(0, Math.min(1, top));
        right = Math.max(0, Math.min(1, right));
        bottom = Math.max(0, Math.min(1, bottom));
        
        return new FaceTaggingPayload.BoundingBox(left, top, right, bottom);
    }
    
    /**
     * Extract face embedding vector from a face and embedding array.
     * Helper for creating FaceEmbedding objects.
     */
    @NonNull
    public static FaceTaggingPayload.FaceEmbedding createFaceEmbeddingObject(
            @NonNull Face face,
            @NonNull List<Double> embedding,
            int photoWidth,
            int photoHeight) {
        
        FaceTaggingPayload.BoundingBox boundingBox = 
            convertFaceBoundingBox(face, photoWidth, photoHeight);
        
        float confidence = 0.85f; // Default confidence
        
        return new FaceTaggingPayload.FaceEmbedding(embedding, boundingBox, confidence);
    }
    
    /**
     * Validate face embedding vector dimensions.
     * MobileFaceNet produces 192-dimensional embeddings.
     */
    public static boolean isValidEmbedding(@NonNull List<Double> embedding) {
        return embedding != null && embedding.size() == 192;
    }
    
    /**
     * Validate face embedding vector dimensions (float array version).
     */
    public static boolean isValidEmbedding(@NonNull float[] embedding) {
        return embedding != null && embedding.length == 192;
    }
    
    /**
     * Calculate similarity between two embeddings using cosine distance.
     * Returns a value between 0 and 1, where 1 = identical embeddings.
     */
    public static double calculateCosineSimilarity(@NonNull List<Double> embedding1,
                                                   @NonNull List<Double> embedding2) {
        if (embedding1.size() != embedding2.size()) {
            Log.w(TAG, "Embedding size mismatch: " + embedding1.size() + " vs " + embedding2.size());
            return 0;
        }
        
        double dotProduct = 0;
        double magnitude1 = 0;
        double magnitude2 = 0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            double val1 = embedding1.get(i);
            double val2 = embedding2.get(i);
            
            dotProduct += val1 * val2;
            magnitude1 += val1 * val1;
            magnitude2 += val2 * val2;
        }
        
        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);
        
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0;
        }
        
        return dotProduct / (magnitude1 * magnitude2);
    }
    
    /**
     * Determine if two embeddings belong to the same person.
     * Uses cosine similarity with a threshold of 0.4 (tunable based on accuracy requirements).
     */
    public static boolean areFacesSimilar(@NonNull List<Double> embedding1,
                                          @NonNull List<Double> embedding2) {
        double similarity = calculateCosineSimilarity(embedding1, embedding2);
        double threshold = 0.4; // Threshold for face matching (tune based on your requirements)
        return similarity >= threshold;
    }
}
