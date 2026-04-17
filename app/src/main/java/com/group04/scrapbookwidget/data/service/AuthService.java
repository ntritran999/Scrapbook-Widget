package com.group04.scrapbookwidget.data.service;

import com.group04.scrapbookwidget.data.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("auth/google")
    Call<User> loginWithGoogle(@Body User userRequest);
}
