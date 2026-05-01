package com.group04.scrapbookwidget.ui.scrapbookview;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.group04.scrapbookwidget.R;
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
    private static final String SETTING_PREF_NAME = "APP_SETTINGS";
    private ScrapbookViewModel scrapbookViewModel;
    private PageCurlView pageCurlView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingPrepareBitmapsRunnable = null;
    private boolean isBitmapPreparing = false;
    private static final long BITMAP_PREPARE_DEBOBUNCE_MS = 200;
    private final ExecutorService bitmapExecutor = Executors.newSingleThreadExecutor();
    private static final String TAG = "ScrapbookLoader";
    private long renderStartTime = 0;

    public PageFragment() {}

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrapbookViewModel = new ViewModelProvider(requireParentFragment()).get(ScrapbookViewModel.class);
        
        scrapbookViewModel.getPagesLiveData().observe(getViewLifecycleOwner(), pages -> {
            if (pages != null && !pages.isEmpty()) {
                android.util.Log.d(TAG, "onViewCreated: Pages not empty, calling debouncedPrepareBitmaps");
                renderStartTime = System.currentTimeMillis();
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

                pageCurlView.setOnSwipeToNewPageListener(this::handleSwipeToNewPage);
            }
            else {
                Toast.makeText(getContext(), "Cannot load pages.", Toast.LENGTH_SHORT).show();
            }
        });

        scrapbookViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        scrapbookViewModel.getIsPageCurlEffectEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            if (isEnabled != null) {
                pageCurlView.togglePageCurlEffect(isEnabled);
            }
        });
    }

    private void handleSwipeToNewPage() {
        if (scrapbookViewModel.canAddNewPage()) {
            showAddNewPageDialog();
        } else {
            showUpgradePrompt();
        }
    }

    private void showAddNewPageDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_new_page)
                .setMessage("A new page will be created using the same background.")
                .setPositiveButton("Create Page", (dialog, which) -> {
                    // Automatically use the background from the current last page
                    List<ScrapbookPageData> pages = scrapbookViewModel.getPagesLiveData().getValue();
                    String lastBackground = "";
                    if (pages != null && !pages.isEmpty()) {
                        lastBackground = pages.get(pages.size() - 1).scrapbookPage.getBackgroundImage();
                    }
                    scrapbookViewModel.createScrapbookPage(lastBackground);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showUpgradePrompt() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Scrapbook Limit Reached")
                .setMessage(R.string.upgrade_to_add_more)
                .setPositiveButton("Upgrade to Gold", (dialog, which) -> {
                    Toast.makeText(getContext(), "Redirecting to Premium...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        pageCurlView = new PageCurlView(this.getActivity(),
                requireActivity().getSharedPreferences(SETTING_PREF_NAME, Activity.MODE_PRIVATE)
                        .getBoolean("PAGE_CURL_EFFECT_ENABLED", true));
        return pageCurlView;
    }

    private void debouncedPrepareBitmaps(List<ScrapbookPageData> data, int page) {
        if (isBitmapPreparing) return;

        if (pendingPrepareBitmapsRunnable != null) {
            mainHandler.removeCallbacks(pendingPrepareBitmapsRunnable);
        }

        pendingPrepareBitmapsRunnable = () -> {
            prepareBitmaps(data, page);
            pendingPrepareBitmapsRunnable = null;
        };

        mainHandler.postDelayed(pendingPrepareBitmapsRunnable, BITMAP_PREPARE_DEBOBUNCE_MS);
    }

    private void prepareBitmaps(List<ScrapbookPageData> data, int page) {
        scrapbookViewModel.setCurrentDisplayingPageIndex(page);
        pageCurlView.setCurPage(page);
        isBitmapPreparing = true;

        mainHandler.post(() -> scrapbookViewModel.getIsRendering().setValue(true));
        long bitmapPrepareStart = System.currentTimeMillis();
        
        bitmapExecutor.execute(() -> {
            try {
                PageResources resources = PageBuilder.buildPages(getContext(), data);
                pageCurlView.queueEvent(() -> {
                    pageCurlView.getPageRenderer().updatePageResources(resources);
                    pageCurlView.requestRender();
                    isBitmapPreparing = false;
                    mainHandler.post(() -> scrapbookViewModel.getIsRendering().setValue(false));
                });
            } catch (ExecutionException | InterruptedException e) {
                isBitmapPreparing = false;
                mainHandler.post(() -> scrapbookViewModel.getIsRendering().setValue(false));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bitmapExecutor.shutdown();
        if (mainHandler != null && pendingPrepareBitmapsRunnable != null) {
            mainHandler.removeCallbacks(pendingPrepareBitmapsRunnable);
        }
    }
}
