package com.group04.scrapbookwidget.ui.scrapbookview;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.group04.scrapbookwidget.ui.pagecurl.PageBuilder;
import com.group04.scrapbookwidget.ui.pagecurl.PageCurlView;
import com.group04.scrapbookwidget.ui.pagecurl.PageResources;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PageFragment extends Fragment {
    private ScrapbookViewModel scrapbookViewModel;
    private PageCurlView pageCurlView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingPrepareBitmapsRunnable = null;
    private boolean isBitmapPreparing = false;
    private static final long BITMAP_PREPARE_DEBOUNCE_MS = 200;
    private final ExecutorService bitmapExecutor = Executors.newSingleThreadExecutor();
    private static final String TAG = "ScrapbookLoader";
    private long renderStartTime = 0;
    private long scrapbookLoadStartTime = 0;

    public PageFragment() {}

    public static PageFragment newInstance(String param1, String param2) {
        PageFragment fragment = new PageFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrapbookViewModel = new ViewModelProvider(requireParentFragment()).get(ScrapbookViewModel.class);
        scrapbookViewModel.getPagesLiveData().observe(getViewLifecycleOwner(), pages -> {
            if (pages != null && !pages.isEmpty()) {
                android.util.Log.d(TAG, "onViewCreated: Pages not empty, calling debouncedPrepareBitmaps");
                android.util.Log.d(TAG, "[RENDER_PIPELINE_START] Starting render preparation for page");
                renderStartTime = System.currentTimeMillis();
                // Debounce bitmap preparation to avoid frequent rendering
                debouncedPrepareBitmaps(pages, scrapbookViewModel.getPageIndex());

                pageCurlView.setPhotoRects(pages);
                pageCurlView.setOnPhotoHitListener((pageId, itemId) -> {
                    PhotoDialogFragment photoDialogFragment = PhotoDialogFragment.newInstance(scrapbookViewModel.getGroupId(), pageId, itemId);
                    photoDialogFragment.show(getChildFragmentManager(), PhotoDialogFragment.TAG);
                });

                pageCurlView.setOnPageChangedListener(newPageIndex -> {
                    android.util.Log.d("PageFragment", "Page flipped by user to index: " + newPageIndex);
                    scrapbookViewModel.setCurrentDisplayingPageIndex(newPageIndex);
                });
            }
            else {
                Toast.makeText(getContext(), "Cannot load pages.", Toast.LENGTH_SHORT).show();
            }
        });
        scrapbookViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), "Cannot load pages", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        pageCurlView = new PageCurlView(this.getActivity());
        return pageCurlView;
    }

    /**
     * Debounces bitmap preparation to prevent excessive re-renders during rapid updates.
     * This prevents ANR when multiple images are pasted or reloaded rapidly.
     */
    private void debouncedPrepareBitmaps(List<ScrapbookPageData> data, int page) {
        android.util.Log.d(TAG, "debouncedPrepareBitmaps: Called with " + (data != null ? data.size() : "null") + " pages, current page=" + page);

        // Skip if already preparing bitmaps
        if (isBitmapPreparing) {
            android.util.Log.d(TAG, "debouncedPrepareBitmaps: Already preparing, skipping");
            return;
        }

        // Remove any pending prepare task
        if (pendingPrepareBitmapsRunnable != null) {
            mainHandler.removeCallbacks(pendingPrepareBitmapsRunnable);
            android.util.Log.d(TAG, "debouncedPrepareBitmaps: Removed pending prepare task");
        }

        // Create the debounced task
        pendingPrepareBitmapsRunnable = () -> {
            android.util.Log.d(TAG, "debouncedPrepareBitmaps: Executing debounced prepare task");
            prepareBitmaps(data, page);
            pendingPrepareBitmapsRunnable = null;
        };

        // Post with debounce delay to allow UI to settle
        mainHandler.postDelayed(pendingPrepareBitmapsRunnable, BITMAP_PREPARE_DEBOUNCE_MS);
    }

    private void prepareBitmaps(List<ScrapbookPageData> data, int page) {
        android.util.Log.d(TAG, "[RENDER_PREPARE_START] Preparing bitmaps for page " + page);

        // Notify ViewModel about the currently rendering page
        scrapbookViewModel.setCurrentDisplayingPageIndex(page);

        pageCurlView.setCurPage(page);
        isBitmapPreparing = true;

        mainHandler.post(() -> scrapbookViewModel.getIsRendering().setValue(true));

        long bitmapPrepareStart = System.currentTimeMillis();
        
        // Use single-threaded executor to avoid thread pile-up
        bitmapExecutor.execute(() -> {
            try {
                android.util.Log.d(TAG, "[BITMAP_BUILD_PROCESS] Calling PageBuilder.buildPages on background thread");
                PageResources resources = PageBuilder.buildPages(getContext(), data);

                long bitmapPrepareDuration = System.currentTimeMillis() - bitmapPrepareStart;
                android.util.Log.d(TAG, "[BITMAP_PREPARE_COMPLETE] Bitmap preparation completed in " + bitmapPrepareDuration + "ms");
                android.util.Log.d(TAG, "[RENDER_QUEUE_START] Queuing render update to GL thread");

                pageCurlView.queueEvent(() -> {
                    pageCurlView.getPageRenderer().updatePageResources(resources);
                    pageCurlView.requestRender();
                    long renderTotalTime = System.currentTimeMillis() - renderStartTime;
                    android.util.Log.d(TAG, "[RENDER_COMPLETE] Page render completed in " + renderTotalTime + "ms (total from start)");
                    android.util.Log.d(TAG, "[UX_STRATEGY_SUMMARY] ✓ Strategy 1: Thumbnail loading (10% quality) ✓ Strategy 2: Shimmer skeleton UI ✓ Strategy 3: Image downsampling to screen size");
                    isBitmapPreparing = false;

                    mainHandler.post(() -> scrapbookViewModel.getIsRendering().setValue(false));
                });
            } catch (ExecutionException | InterruptedException e) {
                android.util.Log.e(TAG, "[RENDER_ERROR] Exception during bitmap preparation: " + e.getMessage());
                e.printStackTrace();
                isBitmapPreparing = false;

                long failDuration = System.currentTimeMillis() - renderStartTime;
                android.util.Log.d(TAG, "[RENDER_FAIL] Render failed after " + failDuration + "ms");

                mainHandler.post(() -> scrapbookViewModel.getIsRendering().setValue(false));
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load images", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up executor to avoid resource leak
        bitmapExecutor.shutdown();
        if (mainHandler != null && pendingPrepareBitmapsRunnable != null) {
            mainHandler.removeCallbacks(pendingPrepareBitmapsRunnable);
        }
    }
}