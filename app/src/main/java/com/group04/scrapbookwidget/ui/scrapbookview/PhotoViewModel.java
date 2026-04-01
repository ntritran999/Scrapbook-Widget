package com.group04.scrapbookwidget.ui.scrapbookview;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.model.Reaction;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.repository.IScrapbookRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PhotoViewModel extends ViewModel {
    private IScrapbookRepository scrapbookRepository;
    private MutableLiveData<ScrapbookItem> itemLiveData = new MutableLiveData<>();
    private MutableLiveData<Integer> originalReactionCount = new MutableLiveData<>();
    private MutableLiveData<Integer> reactionCountLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> isCheckedLiveData = new MutableLiveData<>();

    public LiveData<ScrapbookItem> getItemLiveData() {
        return itemLiveData;
    }

    public LiveData<Integer> getReactionCountLiveData() {
        return reactionCountLiveData;
    }

    public LiveData<Integer> getOriginalReactionCountLiveData() {
        return originalReactionCount;
    }
    @Inject
    public PhotoViewModel(IScrapbookRepository scrapbookRepository) {
        this.scrapbookRepository = scrapbookRepository;
    }

    public void loadItem(String groupId, String pageId, String itemId) {
        scrapbookRepository.getItem(groupId, pageId, itemId, new RepositoryCallback<ScrapbookItem>() {
            @Override
            public void onSuccess(ScrapbookItem item) {
                itemLiveData.setValue(item);
            }

            @Override
            public void onError(Exception e) {
                Log.e("load-item-error", e.getMessage());
            }
        });
    }

    public void loadReactions(String groupId, String pageId, String itemId, String userId) {
        scrapbookRepository.getReactions(groupId, pageId, itemId, new RepositoryCallback<List<Reaction>>() {
            @Override
            public void onSuccess(List<Reaction> result) {
                originalReactionCount.setValue(result.size());
                reactionCountLiveData.setValue(result.size());
                for (var reaction: result) {
                    if (reaction.getUserId().equals(userId)) {
                        isCheckedLiveData.postValue(true);
                        break;
                    }
                }
            }

            @Override
            public void onError(Exception exception) {
                Log.e("load-reaction-error", exception.getMessage());
            }
        });
    }

    public void addReaction(String groupId, String pageId, String itemId, Reaction reaction) {
        scrapbookRepository.addReaction(groupId, pageId, itemId, reaction, new RepositoryCallback<Reaction>() {
            @Override
            public void onSuccess(Reaction result) {

            }

            @Override
            public void onError(Exception exception) {
                Log.e("add-reaction-error", exception.getMessage());
            }
        });
    }

    public void removeReaction(String groupId, String pageId, String itemId, Reaction reaction) {
        scrapbookRepository.removeReaction(groupId, pageId, itemId, reaction.getUserId(), new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {

            }

            @Override
            public void onError(Exception exception) {
                Log.e("remove-reaction-error", exception.getMessage());
            }
        });
    }

    public void isCheckedChanged() {
        boolean isChecked = Boolean.TRUE.equals(isCheckedLiveData.getValue());
        isCheckedLiveData.setValue(!isChecked);
        Integer count = reactionCountLiveData.getValue();
        if (count != null) {
            if (isChecked) {
                reactionCountLiveData.setValue(count - 1);
            }
            else {
                reactionCountLiveData.setValue(count + 1);
            }
        }
    }
}
