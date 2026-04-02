package com.group04.scrapbookwidget.data.service;

import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.data.model.Invitation;
import com.group04.scrapbookwidget.data.model.LeaveGroupResponse;
import com.group04.scrapbookwidget.data.model.Message;
import com.group04.scrapbookwidget.data.model.Reaction;
import com.group04.scrapbookwidget.data.model.ScrapbookItem;
import com.group04.scrapbookwidget.data.model.ScrapbookPage;
import com.group04.scrapbookwidget.data.model.TodayMemory;
import com.group04.scrapbookwidget.data.model.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface GroupService {
    @GET("groups")
    Call<List<Group>> getGroups();

    @GET("groups/{groupId}")
    Call<Group> getGroupById(@Path("groupId") String groupId);

    @POST("groups")
    Call<Group> createGroup(@Body Map<String, Object> body);

    @PATCH("groups/{groupId}/name")
    Call<Group> updateGroupName(@Path("groupId") String groupId, @Body Map<String, Object> body);

    @PATCH("groups/{groupId}/avatar")
    Call<Group> updateGroupAvatar(@Path("groupId") String groupId, @Body Map<String, Object> body);

    class AvatarUploadResponse {
        public String avatarUrl;
    }

    @Multipart
    @POST("groups/{groupId}/avatar")
    Call<AvatarUploadResponse> uploadGroupAvatar(@Path("groupId") String groupId, @Part MultipartBody.Part file);

    @GET("groups/{groupId}/members")
    Call<List<User>> getGroupMembers(@Path("groupId") String groupId);

    @POST("groups/{groupId}/leave")
    Call<LeaveGroupResponse> leaveGroup(@Path("groupId") String groupId);

    @DELETE("groups/{groupId}/members/{userId}")
    Call<Void> removeMember(@Path("groupId") String groupId, @Path("userId") String userId);

    @GET("users/discover")
    Call<List<User>> searchUsers(@Query("q") String keyword);

    @POST("groups/{groupId}/invitations")
    Call<Void> inviteUser(@Path("groupId") String groupId, @Body Map<String, String> body);

    @PUT("groups/{groupId}/members/{userId}")
    Call<Void> addMemberDirectly(@Path("groupId") String groupId, @Path("userId") String userId);

    @GET("groups/invitations/me")
    Call<List<Invitation>> getMyInvitations();

    @POST("groups/{groupId}/invitations/accept")
    Call<Void> acceptInvitation(@Path("groupId") String groupId);

    @POST("groups/{groupId}/invitations/decline")
    Call<Void> declineInvitation(@Path("groupId") String groupId);

    @GET("groups/{groupId}/scrapbook-pages")
    Call<List<ScrapbookPage>> getScrapbookPages(@Path("groupId") String groupId);

    @POST("groups/{groupId}/scrapbook-pages")
    Call<ScrapbookPage> createScrapbookPage(@Path("groupId") String groupId);

    @GET("groups/{groupId}/scrapbook-pages/{pageId}/items")
    Call<List<ScrapbookItem>> getItems(@Path("groupId") String groupId, @Path("pageId") String pageId);

    @GET("groups/{groupId}/scrapbook-pages/{pageId}/{itemId}")
    Call<ScrapbookItem> getItem(@Path("groupId") String groupId, @Path("pageId") String pageId, @Path("itemId") String itemId);

    @POST("groups/{groupId}/scrapbook-pages/{pageId}/items")
    Call<ScrapbookItem> createItem(@Path("groupId") String groupId, @Path("pageId") String pageId, @Body ScrapbookItem item);

    @Multipart
    @POST("groups/{groupId}/scrapbook-pages/{pageId}/items")
    Call<ScrapbookItem> createItemWithFile(@Path("groupId") String groupId, @Path("pageId") String pageId, @Part MultipartBody.Part file, @Part("payload") RequestBody item);

    @GET("groups/{groupId}/messages")
    Call<List<Message>> getMessages(@Path("groupId") String groupId);

    @GET("groups/{groupId}/today-memory")
    Call<List<TodayMemory>> getTodayMemory(@Path("groupId") String groupId);

    @POST("groups/{groupId}/messages")
    Call<Message> sendMessage(@Path("groupId") String groupId, @Body Map<String, String> body);

    @PUT("groups/{groupId}/messages/{messageId}/seen-by/{userId}")
    Call<Message.SeenBy> markAsSeen(@Path("groupId") String groupId, @Path("messageId") String messageId, @Path("userId") String userId);

    @Streaming
    @GET("groups/{groupId}/messages/stream")
    Call<ResponseBody> streamMessages(@Path("groupId") String groupId, @Header("Authorization") String bearerToken);

    @GET("groups/{groupId}/scrapbook-pages/{pageId}/{itemId}/reactions")
    Call<List<Reaction>> getReactions(@Path("groupId") String groupId, @Path("pageId") String pageId, @Path("itemId") String itemId);

    @POST("groups/{groupId}/scrapbook-pages/{pageId}/{itemId}/reactions")
    Call<Reaction> addReaction(@Path("groupId") String groupId, @Path("pageId") String pageId, @Path("itemId") String itemId, @Body Reaction reaction);

    @DELETE("groups/{groupId}/scrapbook-pages/{pageId}/{itemId}/{userId}")
    Call<Boolean> removeReaction(@Path("groupId") String groupId, @Path("pageId") String pageId, @Path("itemId") String itemId, @Path("userId") String userId);
}
