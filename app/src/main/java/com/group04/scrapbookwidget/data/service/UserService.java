package com.group04.scrapbookwidget.data.service;

import com.group04.scrapbookwidget.data.model.Group;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface UserService {
    @GET("users/{userId}/groups")
    Call<List<Group>> getUserGroups(@Path("userId") String userId);
}
