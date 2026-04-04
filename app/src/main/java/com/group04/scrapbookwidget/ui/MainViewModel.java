package com.group04.scrapbookwidget.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.model.JoinByLinkRequest;
import com.group04.scrapbookwidget.data.model.JoinByLinkResponse;
import com.group04.scrapbookwidget.data.service.GroupService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class MainViewModel extends ViewModel {

    private final GroupService groupService;

    private final MutableLiveData<Boolean> isJoiningInvite = new MutableLiveData<>(false);
    private final MutableLiveData<JoinByLinkResponse> joinInviteSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> joinInviteError = new MutableLiveData<>();

    @Inject
    public MainViewModel(GroupService groupService) {
        this.groupService = groupService;
    }

    public LiveData<Boolean> getIsJoiningInvite() {
        return isJoiningInvite;
    }

    public LiveData<JoinByLinkResponse> getJoinInviteSuccess() {
        return joinInviteSuccess;
    }

    public LiveData<String> getJoinInviteError() {
        return joinInviteError;
    }

    public void joinGroupByInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            joinInviteError.setValue("Invalid or Expired Link");
            return;
        }

        isJoiningInvite.setValue(true);
        joinInviteError.setValue(null);

        groupService.joinByLink(new JoinByLinkRequest(inviteCode.trim())).enqueue(new Callback<JoinByLinkResponse>() {
            @Override
            public void onResponse(Call<JoinByLinkResponse> call, Response<JoinByLinkResponse> response) {
                isJoiningInvite.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    joinInviteSuccess.setValue(response.body());
                } else {
                    joinInviteError.setValue("Invalid or Expired Link");
                }
            }

            @Override
            public void onFailure(Call<JoinByLinkResponse> call, Throwable t) {
                isJoiningInvite.setValue(false);
                joinInviteError.setValue("Invalid or Expired Link");
            }
        });
    }

    public void clearJoinInviteSuccess() {
        joinInviteSuccess.setValue(null);
    }

    public void clearJoinInviteError() {
        joinInviteError.setValue(null);
    }
}
