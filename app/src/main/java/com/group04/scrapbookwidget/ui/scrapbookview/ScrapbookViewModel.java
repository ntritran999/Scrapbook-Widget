package com.group04.scrapbookwidget.ui.scrapbookview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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

    private int pageIndex = 0;

    private String groupId;
    private final MutableLiveData<List<ScrapbookPageData>> pagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    @Inject
    public ScrapbookViewModel(IScrapbookRepository repo) {
        scrapbookRepository = repo;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public String getGroupId() {
        return groupId;
    }
    public LiveData<List<ScrapbookPageData>> getPagesLiveData() {
        return pagesLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadScrapbook(String groupId, String pageId) {
        List<ScrapbookPageData> data = pagesLiveData.getValue();
        if (data != null && !data.isEmpty()) {
            return;
        }

        scrapbookRepository.getAllPages(groupId, new RepositoryCallback<List<ScrapbookPage>>() {
            @Override
            public void onSuccess(List<ScrapbookPage> pages) {
                if (pages == null || pages.isEmpty()) {
                    pagesLiveData.setValue(new ArrayList<>());
                    return;
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
                                ScrapbookViewModel.this.groupId = groupId;
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (hasError[0]) return;
                            hasError[0] = true;
                            errorMessage.setValue("Failed to load items: " + e.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Failed to load pages: " + e.getMessage());
            }
        });
    }
}
