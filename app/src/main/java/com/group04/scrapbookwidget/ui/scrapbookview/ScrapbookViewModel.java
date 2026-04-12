package com.group04.scrapbookwidget.ui.scrapbookview;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
    private final MutableLiveData<Boolean> isPageCurlEffectEnabled = new MutableLiveData<>();
    
    private final MutableLiveData<Boolean> isRendering = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> getIsRendering() { return isRendering; }
    
    // Debounce mechanism for reload
    private Runnable pendingReloadRunnable = null;
    private static final long RELOAD_DEBOUNCE_MS = 300;
    private static final long RELOAD_DELAY_MS = 100;  // Reduced from 500ms for faster feedback
    
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
    //
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

    public LiveData<Boolean> getIsPageCurlEffectEnabled() {
        return isPageCurlEffectEnabled;
    }

    public void togglePageCurlEffect(boolean isEnabled) {
        isPageCurlEffectEnabled.setValue(isEnabled);
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

    /**
     * Updates the page data with a newly saved item immediately for instant feedback.
     * This adds the saved item to the existing items list for the specified page.
     * 
     * @param pageId The ID of the page
     * @param newItem The newly saved ScrapbookItem
     */
    private void updatePageWithNewItem(String pageId, ScrapbookItem newItem) {
        if (newItem == null || pageId == null) {
            android.util.Log.w("ScrapbookViewModel", "updatePageWithNewItem: Invalid parameters, newItem=" + newItem + ", pageId=" + pageId);
            return;
        }

        android.util.Log.d("ScrapbookViewModel", "updatePageWithNewItem: Adding item " + newItem.getId() + " to page " + pageId);

        List<ScrapbookPageData> currentPages = pagesLiveData.getValue();
        if (currentPages != null) {
            for (int i = 0; i < currentPages.size(); i++) {
                if (currentPages.get(i).scrapbookPage.getId().equals(pageId)) {
                    List<ScrapbookItem> items = currentPages.get(i).scrapbookItems;
                    if (items == null) {
                        items = new ArrayList<>();
                        currentPages.get(i).scrapbookItems = items;
                    }
                    
                    // Add the new item to the list
                    items.add(newItem);
                    android.util.Log.d("ScrapbookViewModel", "updatePageWithNewItem: Added item, page now has " + items.size() + " items");
                    
                    // Update LiveData with new reference to trigger observers
                    List<ScrapbookPageData> updatedPages = new ArrayList<>(currentPages);
                    android.util.Log.d("ScrapbookViewModel", "updatePageWithNewItem: Updating pages via LiveData");
                    pagesLiveData.setValue(updatedPages);
                    return;
                }
            }
            android.util.Log.w("ScrapbookViewModel", "updatePageWithNewItem: Page not found for pageId: " + pageId);
        } else {
            android.util.Log.e("ScrapbookViewModel", "updatePageWithNewItem: currentPages is null");
        }
    }

    /**
     * Reloads items for the specified page with debounce and delay to prevent ANR.
     * Uses debouncing to avoid frequent re-renders when multiple paste operations happen quickly.
     * Delays reload briefly to allow UI to settle before rendering new items.
     *
     * @param pageId The ID of the page to reload items for
     */
    private void reloadPageItems(String pageId) {
        if (groupId == null || groupId.isEmpty() || pageId == null || pageId.isEmpty()) {
            android.util.Log.e("ScrapbookViewModel", "reloadPageItems: Invalid groupId or pageId");
            return;
        }

        android.util.Log.d("ScrapbookViewModel", "reloadPageItems: Starting reload for pageId: " + pageId);

        // Remove any pending reload to implement debounce
        if (pendingReloadRunnable != null) {
            mainHandler.removeCallbacks(pendingReloadRunnable);
            android.util.Log.d("ScrapbookViewModel", "reloadPageItems: Cancelled previous pending reload");
        }

        // Create the debounced reload task
        pendingReloadRunnable = () -> {
            android.util.Log.d("ScrapbookViewModel", "reloadPageItems: Executing reload task for pageId: " + pageId);
            scrapbookRepository.getAllItems(groupId, pageId, new RepositoryCallback<List<ScrapbookItem>>() {
                @Override
                public void onSuccess(List<ScrapbookItem> items) {
                    android.util.Log.d("ScrapbookViewModel", "reloadPageItems: onSuccess - retrieved " + (items != null ? items.size() : 0) + " items");
                    if (items != null && !items.isEmpty()) {
                        for (ScrapbookItem item : items) {
                            android.util.Log.d("ScrapbookViewModel", "  - Item: id=" + item.getId() + 
                                    ", photoUrl=" + (item.getContent() != null ? item.getContent().photoUrl : "null"));
                        }
                    } else {
                        android.util.Log.w("ScrapbookViewModel", "reloadPageItems: Items list is null or empty!");
                    }
                    
                    // Update the current pages data with the reloaded items
                    List<ScrapbookPageData> currentPages = pagesLiveData.getValue();
                    if (currentPages != null) {
                        boolean found = false;
                        for (int i = 0; i < currentPages.size(); i++) {
                            if (currentPages.get(i).scrapbookPage.getId().equals(pageId)) {
                                android.util.Log.d("ScrapbookViewModel", "reloadPageItems: Updating page at index " + i + 
                                        " with " + (items != null ? items.size() : 0) + " items");
                                currentPages.get(i).scrapbookItems = items;
                                found = true;
                                break;
                            }
                        }
                        
                        if (!found) {
                            android.util.Log.e("ScrapbookViewModel", "reloadPageItems: Page not found for pageId: " + pageId);
                        }
                        
                        // Create new list to ensure LiveData observers are triggered
                        List<ScrapbookPageData> updatedPages = new ArrayList<>(currentPages);
                        android.util.Log.d("ScrapbookViewModel", "reloadPageItems: Setting new pages via LiveData, total pages: " + updatedPages.size());
                        pagesLiveData.setValue(updatedPages);
                    } else {
                        android.util.Log.e("ScrapbookViewModel", "reloadPageItems: currentPages is null, cannot update");
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Log error but don't fail the user experience
                    android.util.Log.e("ScrapbookViewModel", "reloadPageItems: onError - " + e.getMessage());
                    e.printStackTrace();
                    errorMessage.setValue("Failed to refresh items: " + e.getMessage());
                }
            });

            pendingReloadRunnable = null;
        };

        // Post with delay to avoid sudden CPU spike from bitmap rendering
        android.util.Log.d("ScrapbookViewModel", "reloadPageItems: Scheduling reload with " + RELOAD_DELAY_MS + "ms delay");
        mainHandler.postDelayed(pendingReloadRunnable, RELOAD_DELAY_MS);
    }

    /**
     * Saves a pasted scrapbook item to the database via API.
     * This method creates a new ScrapbookItem with the provided data and sends it to the server.
     * After successful save, it reloads the page items to display the new image.
     *
     * @param pageId The ID of the page where the item will be saved
     * @param photoUrl The URL of the cached photo
     * @param userId The ID of the user creating the item
     * @param x X coordinate of the item
     * @param y Y coordinate of the item
     * @param width Width of the item
     * @param height Height of the item
     * @param rotation Rotation angle of the item
     * @param scale Scale factor of the item
     * @param zIndex Z-index (layer depth) of the item
     */
    public void saveScrapbookItem(String pageId, String photoUrl, String userId,
                                   float x, float y, float width, float height,
                                   float rotation, float scale, float zIndex, String caption,
                                   @Nullable List<List<Double>> faceEmbeddings) {
        if (groupId == null || groupId.isEmpty() || pageId == null || pageId.isEmpty()) {
            itemSaveError.setValue("Invalid page or group ID");
            android.util.Log.e("ScrapbookViewModel", "saveScrapbookItem: Invalid page or group ID. groupId=" + groupId + ", pageId=" + pageId);
            return;
        }

        android.util.Log.d("ScrapbookViewModel", "saveScrapbookItem: Starting save");
        android.util.Log.d("ScrapbookViewModel", "  pageId=" + pageId + ", photoUrl=" + photoUrl + ", userId=" + userId);
        android.util.Log.d("ScrapbookViewModel", "  x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
        android.util.Log.d("ScrapbookViewModel", "  rotation=" + rotation + ", scale=" + scale + ", zIndex=" + zIndex);
        android.util.Log.d("ScrapbookViewModel", "  caption=" + caption);
        android.util.Log.d("ScrapbookViewModel", "  faceEmbeddingsCount=" + (faceEmbeddings != null ? faceEmbeddings.size() : 0));

        isSavingItem.setValue(true);
        itemSaveError.setValue(null);

        // Create the item with provided data and caption
        ItemContent itemContent = new ItemContent(photoUrl, caption);
        Layout layout = new Layout(x, y, width, height, rotation, scale, zIndex);
        ScrapbookItem scrapbookItem = new ScrapbookItem("photo", userId, itemContent, layout, faceEmbeddings);

        android.util.Log.d("ScrapbookViewModel", "saveScrapbookItem: Calling repository.addItemWithFile");
        scrapbookRepository.addItemWithFile(groupId, pageId, photoUrl, scrapbookItem, faceEmbeddings, new RepositoryCallback<ScrapbookItem>() {
            @Override
            public void onSuccess(ScrapbookItem savedItem) {
                android.util.Log.d("ScrapbookViewModel", "saveScrapbookItem: Repository callback onSuccess");
                android.util.Log.d("ScrapbookViewModel", "  savedItem.id=" + (savedItem != null ? savedItem.getId() : "null"));
                
                isSavingItem.setValue(false);
                itemSaveError.setValue(null);
                
                // Update the pages data with the newly saved item immediately
                // This combines the saved item with existing items on the page
                updatePageWithNewItem(pageId, savedItem);
            }

            @Override
            public void onError(Exception e) {
                android.util.Log.e("ScrapbookViewModel", "saveScrapbookItem: Repository callback onError: " + e.getMessage());
                e.printStackTrace();
                
                isSavingItem.setValue(false);
                itemSaveError.setValue("Failed to save item: " + e.getMessage());
            }
        });
    }
}
