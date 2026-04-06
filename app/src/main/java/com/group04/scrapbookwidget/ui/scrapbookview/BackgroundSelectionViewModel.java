package com.group04.scrapbookwidget.ui.scrapbookview;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.repository.IBackgroundRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class BackgroundSelectionViewModel extends ViewModel {
    private IBackgroundRepository backgroundRepository;
    private MutableLiveData<List<String>> imageUrlsLiveData = new MutableLiveData<>();
    @Inject
    public BackgroundSelectionViewModel(IBackgroundRepository backgroundRepository) {
        this.backgroundRepository = backgroundRepository;
    }

    public LiveData<List<String>> getImageUrlsLiveData() {
        return imageUrlsLiveData;
    }

    public void loadUrls() {
        backgroundRepository.getBackground(new RepositoryCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> result) {
                if (result == null || result.isEmpty()) {
                    imageUrlsLiveData.setValue(new ArrayList<>());
                    return;
                }
                imageUrlsLiveData.setValue(result);
            }

            @Override
            public void onError(Exception exception) {
                Log.e("background-images", "Failed to load image urls: " + exception.getMessage());
            }
        });
    }
}
