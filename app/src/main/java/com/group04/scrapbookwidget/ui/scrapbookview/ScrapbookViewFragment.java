package com.group04.scrapbookwidget.ui.scrapbookview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentScrapbookViewBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScrapbookViewFragment extends Fragment {
    private static final String TMP_PREF_NAME = "TMP_USER_SESSION";

    private ScrapbookViewModel scrapbookViewModel;
    private String groupId = "", pageId = "";
    private String userId = "";

    private FragmentScrapbookViewBinding binding;
    private float dX, dY;
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

        setupObservers();
        setupClickListeners();

        // PRIORITY: Load pasted image from ImageEditor if present (this takes precedence)
        // ALWAYS show group selection dialog when entering pasting mode - no exceptions
        if (pastedImagePath != null && !pastedImagePath.isEmpty()) {
            android.util.Log.d("ScrapbookViewFragment", "onViewCreated - LOADING PASTED IMAGE: " + pastedImagePath);
            android.util.Log.d("ScrapbookViewFragment", "onViewCreated - Forcing group selection dialog for pasting mode");
            // Always show group selection dialog when pasting - user must select group each time
            showGroupSelectionDialog();
            return;  // Don't load anything until group is selected
        }
        // If no pasted image, load scrapbook with current group (or show dialog if empty)
        else if (groupId.isEmpty()) {
            android.util.Log.d("ScrapbookViewFragment", "GroupId is empty, showing group selection dialog");
            showGroupSelectionDialog();
        } else {
            // Load scrapbook with current groupId
            android.util.Log.d("ScrapbookViewFragment", "onViewCreated - Loading scrapbook normally");
            scrapbookViewModel.loadScrapbook(groupId, pageId);
            binding.btnSwitchGroup.setVisibility(View.INVISIBLE);
        }
    }

    private void setupObservers() {
        androidx.lifecycle.Observer<Boolean> loaderObserver = state -> {
            boolean isLoadingData = scrapbookViewModel.getIsLoading().getValue() != null && scrapbookViewModel.getIsLoading().getValue();
            boolean isRenderingGL = scrapbookViewModel.getIsRendering().getValue() != null && scrapbookViewModel.getIsRendering().getValue();
            
            if (binding != null) {
                binding.loadingOverlay.setVisibility((isLoadingData || isRenderingGL) ? View.VISIBLE : View.GONE);
            }
        };

        scrapbookViewModel.getIsLoading().observe(getViewLifecycleOwner(), loaderObserver);

        scrapbookViewModel.getIsRendering().observe(getViewLifecycleOwner(), rendering -> {
            loaderObserver.onChanged(rendering);
            
            if (!rendering && currentPastingView != null && !isInPastingMode) {
                android.util.Log.d("ScrapbookViewFragment", "Render complete. Removing temporary image safely.");
                exitPastingMode();
            }
        });

        scrapbookViewModel.getIsSavingItem().observe(getViewLifecycleOwner(), isSaving -> {
            android.util.Log.d("ScrapbookViewFragment", "setupObservers: isSavingItem changed to " + isSaving);
            if (!isSaving && binding != null) {
                android.util.Log.d("ScrapbookViewFragment", "setupObservers: Item save completed, waiting for pagesLiveData update");
            }
        });

        scrapbookViewModel.getItemSaveError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                android.util.Log.e("ScrapbookViewFragment", "setupObservers: Item save error: " + error);
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
            if (currentPastingView != null && !isInPastingMode) {
                android.util.Log.d("ScrapbookViewFragment", "setupObservers: Removing temporary pasted image after render");
                // exitPastingMode();
            }
        });
    }

    private void setupClickListeners() {
        binding.cameraBtn.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_scrapbookViewFragment_to_cameraFragment);
        });

        binding.btnConfirmPaste.setOnClickListener(v -> {
            if (currentPastingView != null) {
                confirmPastedImage();
            }
        });

        binding.btnSwitchGroup.setOnClickListener(v -> {
            showGroupSelectionDialog();
        });
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
            if (pastedImagePath != null && !pastedImagePath.isEmpty() && isInPastingMode == false) {
                android.util.Log.d("ScrapbookViewFragment", "Group selected - Loading pasted image");
                addPastedImageToScrapbook(pastedImagePath);
                enterPastingMode();
                // Load scrapbook in background for context
                scrapbookViewModel.loadScrapbook(groupId, "");
            } else {
                // Normal scrapbook load
                android.util.Log.d("ScrapbookViewFragment", "Group selected - Loading scrapbook normally");
                scrapbookViewModel.loadScrapbook(groupId, "");
            }
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
        binding.cameraBtn.setVisibility(View.INVISIBLE);
        binding.btnConfirmPaste.setVisibility(View.VISIBLE);
        Toast.makeText(requireContext(), "Ảnh sẵn sàng dán. Kéo, xoay tuỳ ý rồi nhấn Confirm", Toast.LENGTH_SHORT).show();
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
        binding.cameraBtn.setVisibility(View.VISIBLE);
        binding.btnConfirmPaste.setVisibility(View.INVISIBLE);
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
            return;
        }

        // Get the current page ID where user is viewing/pasting
        // This is tracked by PageFragment calling ViewModel.setCurrentDisplayingPageIndex()
        // Get via ViewModel which properly maps pageIndex to actual page ID
        int currentPageIndex = scrapbookViewModel.getPageIndex();
        String currentPageId = scrapbookViewModel.getCurrentPageId();
        String finalPageId = (currentPageId != null && !currentPageId.isEmpty()) ? currentPageId : pageId;

        if (finalPageId == null || finalPageId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid page", Toast.LENGTH_SHORT).show();
            android.util.Log.e("ScrapbookViewFragment", "confirmPastedImage: Invalid pageId");
            isInPastingMode = true;  // Restore flag since confirmation failed
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
                pastedImageRotation, pastedImageScale, pastedImageZIndex, pastedImageCaption
        );
        binding.btnSwitchGroup.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragListener(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_MOVE:
                    v.animate().x(event.getRawX() + dX).y(event.getRawY() + dY).setDuration(0).start();
                    break;
                case MotionEvent.ACTION_UP:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    v.performClick();
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("GROUP_ID", groupId);
        outState.putString("PAGE_ID", pageId);
        outState.putString("PASTED_IMAGE_PATH", pastedImagePath);
        outState.putBoolean("IS_IN_PASTING_MODE", isInPastingMode);
        outState.putString("CAPTION", pastedImageCaption);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isInPastingMode && pastedImagePath != null && !pastedImagePath.isEmpty()) {
            binding.cameraBtn.setVisibility(View.INVISIBLE);
            binding.btnConfirmPaste.setVisibility(View.VISIBLE);
        }
    }
}
