package com.group04.scrapbookwidget.ui.scrapbookview;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.model.ItemContent;
import com.group04.scrapbookwidget.data.model.Layout;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;
import com.group04.scrapbookwidget.data.repository.IScrapbookRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;
import com.group04.scrapbookwidget.ui.pagecurl.PageBuilder;
import com.group04.scrapbookwidget.ui.pagecurl.PageResources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ScrapbookViewModel extends ViewModel {
    private final IScrapbookRepository scrapbookRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int pageIndex = 0;
    private String groupId;
    private String defaultPageId;
    private final MutableLiveData<List<ScrapbookPageData>> pagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSavingItem = new MutableLiveData<>(false);
    private final MutableLiveData<String> itemSaveError = new MutableLiveData<>();
    
    private final MutableLiveData<Boolean> isRendering = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> getIsRendering() { return isRendering; }

    private final MutableLiveData<Boolean> isExporting = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsExporting() { return isExporting; }

    private final MutableLiveData<String> exportStatus = new MutableLiveData<>();
    public LiveData<String> getExportStatus() { return exportStatus; }
    
    // Debounce mechanism for reload
    private Runnable pendingReloadRunnable = null;
    private static final long RELOAD_DELAY_MS = 100;
    
    @Inject
    public ScrapbookViewModel(IScrapbookRepository repo) {
        scrapbookRepository = repo;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setCurrentDisplayingPageIndex(int index) {
        if (this.pageIndex != index) {
            android.util.Log.d("ScrapbookViewModel", "setCurrentDisplayingPageIndex: Updating pageIndex from " + this.pageIndex + " to " + index);
            this.pageIndex = index;
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public String getCurrentPageId() {
        List<ScrapbookPageData> pages = pagesLiveData.getValue();
        if (pages != null && pageIndex >= 0 && pageIndex < pages.size()) {
            return pages.get(pageIndex).scrapbookPage.getId();
        }
        return null;
    }

    public LiveData<List<ScrapbookPageData>> getPagesLiveData() {
        return pagesLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsSavingItem() {
        return isSavingItem;
    }

    public LiveData<String> getItemSaveError() {
        return itemSaveError;
    }

    public void loadScrapbook(String groupId, String pageId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            errorMessage.setValue("Invalid group ID");
            isLoading.setValue(false);
            return;
        }

        isLoading.setValue(true);
        this.groupId = groupId;

        scrapbookRepository.getAllPages(groupId, new RepositoryCallback<List<ScrapbookPage>>() {
            @Override
            public void onSuccess(List<ScrapbookPage> pages) {
                if (pages == null || pages.isEmpty()) {
                    isLoading.setValue(false);
                    return;
                }

                if (pages.size() == 1) {
                    ScrapbookPage firstPage = pages.get(0);
                    String backgroundImageUrl = firstPage.getBackgroundImage();
                    if (backgroundImageUrl == null || backgroundImageUrl.isEmpty()) {
                        defaultPageId = firstPage.getId();
                        isLoading.setValue(false);
                        Log.d("create-page-defaultPageId", defaultPageId + "");
                        return;
                    }
                }

                List<ScrapbookItem>[] resultsArray = new List[pages.size()];
                AtomicInteger completedCount = new AtomicInteger(0);
                boolean[] hasError = {false};

                for (int i = 0; i < pages.size(); i++) {
                    final int index = i;
                    scrapbookRepository.getAllItems(groupId, pages.get(i).getId(), new RepositoryCallback<List<ScrapbookItem>>() {
                        @Override
                        public void onSuccess(List<ScrapbookItem> items) {
                            if (hasError[0]) return;
                            resultsArray[index] = items;
                            if (completedCount.incrementAndGet() == pages.size()) {
                                List<ScrapbookPageData> scrapbookData = new ArrayList<>();
                                for (int j = 0; j < pages.size(); j++) {
                                    scrapbookData.add(new ScrapbookPageData(pages.get(j), resultsArray[j]));
                                    if (pages.get(j).getId().equals(pageId)) {
                                        pageIndex = j;
                                    }
                                }
                                pagesLiveData.setValue(scrapbookData);
                                isLoading.setValue(false);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (hasError[0]) return;
                            hasError[0] = true;
                            errorMessage.setValue("Failed to load items: " + e.getMessage());
                            isLoading.setValue(false);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Failed to load pages: " + e.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    public void createScrapbookPage(String backgroundImageUrl) {
        isLoading.setValue(true);
        ScrapbookPage newPage = new ScrapbookPage();
        newPage.setBackgroundImage(backgroundImageUrl);
        scrapbookRepository.createPage(groupId, newPage, new RepositoryCallback<ScrapbookPage>() {
            @Override
            public void onSuccess(ScrapbookPage result) {
                List<ScrapbookPageData> scrapbookData = new ArrayList<>();
                scrapbookData.add(new ScrapbookPageData(result, new ArrayList<>()));
                if (defaultPageId != null && !defaultPageId.isEmpty()) {
                    scrapbookRepository.removePage(groupId, defaultPageId, new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            pagesLiveData.setValue(scrapbookData);
                            pageIndex = 0;
                            loadScrapbook(groupId, "");
                            isLoading.setValue(false);
                        }

                        @Override
                        public void onError(Exception exception) {
                            pagesLiveData.setValue(new ArrayList<>());
                            errorMessage.setValue("Failed to create initial page: " + exception.getMessage());
                            isLoading.setValue(false);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception exception) {
                pagesLiveData.setValue(new ArrayList<>());
                errorMessage.setValue("Failed to create initial page: " + exception.getMessage());
                isLoading.setValue(false);
            }
        });
    }

    private void updatePageWithNewItem(String pageId, ScrapbookItem newItem) {
        if (newItem == null || pageId == null) return;

        List<ScrapbookPageData> currentPages = pagesLiveData.getValue();
        if (currentPages != null) {
            for (int i = 0; i < currentPages.size(); i++) {
                if (currentPages.get(i).scrapbookPage.getId().equals(pageId)) {
                    List<ScrapbookItem> items = currentPages.get(i).scrapbookItems;
                    if (items == null) {
                        items = new ArrayList<>();
                        currentPages.get(i).scrapbookItems = items;
                    }
                    items.add(newItem);
                    pagesLiveData.setValue(new ArrayList<>(currentPages));
                    return;
                }
            }
        }
    }

    public void saveScrapbookItem(String pageId, String photoUrl, String userId,
                                   float x, float y, float width, float height,
                                   float rotation, float scale, float zIndex, String caption,
                                   @Nullable List<List<Double>> faceEmbeddings) {
        if (groupId == null || groupId.isEmpty() || pageId == null || pageId.isEmpty()) {
            itemSaveError.setValue("Invalid page or group ID");
            return;
        }

        isSavingItem.setValue(true);
        itemSaveError.setValue(null);

        ItemContent itemContent = new ItemContent(photoUrl, caption);
        Layout layout = new Layout(x, y, width, height, rotation, scale, zIndex);
        ScrapbookItem scrapbookItem = new ScrapbookItem("photo", userId, itemContent, layout, faceEmbeddings);

        scrapbookRepository.addItemWithFile(groupId, pageId, photoUrl, scrapbookItem, faceEmbeddings, new RepositoryCallback<ScrapbookItem>() {
            @Override
            public void onSuccess(ScrapbookItem savedItem) {
                isSavingItem.setValue(false);
                updatePageWithNewItem(pageId, savedItem);
            }

            @Override
            public void onError(Exception e) {
                isSavingItem.setValue(false);
                itemSaveError.setValue("Failed to save item: " + e.getMessage());
            }
        });
    }

    public void saveCurrentPageToStorage(Context context) {
        List<ScrapbookPageData> pages = pagesLiveData.getValue();
        if (pages == null || pageIndex < 0 || pageIndex >= pages.size()) {
            exportStatus.setValue("No page to save");
            return;
        }

        isExporting.setValue(true);
        exportStatus.setValue("Preparing page...");

        new Thread(() -> {
            try {
                // We need to rebuild the current page bitmap at high quality
                List<ScrapbookPageData> singlePageData = new ArrayList<>();
                singlePageData.add(pages.get(pageIndex));
                
                PageResources resources = PageBuilder.buildPages(context, singlePageData);
                if (resources.pageBitmaps != null && !resources.pageBitmaps.isEmpty()) {
                    Bitmap pageBitmap = resources.pageBitmaps.get(0);
                    saveBitmapToGallery(context, pageBitmap, "Scrapbook_" + System.currentTimeMillis());
                    
                    mainHandler.post(() -> {
                        isExporting.setValue(false);
                        exportStatus.setValue("Page saved to gallery!");
                    });
                } else {
                    mainHandler.post(() -> {
                        isExporting.setValue(false);
                        exportStatus.setValue("Failed to generate page image");
                    });
                }
            } catch (Exception e) {
                Log.e("ScrapbookViewModel", "Error saving page", e);
                mainHandler.post(() -> {
                    isExporting.setValue(false);
                    exportStatus.setValue("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private void saveBitmapToGallery(Context context, Bitmap bitmap, String filename) throws IOException {
        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename + ".png");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Scrapbook");
            Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = context.getContentResolver().openOutputStream(imageUri);
        } else {
            // Older versions would need WRITE_EXTERNAL_STORAGE, but we target modern Android
            throw new IOException("Unsupported Android version for this implementation");
        }

        if (fos != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        }
    }
}
