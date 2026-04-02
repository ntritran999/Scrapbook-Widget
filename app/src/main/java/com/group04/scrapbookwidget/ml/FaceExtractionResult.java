package com.group04.scrapbookwidget.ml;

import android.graphics.RectF;
import androidx.annotation.NonNull;
import com.google.mlkit.vision.face.Face;
import java.util.ArrayList;
import java.util.List;

/**
 * Container class for face extraction results.
 * Bundles together the raw Face objects from ML Kit and their extracted embeddings.
 * This allows the caller to build complete payloads with both embeddings and bounding boxes.
 */
public class FaceExtractionResult {
    
    /**
     * Raw Face objects from ML Kit containing bounding boxes, landmarks, etc.
     */
    private final List<Face> detectedFaces;
    
    /**
     * Extracted embedding vectors corresponding to each detected face.
     * Each embedding is a List<Double> of size 192 (MobileFaceNet output).
     */
    private final List<List<Double>> embeddings;
    
    /**
     * Success status of the extraction.
     */
    private final boolean success;
    
    /**
     * Error message if extraction failed.
     */
    private final String errorMessage;
    
    /**
     * Create a successful extraction result.
     */
    public FaceExtractionResult(@NonNull List<Face> detectedFaces,
                               @NonNull List<List<Double>> embeddings) {
        this.detectedFaces = detectedFaces;
        this.embeddings = embeddings;
        this.success = true;
        this.errorMessage = null;
    }
    
    /**
     * Create a failed extraction result.
     */
    public FaceExtractionResult(@NonNull String errorMessage) {
        this.detectedFaces = new ArrayList<>();
        this.embeddings = new ArrayList<>();
        this.success = false;
        this.errorMessage = errorMessage;
    }
    
    // Getters
    
    @NonNull
    public List<Face> getDetectedFaces() {
        return detectedFaces;
    }
    
    @NonNull
    public List<List<Double>> getEmbeddings() {
        return embeddings;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Get count of successfully extracted faces.
     */
    public int getFaceCount() {
        return Math.min(detectedFaces.size(), embeddings.size());
    }
    
    /**
     * Check if any faces were detected and extracted.
     */
    public boolean hasFaces() {
        return getFaceCount() > 0;
    }
    
    /**
     * Get bounding box for a specific face (normalized to 0-1).
     */
    @NonNull
    public RectF getNormalizedBoundingBox(int faceIndex, int imageWidth, int imageHeight) {
        if (faceIndex >= detectedFaces.size()) {
            return new RectF(0, 0, 0, 0);
        }
        
        Face face = detectedFaces.get(faceIndex);
        android.graphics.Rect box = face.getBoundingBox();
        
        return new RectF(
            box.left / (float) imageWidth,
            box.top / (float) imageHeight,
            box.right / (float) imageWidth,
            box.bottom / (float) imageHeight
        );
    }
    
    @Override
    public String toString() {
        return "FaceExtractionResult{" +
                "success=" + success +
                ", faceCount=" + getFaceCount() +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
