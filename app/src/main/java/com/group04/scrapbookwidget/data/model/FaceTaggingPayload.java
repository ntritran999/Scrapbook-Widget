package com.group04.scrapbookwidget.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * FaceTaggingPayload represents the data structure sent to the backend
 * when processing group photos with face detection and embedding extraction.
 * 
 * Contains:
 * - Multiple face embeddings extracted from a group photo
 * - Photo metadata
 * - User and group information
 */
public class FaceTaggingPayload {
    
    @SerializedName("photoId")
    private String photoId;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("groupId")
    private String groupId;
    
    @SerializedName("photoUri")
    private String photoUri;
    
    @SerializedName("timestamp")
    private long timestamp;
    
    @SerializedName("faceEmbeddings")
    private List<FaceEmbedding> faceEmbeddings;
    
    @SerializedName("photoWidth")
    private int photoWidth;
    
    @SerializedName("photoHeight")
    private int photoHeight;
    
    @SerializedName("caption")
    private String caption;
    
    public FaceTaggingPayload() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public FaceTaggingPayload(String userId, String groupId, String photoUri,
                             List<FaceEmbedding> faceEmbeddings) {
        this.userId = userId;
        this.groupId = groupId;
        this.photoUri = photoUri;
        this.faceEmbeddings = faceEmbeddings;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    
    public String getPhotoId() { return photoId; }
    public void setPhotoId(String photoId) { this.photoId = photoId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public List<FaceEmbedding> getFaceEmbeddings() { return faceEmbeddings; }
    public void setFaceEmbeddings(List<FaceEmbedding> faceEmbeddings) { 
        this.faceEmbeddings = faceEmbeddings; 
    }
    
    public int getPhotoWidth() { return photoWidth; }
    public void setPhotoWidth(int photoWidth) { this.photoWidth = photoWidth; }
    
    public int getPhotoHeight() { return photoHeight; }
    public void setPhotoHeight(int photoHeight) { this.photoHeight = photoHeight; }
    
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    
    /**
     * Nested class representing a single face embedding extracted from the photo.
     */
    public static class FaceEmbedding {
        
        @SerializedName("embeddingVector")
        private List<Double> embeddingVector;
        
        @SerializedName("boundingBox")
        private BoundingBox boundingBox;
        
        @SerializedName("confidence")
        private float confidence;
        
        public FaceEmbedding() {}
        
        public FaceEmbedding(List<Double> embeddingVector, BoundingBox boundingBox, float confidence) {
            this.embeddingVector = embeddingVector;
            this.boundingBox = boundingBox;
            this.confidence = confidence;
        }
        
        public List<Double> getEmbeddingVector() { return embeddingVector; }
        public void setEmbeddingVector(List<Double> embeddingVector) { 
            this.embeddingVector = embeddingVector; 
        }
        
        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox boundingBox) { 
            this.boundingBox = boundingBox; 
        }
        
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
    }
    
    /**
     * Nested class representing the bounding box of a detected face.
     * Coordinates are relative to the photo dimensions.
     */
    public static class BoundingBox {
        
        @SerializedName("left")
        private float left;
        
        @SerializedName("top")
        private float top;
        
        @SerializedName("right")
        private float right;
        
        @SerializedName("bottom")
        private float bottom;
        
        public BoundingBox() {}
        
        public BoundingBox(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        
        public float getLeft() { return left; }
        public void setLeft(float left) { this.left = left; }
        
        public float getTop() { return top; }
        public void setTop(float top) { this.top = top; }
        
        public float getRight() { return right; }
        public void setRight(float right) { this.right = right; }
        
        public float getBottom() { return bottom; }
        public void setBottom(float bottom) { this.bottom = bottom; }
        
        public float getWidth() { return right - left; }
        public float getHeight() { return bottom - top; }
    }
}
