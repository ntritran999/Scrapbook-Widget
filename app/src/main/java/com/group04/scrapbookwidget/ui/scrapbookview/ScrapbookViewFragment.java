package com.group04.scrapbookwidget.ui.scrapbookview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.databinding.FragmentScrapbookViewBinding;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScrapbookViewFragment extends Fragment {
    private static final String TMP_PREF_NAME = "TMP_USER_SESSION";
    private static final String SETTING_PREF_NAME = "APP_SETTINGS";

    private ScrapbookViewModel scrapbookViewModel;
    private String groupId = "", pageId = "";
    private String userId = "";

    private FragmentScrapbookViewBinding binding;
    
    @Inject
    IUserRepository userRepository;
    
    private boolean hasCheckedFaceEnrollment = false;
    private boolean shouldPromptEnrollmentAfterGroupSelection = false;
    private boolean hasShownEnrollPrompt = false;
    private float dX, dY;
    private float initialPinchDistance;
    private float initialPinchWidth;
    private float initialPinchHeight;
    private float initialPinchCenterX;
    private float initialPinchCenterY;
    private ImageView currentPastingView;
    private String pastedImagePath;

    // Track if we're in pasting mode to avoid removing image prematurely
    private boolean isInPastingMode = false;

    private float pastedImageX = 0;
    private float pastedImageY = 0;
    private float pastedImageWidth = 0;
    private float pastedImageHeight = 0;
    private float pastedImageRotation = 0;
    private float pastedImageScale = 1.0f;
    private float pastedImageZIndex = 10f;
    private String pastedImageCaption = "";
    private List<List<Double>> pastedFaceEmbeddings;
    private boolean isConfirmingPaste = false;

    public ScrapbookViewFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ưu tiên lấy dữ liệu từ savedInstanceState nếu có (khi xoay màn hình), nếu không lấy từ arguments
        Bundle bundle = (savedInstanceState != null) ? savedInstanceState : getArguments();
        if (bundle != null) {
            String bundleGroupId = bundle.getString("GROUP_ID", "");
            String bundlePageId = bundle.getString("PAGE_ID", "");
            String bundlePastedImagePath = bundle.getString("PASTED_IMAGE_PATH");
            String bundleCaption = bundle.getString("CAPTION", "");
            hasCheckedFaceEnrollment = bundle.getBoolean("HAS_CHECKED_ENROLLMENT", false);
            List<List<Double>> bundleFaceEmbeddings = getFaceEmbeddingsFromBundle(bundle);

            // Only update if bundle has non-empty values (don't overwrite with empty strings)
            if (!bundleGroupId.isEmpty()) {
                groupId = bundleGroupId;
            }
            if (!bundlePageId.isEmpty()) {
                pageId = bundlePageId;
            }
            if (bundlePastedImagePath != null && !bundlePastedImagePath.isEmpty()) {
                pastedImagePath = bundlePastedImagePath;
            }
            if (bundleCaption != null) {
                pastedImageCaption = bundleCaption;
            }
            pastedFaceEmbeddings = bundleFaceEmbeddings;

            // Restore pasting mode flag
            isInPastingMode = bundle.getBoolean("IS_IN_PASTING_MODE", false);

            // Debug log
            android.util.Log.d("ScrapbookViewFragment", "onCreate - groupId: " + groupId +
                    ", pageId: " + pageId + ", pastedImagePath: " + pastedImagePath +
                    ", isInPastingMode: " + isInPastingMode + ", caption: " + pastedImageCaption);

            if (pastedImagePath != null && !pastedImagePath.isEmpty()) {
                android.util.Log.d("ScrapbookViewFragment", "onCreate - FOUND pasted image from bundle: " + pastedImagePath);
            }
        } else {
            android.util.Log.d("ScrapbookViewFragment", "onCreate - bundle is null, no pasted image");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_scrapbook_view, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userId = requireActivity()
                .getSharedPreferences(TMP_PREF_NAME, Activity.MODE_PRIVATE)
                .getString("USER_ID", "");

        scrapbookViewModel = new ViewModelProvider(this).get(ScrapbookViewModel.class);


        // Lưu GROUP_ID, PAGE_ID vào SharedPreferences để ImageEditorFragment dùng
        if (!groupId.isEmpty() && !pageId.isEmpty()) {
            requireActivity().getSharedPreferences(TMP_PREF_NAME, Activity.MODE_PRIVATE)
                    .edit()
                    .putString("CURRENT_GROUP_ID", groupId)
                    .putString("CURRENT_PAGE_ID", pageId)
                    .apply();
            android.util.Log.d("ScrapbookViewFragment",
                    "Saved to SharedPref - groupId: " + groupId + ", pageId: " + pageId);
        }

        // Tải setting hiệu ứng lật trang
        binding.btnSwitchPageCurlEffect.setChecked(requireActivity().getSharedPreferences(SETTING_PREF_NAME, Activity.MODE_PRIVATE)
                .getBoolean("PAGE_CURL_EFFECT_ENABLED", true));

        setupObservers();
        setupClickListeners();

        // PRIORITY: Load pasted image from ImageEditor if present (this takes precedence)
        // ALWAYS show group selection dialog when entering pasting mode - no exceptions
        if (pastedImagePath != null && !pastedImagePath.isEmpty()) {
            android.util.Log.d("ScrapbookViewFragment", "onViewCreated - LOADING PASTED IMAGE: " + pastedImagePath);
            android.util.Log.d("ScrapbookViewFragment", "onViewCreated - Forcing group selection dialog for pasting mode");
            currentPastingView = null;
            // Always show group selection dialog when pasting - user must select group each time
            shouldPromptEnrollmentAfterGroupSelection = true;
            showGroupSelectionDialog();
            return;  // Don't load anything until group is selected
        }
        // If no pasted image, load scrapbook with current group (or show dialog if empty)
        else if (groupId.isEmpty()) {
            android.util.Log.d("ScrapbookViewFragment", "GroupId is empty, showing group selection dialog");
            shouldPromptEnrollmentAfterGroupSelection = true;
            showGroupSelectionDialog();
        } else {
            // Load scrapbook with current groupId
            android.util.Log.d("ScrapbookViewFragment", "onViewCreated - Loading scrapbook normally");
            scrapbookViewModel.loadScrapbook(groupId, pageId);
            binding.btnSwitchGroup.setVisibility(View.INVISIBLE);
            maybeCheckFaceEnrollment(false);
        }
    }

    private void setupObservers() {
        androidx.lifecycle.Observer<Boolean> loaderObserver = state -> {
            boolean isLoadingData = scrapbookViewModel.getIsLoading().getValue() != null && scrapbookViewModel.getIsLoading().getValue();
            boolean isRenderingGL = scrapbookViewModel.getIsRendering().getValue() != null && scrapbookViewModel.getIsRendering().getValue();
            boolean isExporting = scrapbookViewModel.getIsExporting().getValue() != null && scrapbookViewModel.getIsExporting().getValue();

            if (binding != null) {
                binding.loadingOverlay.setVisibility((isLoadingData || isRenderingGL || isExporting) ? View.VISIBLE : View.GONE);
                if (isExporting) {
                    String status = scrapbookViewModel.getExportStatus().getValue();
                    binding.loadingText.setText(status != null ? status : "Saving Page...");
                } else {
                    binding.loadingText.setText("Loading Scrapbook...");
                }
            }
        };

        scrapbookViewModel.getIsLoading().observe(getViewLifecycleOwner(), loaderObserver);
        scrapbookViewModel.getIsRendering().observe(getViewLifecycleOwner(), loaderObserver);
        scrapbookViewModel.getIsExporting().observe(getViewLifecycleOwner(), loaderObserver);

        scrapbookViewModel.getExportStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null && !status.isEmpty()) {
                if (status.contains("saved")) {
                    showSaveSuccessDialog(status);
                } else if (status.contains("Error") || status.contains("Failed")) {
                    Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show();
                }
            }
        });

        scrapbookViewModel.getIsSavingItem().observe(getViewLifecycleOwner(), isSaving -> {
            android.util.Log.d("ScrapbookViewFragment", "setupObservers: isSavingItem changed to " + isSaving);
            if (binding != null) {
                boolean shouldEnableConfirm = Boolean.FALSE.equals(isSaving) && isInPastingMode && !isConfirmingPaste;
                binding.btnConfirmPaste.setEnabled(shouldEnableConfirm);
                if (!isSaving) {
                    android.util.Log.d("ScrapbookViewFragment", "setupObservers: Item save completed, waiting for pagesLiveData update");
                }
            }
        });

        scrapbookViewModel.getItemSaveError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                android.util.Log.e("ScrapbookViewFragment", "setupObservers: Item save error: " + error);
                isConfirmingPaste = false;
                exitPastingMode();
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe pagesLiveData to detect when render is complete and remove temporary image
        // BUT only if we're not in pasting mode (to avoid removing image when loading scrapbook)
        scrapbookViewModel.getPagesLiveData().observe(getViewLifecycleOwner(), pages -> {
            android.util.Log.d("ScrapbookViewFragment", "setupObservers: pagesLiveData observer triggered");
            android.util.Log.d("ScrapbookViewFragment", "  pages: " + (pages != null ? pages.size() : "null") + " page(s)");
            android.util.Log.d("ScrapbookViewFragment", "  currentPastingView: " + (currentPastingView != null ? "exists" : "null"));
            android.util.Log.d("ScrapbookViewFragment", "  isInPastingMode: " + isInPastingMode);

            // ONLY remove temporary image if user confirmed save (not during initial load)
            if (isConfirmingPaste && currentPastingView != null && !isInPastingMode) {
                android.util.Log.d("ScrapbookViewFragment", "setupObservers: Removing temporary pasted image after render");
                isConfirmingPaste = false;
                exitPastingMode();
            }
        });
    }

    private void setupClickListeners() {
        binding.cameraBtn.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_scrapbookViewFragment_to_cameraFragment);
        });

        binding.btnConfirmPaste.setOnClickListener(v -> {
            if (currentPastingView != null && !isConfirmingPaste) {
                isConfirmingPaste = true;
                binding.btnConfirmPaste.setEnabled(false);
                confirmPastedImage();
            }
        });

        binding.btnSwitchGroup.setOnClickListener(v -> {
            showGroupSelectionDialog();
        });

        binding.btnSwitchPageCurlEffect.setOnClickListener(v -> {
            saveEffectChoice(binding.btnSwitchPageCurlEffect.isChecked());
        });

        binding.btnSavePage.setOnClickListener(v -> {
            scrapbookViewModel.saveCurrentPageToStorage(requireContext());
        });
    }

    private void saveEffectChoice(boolean isEnabled) {
        requireActivity().getSharedPreferences(SETTING_PREF_NAME, Activity.MODE_PRIVATE)
                .edit()
                .putBoolean("PAGE_CURL_EFFECT_ENABLED", isEnabled)
                .apply();
        scrapbookViewModel.togglePageCurlEffect(isEnabled);
    }

    private void showSaveSuccessDialog(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Saved")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showGroupSelectionDialog() {
        GroupSelectionDialogFragment dialog = new GroupSelectionDialogFragment();
        dialog.setOnGroupSelectedListener(group -> {
            // Update groupId
            groupId = group.getId();
            pageId = ""; // Reset pageId when switching groups

            // Save to SharedPreferences
            requireActivity().getSharedPreferences(TMP_PREF_NAME, Activity.MODE_PRIVATE)
                    .edit()
                    .putString("CURRENT_GROUP_ID", groupId)
                    .remove("CURRENT_PAGE_ID")
                    .apply();

            android.util.Log.d("ScrapbookViewFragment", "Group selected - groupId: " + groupId);

            // Check if we have a pasted image waiting to be placed
            if (pastedImagePath != null && !pastedImagePath.isEmpty()) {
                android.util.Log.d("ScrapbookViewFragment", "Group selected - Loading pasted image");
                if (currentPastingView == null) {
                    addPastedImageToScrapbook(pastedImagePath);
                }
                enterPastingMode();
                // Load scrapbook in background for context
                scrapbookViewModel.loadScrapbook(groupId, "");
            } else {
                // Normal scrapbook load
                android.util.Log.d("ScrapbookViewFragment", "Group selected - Loading scrapbook normally");
                scrapbookViewModel.loadScrapbook(groupId, "");
            }

            maybeCheckFaceEnrollment(true);
        });
        dialog.setCancelable(false); // Force user to select a group
        dialog.show(getChildFragmentManager(), "GroupSelectionDialog");
    }

    private void enterPastingMode() {
        if (binding == null) {
            android.util.Log.e("ScrapbookViewFragment", "enterPastingMode: binding is null!");
            return;
        }
        android.util.Log.d("ScrapbookViewFragment", "enterPastingMode: Entering pasting mode");
        isInPastingMode = true;
        isConfirmingPaste = false;
        binding.cameraBtn.setVisibility(View.INVISIBLE);
        binding.btnSwitchPageCurlEffect.setVisibility(View.INVISIBLE);
        binding.btnConfirmPaste.setVisibility(View.VISIBLE);
        binding.btnConfirmPaste.setEnabled(true);
        binding.btnSavePage.setVisibility(View.GONE);
        Toast.makeText(requireContext(), "Please patse the image to scrapbook", Toast.LENGTH_SHORT).show();
    }

    private void exitPastingMode() {
        if (binding == null) {
            android.util.Log.w("ScrapbookViewFragment", "exitPastingMode: binding is null");
            return;
        }

        if (currentPastingView != null) {
            currentPastingView.setOnTouchListener(null);
            binding.scrapbookFrame.removeView(currentPastingView);
            currentPastingView = null;
            android.util.Log.d("ScrapbookViewFragment", "exitPastingMode: Removed temporary pasted image");
        }
        pastedImagePath = null;
        isInPastingMode = false;
        isConfirmingPaste = false;
        binding.cameraBtn.setVisibility(View.VISIBLE);
        binding.btnSwitchPageCurlEffect.setVisibility(View.INVISIBLE);
        binding.btnConfirmPaste.setVisibility(View.INVISIBLE);
        binding.btnSavePage.setVisibility(View.VISIBLE);
    }

    private void addPastedImageToScrapbook(String path) {
        if (binding == null || binding.scrapbookFrame == null) {
            android.util.Log.e("ScrapbookViewFragment", "addPastedImageToScrapbook: binding or scrapbookFrame is null!");
            return;
        }

        if (path == null || path.isEmpty()) {
            android.util.Log.e("ScrapbookViewFragment", "addPastedImageToScrapbook: path is null or empty");
            Toast.makeText(requireContext(), "Invalid image path", Toast.LENGTH_SHORT).show();
            return;
        }

        // Group must be selected before adding image (enforced in onViewCreated)
        if (groupId == null || groupId.isEmpty()) {
            android.util.Log.e("ScrapbookViewFragment", "addPastedImageToScrapbook: ERROR - GroupId is empty!");
            Toast.makeText(requireContext(), "Please select a group first", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("ScrapbookViewFragment", "addPastedImageToScrapbook: Loading image from path: " + path);

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Lỗi tải ảnh edit", Toast.LENGTH_SHORT).show();
            android.util.Log.e("ScrapbookViewFragment", "Failed to decode bitmap from: " + path);
            return;
        }

        android.util.Log.d("ScrapbookViewFragment", "Successfully loaded bitmap, size: " +
                bitmap.getWidth() + "x" + bitmap.getHeight());

        currentPastingView = new ImageView(requireContext());
        currentPastingView.setImageBitmap(bitmap);
        currentPastingView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Calculate initial display size on this device's preview screen
        // Scrapbook page is typically 1080x2400, but display preview is device screen
        int frameWidth = binding.scrapbookFrame.getWidth();
        int frameHeight = binding.scrapbookFrame.getHeight();
        if (frameWidth <= 0 || frameHeight <= 0) {
            frameWidth = getResources().getDisplayMetrics().widthPixels;
            frameHeight = (int)(frameWidth * 2.22); // Approx 1080x2400 aspect ratio
        }
        
        // Initial size: 40% of visible frame width, maintain aspect ratio
        int imageWidth = (int) (frameWidth * 0.4);
        int imageHeight = (int) (imageWidth * bitmap.getHeight() / (float) bitmap.getWidth());
        
        // Position: centered horizontally, positioned in middle vertically
        int leftMargin = (frameWidth - imageWidth) / 2;
        int topMargin = (frameHeight - imageHeight) / 2;
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(imageWidth, imageHeight);
        params.leftMargin = leftMargin;
        params.topMargin = topMargin;
        currentPastingView.setLayoutParams(params);
        currentPastingView.setElevation(pastedImageZIndex);

        setupDragListener(currentPastingView);
        binding.scrapbookFrame.addView(currentPastingView);

        android.util.Log.d("ScrapbookViewFragment", "Pasted image added to scrapbook frame");
        android.util.Log.d("ScrapbookViewFragment", "  Frame size: " + frameWidth + "x" + frameHeight);
        android.util.Log.d("ScrapbookViewFragment", "  Image initial size: " + imageWidth + "x" + imageHeight);
        android.util.Log.d("ScrapbookViewFragment", "  Position: left=" + leftMargin + ", top=" + topMargin);
    }

    private void confirmPastedImage() {
        if (currentPastingView == null) {
            android.util.Log.w("ScrapbookViewFragment", "confirmPastedImage: currentPastingView is null");
            isConfirmingPaste = false;
            return;
        }

        android.util.Log.d("ScrapbookViewFragment", "confirmPastedImage: Starting confirm process");

        // Signal that we're exiting pasting mode so observer can remove temporary image after render
        isInPastingMode = false;

        // Validate that we have a valid group and page
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a group first", Toast.LENGTH_SHORT).show();
            android.util.Log.e("ScrapbookViewFragment", "confirmPastedImage: Invalid groupId");
            isInPastingMode = true;  // Restore flag since confirmation failed
            isConfirmingPaste = false;
            if (binding != null) {
                binding.btnConfirmPaste.setEnabled(true);
            }
            return;
        }

        // Get the current page ID where user is viewing/pasting
        // This is tracked by PageFragment calling ViewModel.setCurrentDisplayingPageIndex()
        // Get via ViewModel which properly maps pageIndex to actual page ID
        int currentPageIndex = scrapbookViewModel.getPageIndex();
        String currentPageId = scrapbookViewModel.getCurrentPageId();
        String finalPageId = (currentPageId != null && !currentPageId.isEmpty()) ? currentPageId : pageId;

        if (finalPageId == null || finalPageId.isEmpty()) {
            showBackgroundSelectionDialog();
            isInPastingMode = true;  // Restore flag since confirmation failed
            isConfirmingPaste = false;
            if (binding != null) {
                binding.btnConfirmPaste.setEnabled(true);
            }
            return;
        }

        android.util.Log.d("ScrapbookViewFragment", "confirmPastedImage: Page tracking info");
        android.util.Log.d("ScrapbookViewFragment", "  currentPageIndex from ViewModel: " + currentPageIndex);
        android.util.Log.d("ScrapbookViewFragment", "  currentPageId from ViewModel: " + currentPageId);
        android.util.Log.d("ScrapbookViewFragment", "  fallback pageId from args: " + pageId);
        android.util.Log.d("ScrapbookViewFragment", "  FINAL pageId for save: " + finalPageId);

        // Get the actual transformed dimensions after user dragging/scaling
        // Note: getX/Y are relative to parent FrameLayout, getWidth/Height are actual rendered size
        float currentX = currentPastingView.getX();
        float currentY = currentPastingView.getY();
        float currentWidth = currentPastingView.getWidth();
        float currentHeight = currentPastingView.getHeight();
        float currentScale = currentPastingView.getScaleX();

        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int targetWidth = 1080;
        int targetHeight = (int) (targetWidth * ((float) displayMetrics.heightPixels / displayMetrics.widthPixels));

        int frameWidth = binding.scrapbookFrame.getWidth();
        int frameHeight = binding.scrapbookFrame.getHeight();

        float scaleX = frameWidth > 0 ? (float) targetWidth / frameWidth : 1f;
        float scaleY = frameHeight > 0 ? (float) targetHeight / frameHeight : 1f;

        pastedImageX = currentX * scaleX;
        pastedImageY = currentY * scaleY;
        pastedImageWidth = currentWidth * scaleX;
        pastedImageHeight = currentHeight * scaleY;
        pastedImageScale = currentScale;

        android.util.Log.d("ScrapbookViewFragment", "confirmPastedImage: Saving item to server");
        android.util.Log.d("ScrapbookViewFragment", "  groupId=" + groupId + ", pageId=" + finalPageId + ", userId=" + userId);
        android.util.Log.d("ScrapbookViewFragment", "  imagePath=" + pastedImagePath);
        android.util.Log.d("ScrapbookViewFragment", "  caption=" + pastedImageCaption);

        Toast.makeText(requireContext(), "Saving image", Toast.LENGTH_SHORT).show();

        scrapbookViewModel.saveScrapbookItem(
                finalPageId, pastedImagePath, userId,
                pastedImageX, pastedImageY, pastedImageWidth, pastedImageHeight,
                pastedImageRotation, pastedImageScale, pastedImageZIndex, pastedImageCaption,
                pastedFaceEmbeddings
        );
        binding.btnSwitchGroup.setVisibility(View.INVISIBLE);
    }

    /**
     * Check if user enroll their face.
     * Just check once in this view's life cycle.
     */
    private void checkFaceEnrollmentStatus() {
        if (hasCheckedFaceEnrollment || userId == null || userId.isEmpty()) {
            return;
        }

        userRepository.hasUserEnrolledFace(userId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isEnrolled) {
                hasCheckedFaceEnrollment = true;

                if (!isEnrolled) {
                    android.util.Log.d("ScrapbookViewFragment", "User chưa có faceVector, hiển thị prompt...");
                    showEnrollFacePrompt();
                } else {
                    android.util.Log.d("ScrapbookViewFragment", "User ĐÃ CÓ faceVector, bỏ qua prompt.");
                }
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.e("ScrapbookViewFragment", "Lỗi khi kiểm tra face enrollment", e);
                // Nếu lỗi mạng, có thể để hasCheckedFaceEnrollment = false để lần sau check lại
                hasCheckedFaceEnrollment = false;
            }
        });
    }

    private void maybeCheckFaceEnrollment(boolean completedGroupSelection) {
        if (completedGroupSelection) {
            shouldPromptEnrollmentAfterGroupSelection = false;
            checkFaceEnrollmentStatus();
            return;
        }

        if (!groupId.isEmpty() && !shouldPromptEnrollmentAfterGroupSelection) {
            checkFaceEnrollmentStatus();
        }
    }

    /**
     * Display Bottom Sheet to enroll user's face.
     */
    private void showEnrollFacePrompt() {
        if (hasShownEnrollPrompt || !isAdded()) {
            return;
        }

        hasShownEnrollPrompt = true;
        EnrollFaceBottomSheetFragment enrollBottomSheet = EnrollFaceBottomSheetFragment.newInstance();

        enrollBottomSheet.setEnrollmentListener(new EnrollFaceBottomSheetFragment.OnEnrollmentCompleteListener() {
            @Override
            public void onEnrollmentComplete(List<Double> faceEmbedding) {
                Toast.makeText(requireContext(), "Face Setup Complete!.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEnrollmentSkipped() {
                hasShownEnrollPrompt = false;
                android.util.Log.d("ScrapbookViewFragment", "User skipped face enrollment.");
            }

            @Override
            public void onEnrollmentFailed(String errorMessage) {
                hasShownEnrollPrompt = false;
                android.util.Log.e("ScrapbookViewFragment", "Face enrollment failed: " + errorMessage);
            }
        });

        enrollBottomSheet.show(getChildFragmentManager(), "EnrollFaceBottomSheet");
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private List<List<Double>> getFaceEmbeddingsFromBundle(@NonNull Bundle bundle) {
        Object serializedEmbeddings = bundle.getSerializable("FACE_EMBEDDINGS");
        if (serializedEmbeddings instanceof ArrayList<?>) {
            return new ArrayList<>((ArrayList<ArrayList<Double>>) serializedEmbeddings);
        }
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragListener(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    v.setAlpha(0.92f);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == 2) {
                        initialPinchDistance = getPointerDistance(event);
                        initialPinchWidth = v.getWidth();
                        initialPinchHeight = v.getHeight();
                        initialPinchCenterX = v.getX() + (v.getWidth() / 2f);
                        initialPinchCenterY = v.getY() + (v.getHeight() / 2f);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() >= 2) {
                        resizePastedImage(v, event);
                    } else {
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    if (event.getPointerCount() - 1 < 2) {
                        initialPinchDistance = 0f;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    initialPinchDistance = 0f;
                    v.setAlpha(1.0f);
                    v.performClick();
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

q    private void resizePastedImage(View view, MotionEvent event) {
        if (initialPinchDistance <= 0f) {
            return;
        }

        float currentDistance = getPointerDistance(event);
        if (currentDistance <= 0f) {
            return;
        }

        float scaleFactor = currentDistance / initialPinchDistance;
        int minSize = (int) (120 * getResources().getDisplayMetrics().density);
        int maxWidth = binding != null && binding.scrapbookFrame != null
                ? binding.scrapbookFrame.getWidth()
                : 0;
        int maxHeight = binding != null && binding.scrapbookFrame != null
                ? binding.scrapbookFrame.getHeight()
                : 0;

        float targetWidth = Math.max(minSize, initialPinchWidth * scaleFactor);
        float targetHeight = Math.max(minSize, initialPinchHeight * scaleFactor);

        if (maxWidth > 0 && targetWidth > maxWidth) {
            float ratio = maxWidth / targetWidth;
            targetWidth = maxWidth;
            targetHeight *= ratio;
        }
        if (maxHeight > 0 && targetHeight > maxHeight) {
            float ratio = maxHeight / targetHeight;
            targetHeight = maxHeight;
            targetWidth *= ratio;
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.width = Math.round(targetWidth);
        layoutParams.height = Math.round(targetHeight);
        view.setLayoutParams(layoutParams);

        float newX = initialPinchCenterX - (targetWidth / 2f);
        float newY = initialPinchCenterY - (targetHeight / 2f);

        if (binding != null && binding.scrapbookFrame != null) {
            float boundedX = Math.max(0f, Math.min(newX, binding.scrapbookFrame.getWidth() - targetWidth));
            float boundedY = Math.max(0f, Math.min(newY, binding.scrapbookFrame.getHeight() - targetHeight));
            view.setX(boundedX);
            view.setY(boundedY);
        } else {
            view.setX(newX);
            view.setY(newY);
        }
    }

    private float getPointerDistance(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return 0f;
        }

        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.hypot(dx, dy);
    }

    private void showBackgroundSelectionDialog() {
        BackgroundSelectionDialogFragment dialog = new BackgroundSelectionDialogFragment();
        dialog.setOnBackgroundSelectListener(url -> {
            scrapbookViewModel.createScrapbookPage(url);
        });
        dialog.setCancelable(false);
        dialog.show(getChildFragmentManager(), BackgroundSelectionDialogFragment.TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("GROUP_ID", groupId);
        outState.putString("PAGE_ID", pageId);
        outState.putString("PASTED_IMAGE_PATH", pastedImagePath);
        outState.putBoolean("IS_IN_PASTING_MODE", isInPastingMode);
        outState.putString("CAPTION", pastedImageCaption);
        outState.putBoolean("HAS_CHECKED_ENROLLMENT", hasCheckedFaceEnrollment);
        if (pastedFaceEmbeddings != null) {
            ArrayList<ArrayList<Double>> serializableEmbeddings = new ArrayList<>();
            for (List<Double> embedding : pastedFaceEmbeddings) {
                serializableEmbeddings.add(embedding != null ? new ArrayList<>(embedding) : new ArrayList<>());
            }
            outState.putSerializable("FACE_EMBEDDINGS", serializableEmbeddings);
        } else {
            outState.putSerializable("FACE_EMBEDDINGS", null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isInPastingMode && pastedImagePath != null && !pastedImagePath.isEmpty()) {
            binding.cameraBtn.setVisibility(View.INVISIBLE);
            binding.btnSwitchPageCurlEffect.setVisibility(View.INVISIBLE);
            binding.btnConfirmPaste.setVisibility(View.VISIBLE);
            binding.btnConfirmPaste.setEnabled(!isConfirmingPaste);
        }
    }
}
