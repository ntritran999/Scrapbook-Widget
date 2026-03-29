package com.group04.scrapbookwidget.data.service;

import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface GroupService {
    @GET("groups/{groupId}/scrapbook-pages")
    Call<List<ScrapbookPage>> getScrapbookPages(@Path("groupId") String groupId);

    @GET("groups/{groupId}/scrapbook-pages/{pageId}/items")
    Call<List<ScrapbookItem>> getItems(@Path("groupId") String groupId, @Path("pageId") String pageId);

    @GET("groups/{groupId}/scrapbook-pages/{pageId}/{itemId}")
    Call<ScrapbookItem> getItem(@Path("groupId") String groupId, @Path("pageId") String pageId, @Path("itemId") String itemId);

    @POST("groups/{groupId}/scrapbook-pages/{pageId}/items")
    Call<ScrapbookItem> createItem(@Path("groupId") String groupId, @Path("pageId") String pageId, @Body ScrapbookItem item);

    @Multipart
    @POST("groups/{groupId}/scrapbook-pages/{pageId}/items")
    Call<ScrapbookItem> createItemWithFile(@Path("groupId") String groupId, @Path("pageId") String pageId, @Part MultipartBody.Part file, @Part("payload") RequestBody item);
}
