package com.group04.scrapbookwidget.ui.scrapbookview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.repository.IScrapbookRepository;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ScrapbookViewModel extends ViewModel {
    private final IScrapbookRepository scrapbookRepository;

    private final MutableLiveData<List<ScrapbookPageData>> pagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    @Inject
    public ScrapbookViewModel(IScrapbookRepository repo) {
        scrapbookRepository = repo;
    }

    public LiveData<List<ScrapbookPageData>> getPagesLiveData() {
        return pagesLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadScrapbook(String groupId) {
        List<ScrapbookPageData> data = pagesLiveData.getValue();
        if (data != null && !data.isEmpty()) {
            return;
        }

        scrapbookRepository.getAllPages(groupId).addOnSuccessListener(pages -> {
            if (pages == null || pages.isEmpty()) {
                pagesLiveData.setValue(new ArrayList<>());
                return;
            }

            List<Task<List<ScrapbookItem>>> itemTasks = new ArrayList<>();
            for (var page: pages) {
                itemTasks.add(scrapbookRepository.getAllItems(groupId, page.getId()));
            }
            Tasks.whenAllSuccess(itemTasks).addOnSuccessListener(results -> {
                List<ScrapbookPageData> scrapbookData = new ArrayList<>();
                for (int i = 0; i < pages.size(); i++) {
                    scrapbookData.add(new ScrapbookPageData(pages.get(i), (List<ScrapbookItem>) results.get(i)));
                }

                pagesLiveData.setValue(scrapbookData);
            }).addOnFailureListener(e -> {
                errorMessage.setValue("Failed to load items: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            errorMessage.setValue("Failed to load pages: " + e.getMessage());
        });
    }
}
