package com.group04.scrapbookwidget.data.service;

import com.group04.scrapbookwidget.data.model.Widget;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface WidgetService {
    @GET("users/{userId}/widgets")
    Call<List<Widget>> getWidgets(@Path("userId") String userId);
}
