package com.group04.scrapbookwidget.data.repository;

import android.content.Context;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group04.scrapbookwidget.data.cache.AppCacheStore;
import com.group04.scrapbookwidget.data.cache.NetworkStatusProvider;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.service.AuthService;
import com.group04.scrapbookwidget.data.service.UserService;
import com.group04.scrapbookwidget.data.worker.WidgetUpdateWorker;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository implements IUserRepository {

    private static final String TAG = "UserRepository";
    private final UserService userService;
    private final AuthService authService;
    private final FirebaseAuth firebaseAuth;
    private final Context context;
    private final AppCacheStore cacheStore;
    private final NetworkStatusProvider networkStatusProvider;

    @Inject
    public UserRepository(UserService userService, FirebaseAuth firebaseAuth, AuthService authService,
                          @ApplicationContext Context context, AppCacheStore cacheStore,
                          NetworkStatusProvider networkStatusProvider) {
        this.userService = userService;
        this.authService = authService;
        this.firebaseAuth = firebaseAuth;
        this.context = context;
        this.cacheStore = cacheStore;
        this.networkStatusProvider = networkStatusProvider;
    }

    @Override
    public void login(String email, String password, RepositoryCallback<User> callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String idToken = tokenTask.getResult().getToken();
                                    verifySessionOnServer(idToken, callback);
                                } else {
                                    callback.onError(tokenTask.getException());
                                }
                            });
                        } else {
                            callback.onError(new Exception("Firebase user is null"));
                        }
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    @Override
    public void loginWithGoogle(String idToken, RepositoryCallback<User> callback) {
        User userRequest = new User();
        userRequest.setIdToken(idToken);

        Log.d(TAG, "loginWithGoogle: Calling POST /auth/google with token length: " + idToken.length());

        authService.loginWithGoogle(userRequest).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cacheStore.saveUser(response.body());
                    Log.d(TAG, "loginWithGoogle: SUCCESS");
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "Google login verification failed: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String serverError = response.errorBody().string();
                            Log.e(TAG, "Server error body: " + serverError);
                            errorMsg += " - " + serverError;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(new Exception(errorMsg));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Log.e(TAG, "loginWithGoogle: Network/Technical Failure", t);
                callback.onError(new Exception("Network error: " + t.getMessage()));
            }
        });
    }

    private void verifySessionOnServer(String idToken, RepositoryCallback<User> callback) {
        User userRequest = new User();
        userRequest.setIdToken(idToken);

        userService.verifySession(userRequest).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cacheStore.saveUser(response.body());
                    callback.onSuccess(response.body());
                    OneTimeWorkRequest workRequest = OneTimeWorkRequest.from(WidgetUpdateWorker.class);
                    WorkManager.getInstance(context)
                            .enqueueUniqueWork("updateWidgetAfterLogin", ExistingWorkPolicy.REPLACE, workRequest);
                } else {
                    callback.onError(new Exception("Server session verification failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void register(String email, String password, String name, RepositoryCallback<User> callback) {
        callback.onError(new Exception("Registration now requires OTP verification"));
    }

    @Override
    public void requestRegisterOtp(String email, RepositoryCallback<UserService.RegisterOtpResponse> callback) {
        userService.requestRegisterOtp(new UserService.RegisterOtpRequest(email)).enqueue(new Callback<UserService.RegisterOtpResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.RegisterOtpResponse> call,
                                   @NonNull Response<UserService.RegisterOtpResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception(readErrorMessage(response, "Failed to send OTP")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.RegisterOtpResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception("Network error: " + t.getMessage()));
            }
        });
    }

    @Override
    public void registerWithOtp(String email, String password, String displayName, String otpCode,
                                RepositoryCallback<UserService.RegisterResponse> callback) {
        UserService.RegisterOtpConfirmRequest request = new UserService.RegisterOtpConfirmRequest();
        request.email = email;
        request.password = password;
        request.otpCode = otpCode;
        request.displayName = displayName;
        request.username = buildUsernameFromEmail(email);
        request.nickname = displayName;
        request.status = "active";

        userService.registerWithOtp(request).enqueue(new Callback<UserService.RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.RegisterResponse> call,
                                   @NonNull Response<UserService.RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserService.RegisterResponse registerResponse = response.body();
                    User cachedUser = new User();
                    cachedUser.setUid(registerResponse.uid);
                    cachedUser.setEmail(registerResponse.email);
                    cachedUser.setDisplayName(displayName);
                    cachedUser.setUsername(request.username);
                    cachedUser.setNickname(request.nickname);
                    if (registerResponse.onboarding != null) {
                        cachedUser.setDefaultGroupId(registerResponse.onboarding.defaultGroupId);
                        cachedUser.setDefaultPageId(registerResponse.onboarding.defaultPageId);
                    }
                    cacheStore.saveUser(cachedUser);
                    callback.onSuccess(registerResponse);
                } else {
                    callback.onError(new Exception(readErrorMessage(response, "Registration failed")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.RegisterResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception("Network error: " + t.getMessage()));
            }
        });
    }

    @Override
    public void logout() {
        firebaseAuth.signOut();
        userService.logout().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Log.d(TAG, "Server logout success: " + response.isSuccessful());
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Server logout failed", t);
            }
        });
    }

    @Override
    public void getUserById(String userId, RepositoryCallback<User> callback) {
        userService.getUserById(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    User user = response.body();
                    if (user != null) {
                        cacheStore.saveUser(user);
                        callback.onSuccess(user);
                    } else {
                        callback.onError(new Exception("User not found"));
                    }
                } else {
                    deliverCachedUser(userId, callback, new Exception("User not found"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                deliverCachedUser(userId, callback, new Exception(t));
            }
        });
    }

    @Override
    public void getUserByUsername(String username, RepositoryCallback<User> callback) {
    }

    @Override
    public void createUser(User user, RepositoryCallback<Void> callback) {
        userService.createUser(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        cacheStore.saveUser(response.body());
                    }
                    callback.onSuccess(null);
                } else {
                    callback.onError(new Exception("Create user failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void updateUser(String userId, User updatedUser, RepositoryCallback<Void> callback) {
        userService.updateUser(userId, updatedUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    cacheMergedUser(userId, updatedUser);
                    callback.onSuccess(null);
                } else {
                    String errorMsg = "Update user failed";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body", e);
                    }
                    callback.onError(new Exception(errorMsg));
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void updateUserStatus(String userId, String status, RepositoryCallback<Void> callback) {
        User user = new User();
        user.setStatus(status);
        updateUser(userId, user, callback);
    }

    @Override
    public void updateAvatarUrl(String userId, String avatarUrl, RepositoryCallback<Void> callback) {
        User user = new User();
        user.setAvatarUrl(avatarUrl);
        updateUser(userId, user, callback);
    }

    @Override
    public void deleteUser(String userId, RepositoryCallback<Void> callback) {
        userService.deleteAccount().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        user.delete().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                callback.onSuccess(null);
                            } else {
                                callback.onError(task.getException());
                            }
                        });
                    } else {
                        callback.onSuccess(null);
                    }
                } else {
                    callback.onError(new Exception("Server account deletion failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void getUserGroups(String userId, RepositoryCallback<List<Group>> callback) {
        userService.getUserGroups(userId).enqueue(new Callback<List<Group>>() {
            @Override
            public void onResponse(@NonNull Call<List<Group>> call, @NonNull Response<List<Group>> response) {
                if (response.isSuccessful()) {
                    List<Group> groups = response.body();
                    if (groups != null) {
                        cacheStore.saveUserGroups(userId, groups);
                        for (Group group : groups) {
                            if (group != null) {
                                cacheStore.saveGroup(group);
                            }
                        }
                        callback.onSuccess(groups);
                    } else {
                        callback.onSuccess(java.util.Collections.emptyList());
                    }
                } else {
                    deliverCachedGroups(userId, callback, new Exception("Failed to fetch groups"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Group>> call, @NonNull Throwable t) {
                deliverCachedGroups(userId, callback, new Exception(t));
            }
        });
    }

    @Override
    public void checkUsername(String username, RepositoryCallback<UserService.UsernameCheckResponse> callback) {
        userService.checkUsername(username).enqueue(new Callback<UserService.UsernameCheckResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.UsernameCheckResponse> call, @NonNull Response<UserService.UsernameCheckResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Check username failed"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.UsernameCheckResponse> call, @NonNull Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }

    @Override
    public void uploadAvatar(File imageFile, RepositoryCallback<String> callback) {
        Log.d(TAG, "Starting avatar upload. File size: " + imageFile.length() + " bytes");
        
        String mimeType = getMimeType(imageFile);
        Log.d(TAG, "Detected MIME type: " + mimeType);

        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", imageFile.getName(), requestFile);

        userService.uploadAvatar(body).enqueue(new Callback<UserService.AvatarUploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserService.AvatarUploadResponse> call, @NonNull Response<UserService.AvatarUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Avatar upload success. URL: " + response.body().avatarUrl);
                    callback.onSuccess(response.body().avatarUrl);
                } else {
                    String errorMsg = "Avatar upload failed: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(new Exception(errorMsg));
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserService.AvatarUploadResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Avatar upload network failure", t);
                callback.onError(new Exception(t));
            }
        });
    }

    private String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getPath());
        if (extension == null || extension.isEmpty()) {
            String fileName = file.getName();
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1);
            }
        }
        if (extension != null) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (type != null) return type;
        }
        return "image/jpeg"; // Default fallback
    }

    /**
     * Save face embedding to user profile.
     * Called during user enrollment process.
     * The face embedding is extracted locally and sent to the backend for storage.
     *
     * @param userId Unique identifier of the user
     * @param faceEmbedding The extracted face embedding vector (List<Double>)
     * @param callback Callback for success/error handling
     */
    public void saveFaceEmbedding(@NonNull String userId, 
                                  @NonNull List<Double> faceEmbedding,
                                  @NonNull RepositoryCallback<Void> callback) {
        Log.d(TAG, "Saving face embedding for user: " + userId + 
              ", embedding size: " + faceEmbedding.size());
        
        // Create enrollment request with the face embedding
        UserService.FaceEnrollmentRequest enrollmentRequest = 
            new UserService.FaceEnrollmentRequest(faceEmbedding);
        
        // Send to backend via dedicated enrollment endpoint
        userService.enrollUserFace(userId, enrollmentRequest)
            .enqueue(new Callback<UserService.FaceEnrollmentResponse>() {
                @Override
                public void onResponse(@NonNull Call<UserService.FaceEnrollmentResponse> call,
                                      @NonNull Response<UserService.FaceEnrollmentResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UserService.FaceEnrollmentResponse enrollmentResponse = response.body();
                        boolean enrollmentSucceeded = enrollmentResponse.success
                                || enrollmentResponse.enrolledAt > 0
                                || looksLikeSuccessfulEnrollmentMessage(enrollmentResponse.message);
                        if (enrollmentSucceeded) {
                            Log.d(TAG, "Face embedding saved successfully for user: " + userId + 
                                  " at: " + enrollmentResponse.enrolledAt);
                            callback.onSuccess(null);
                        } else {
                            Log.e(TAG, "Backend enrollment failed: " + enrollmentResponse.message);
                            callback.onError(new Exception("Enrollment failed: " + enrollmentResponse.message));
                        }
                    } else {
                        String errorMsg = "Enrollment request failed: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                        Log.e(TAG, errorMsg);
                        callback.onError(new Exception(errorMsg));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserService.FaceEnrollmentResponse> call,
                                     @NonNull Throwable t) {
                    Log.e(TAG, "Failed to save face embedding for user: " + userId, t);
                    callback.onError(new Exception("Network error: " + t.getMessage()));
                }
            });
    }

    private boolean looksLikeSuccessfulEnrollmentMessage(String message) {
        if (message == null) {
            return false;
        }

        String normalizedMessage = message.trim().toLowerCase();
        return normalizedMessage.contains("success")
                || normalizedMessage.contains("successful")
                || normalizedMessage.contains("thanh cong")
                || normalizedMessage.contains("thành công")
                || normalizedMessage.contains("enroll complete");
    }

    /**
     * Retrieve user's face embedding from their profile.
     * Returns null if face embedding doesn't exist (user not enrolled).
     *
     * @param userId Unique identifier of the user
     * @param callback Callback containing the user with faceVector (may be null)
     */
    public void getUserFaceEmbedding(@NonNull String userId,
                                      @NonNull RepositoryCallback<List<Double>> callback) {
        Log.d(TAG, "Retrieving face embedding for user: " + userId);
        
        getUserById(userId, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getFaceVector() != null && !user.getFaceVector().isEmpty()) {
                    Log.d(TAG, "Face embedding found for user: " + userId + 
                          ", embedding size: " + user.getFaceVector().size());
                    callback.onSuccess(user.getFaceVector());
                } else {
                    Log.d(TAG, "No face embedding found for user: " + userId);
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Failed to retrieve user face embedding: " + userId, error);
                callback.onError(error);
            }
        });
    }

    /**
     * Check if user has already enrolled their face.
     * Returns true if faceVector exists in user profile.
     *
     * @param userId Unique identifier of the user
     * @param callback Callback with boolean result (true = enrolled, false = not enrolled)
     */
    public void hasUserEnrolledFace(@NonNull String userId,
                                     @NonNull RepositoryCallback<Boolean> callback) {
        getUserFaceEmbedding(userId, new RepositoryCallback<List<Double>>() {
            @Override
            public void onSuccess(List<Double> embedding) {
                boolean isEnrolled = embedding != null && !embedding.isEmpty();
                Log.d(TAG, "User " + userId + " face enrollment status: " + isEnrolled);
                callback.onSuccess(isEnrolled);
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error checking face enrollment status", error);
                callback.onError(error);
            }
        });
    }

    private void deliverCachedUser(@NonNull String userId,
                                   @NonNull RepositoryCallback<User> callback,
                                   @NonNull Exception originalError) {
        User cachedUser = cacheStore.getUser(userId);
        if (cachedUser != null) {
            callback.onSuccess(cachedUser);
            return;
        }
        callback.onError(networkStatusProvider.isOnline() ? originalError : new Exception("Offline mode: user profile unavailable in cache"));
    }

    private void deliverCachedGroups(@NonNull String userId,
                                     @NonNull RepositoryCallback<List<Group>> callback,
                                     @NonNull Exception originalError) {
        List<Group> cachedGroups = cacheStore.getUserGroups(userId);
        if (cachedGroups != null) {
            callback.onSuccess(cachedGroups);
            return;
        }
        callback.onError(networkStatusProvider.isOnline() ? originalError : new Exception("Offline mode: group list unavailable in cache"));
    }

    private void cacheMergedUser(@NonNull String userId, @NonNull User updatedUser) {
        User mergedUser = cacheStore.getUser(userId);
        if (mergedUser == null) {
            mergedUser = new User();
            mergedUser.setId(userId);
        }

        if (updatedUser.getUid() != null) mergedUser.setUid(updatedUser.getUid());
        if (updatedUser.getEmail() != null) mergedUser.setEmail(updatedUser.getEmail());
        if (updatedUser.getUsername() != null) mergedUser.setUsername(updatedUser.getUsername());
        if (updatedUser.getNickname() != null) mergedUser.setNickname(updatedUser.getNickname());
        if (updatedUser.getDisplayName() != null) mergedUser.setDisplayName(updatedUser.getDisplayName());
        if (updatedUser.getName() != null) mergedUser.setName(updatedUser.getName());
        if (updatedUser.getAvatarUrl() != null) mergedUser.setAvatarUrl(updatedUser.getAvatarUrl());
        if (updatedUser.getStatus() != null) mergedUser.setStatus(updatedUser.getStatus());
        if (updatedUser.getToken() != null) mergedUser.setToken(updatedUser.getToken());
        if (updatedUser.getIdToken() != null) mergedUser.setIdToken(updatedUser.getIdToken());
        if (updatedUser.getFaceVector() != null) mergedUser.setFaceVector(updatedUser.getFaceVector());
        if (updatedUser.getDefaultGroupId() != null) mergedUser.setDefaultGroupId(updatedUser.getDefaultGroupId());
        if (updatedUser.getDefaultPageId() != null) mergedUser.setDefaultPageId(updatedUser.getDefaultPageId());

        cacheStore.saveUser(mergedUser);
    }

    @NonNull
    private String buildUsernameFromEmail(@NonNull String email) {
        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String sanitized = localPart.trim().toLowerCase().replaceAll("[^a-z0-9._]", "_");
        return sanitized.isEmpty() ? "user" : sanitized;
    }

    @NonNull
    private <T> String readErrorMessage(@NonNull Response<T> response, @NonNull String fallback) {
        String errorMessage = fallback;
        try {
            if (response.errorBody() != null) {
                String rawBody = response.errorBody().string();
                if (rawBody != null && !rawBody.isEmpty()) {
                    JSONObject jsonObject = new JSONObject(rawBody);
                    errorMessage = jsonObject.optString("message", rawBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading error body", e);
        }
        return errorMessage;
    }
}
