package com.group04.scrapbookwidget.data.service;

import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GroupService {
    @GET("groups/{groupId}/scrapbook-pages")
    Call<List<ScrapbookPage>> getScrapbookPages(@Path("groupId") String groupId);

    @GET("groups/{groupId}/scrapbook-pages/{pageId}/items")
    Call<List<ScrapbookItem>> getItems(@Path("groupId") String groupId, @Path("pageId") String pageId);

    @GET("groups/{groupId}/scrapbook-pages/{pageId}/{itemId}")
    Call<ScrapbookItem> getItem(@Path("groupId") String groupId, @Path("pageId") String pageId, @Path("itemId") String itemId);
}
