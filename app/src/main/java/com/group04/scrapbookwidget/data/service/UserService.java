package com.group04.scrapbookwidget.data.service;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface UserService {
    @POST("auth/login")
    Call<User> login(@Body User user);

    @POST("auth/register")
    Call<User> register(@Body User user);

    @POST("auth/session")
    Call<User> verifySession(@Body User user);

    @POST("auth/signout")
    Call<Void> logout();

    @DELETE("auth/account")
    Call<Void> deleteAccount();

    @GET("users")
    Call<List<User>> getUsers();

    @GET("users/{userId}")
    Call<User> getUserById(@Path("userId") String userId);

    @POST("users")
    Call<User> createUser(@Body User user);

    @PATCH("users/{userId}")
    Call<User> updateUser(@Path("userId") String userId, @Body User user);

    @GET("users/{userId}/groups")
    Call<List<Group>> getUserGroups(@Path("userId") String userId);
}
