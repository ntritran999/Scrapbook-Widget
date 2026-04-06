package com.group04.scrapbookwidget.data.service;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface BackgroundImageService {
    @GET("backgrounds")
    Call<List<String>> getBackgrounds();
}
