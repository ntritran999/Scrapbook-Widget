package com.group04.scrapbookwidget.data.repository;

import com.group04.scrapbookwidget.data.service.BackgroundImageService;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackgroundRepository implements IBackgroundRepository {
    private BackgroundImageService service;
    @Inject
    public BackgroundRepository(BackgroundImageService service) {
        this.service = service;
    }
    @Override
    public void getBackground(RepositoryCallback<List<String>> callback) {
        service.getBackgrounds().enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                }
                else {
                    callback.onError(new Exception("Failed to load urls: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                callback.onError(new Exception(t));
            }
        });
    }
}
