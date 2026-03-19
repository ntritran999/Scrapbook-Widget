package com.group04.scrapbookwidget.ui.scrapbookview;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.repository.IScrapbookRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PhotoViewModel extends ViewModel {
    private IScrapbookRepository scrapbookRepository;
    private MutableLiveData<ScrapbookItem> itemLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<ScrapbookItem> getItemLiveData() {
        return itemLiveData;
    }
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    @Inject
    public PhotoViewModel(IScrapbookRepository scrapbookRepository) {
        this.scrapbookRepository = scrapbookRepository;
    }

    public void loadItem(String groupId, String pageId, String itemId) {
        scrapbookRepository.getItem(groupId, pageId, itemId).addOnSuccessListener(item -> {
            itemLiveData.setValue(item);
        }).addOnFailureListener(e -> {
            errorMessage.setValue(e.getMessage());
        });
    }
}
