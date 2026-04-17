package com.group04.scrapbookwidget.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group04.scrapbookwidget.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TMP_PREF_NAME = "TMP_USER_SESSION";
    private static final String KEY_PENDING_INVITE_CODE = "PENDING_INVITE_CODE";

    private MainViewModel mainViewModel;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Only apply bottom padding for system bars when keyboard is NOT showing
            // When keyboard (IME) is showing, we let adjustResize handle the layout shift
            // but we need to subtract the navigation bar height to avoid double padding
            int bottomPadding = systemBars.bottom;
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                bottomPadding = 0;
            }
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
            return insets;
        });

        new WindowInsetsControllerCompat(getWindow(),
                getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setupInviteObservers();
        checkAuthAndNavigate();
        handleInviteIntent(getIntent());
    }

    private void checkAuthAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;
        NavController navController = navHostFragment.getNavController();

        if (currentUser == null) {
            // Chưa đăng nhập: set graph (mặc định vào authLoginFragment do startDestination)
            navController.setGraph(R.navigation.app_nav);
        } else {
            // Đã đăng nhập: sync session và ép buộc điều hướng tới Home
            syncUserSession(currentUser.getUid());
            navController.setGraph(R.navigation.app_nav);
            navController.navigate(R.id.homeFragment, getIntent().getExtras());
        }
    }

    private void setupInviteObservers() {
        mainViewModel.getIsJoiningInvite().observe(this, isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                showInviteLoading();
            } else {
                hideInviteLoading();
            }
        });

        mainViewModel.getJoinInviteSuccess().observe(this, result -> {
            if (result == null) {
                return;
            }

            clearPendingInviteCode();
            navigateToJoinedGroup(result);
            mainViewModel.clearJoinInviteSuccess();
        });

        mainViewModel.getJoinInviteError().observe(this, error -> {
            if (error == null) {
                return;
            }

            clearPendingInviteCode();
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            mainViewModel.clearJoinInviteError();
        });
    }

    private void handleInviteIntent(Intent intent) {
        String inviteCode = extractInviteCode(intent);
        if (inviteCode == null || inviteCode.isEmpty()) {
            inviteCode = getPendingInviteCode();
        }

        if (inviteCode == null || inviteCode.isEmpty()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            savePendingInviteCode(inviteCode);
            Toast.makeText(this, R.string.invite_link_login_required, Toast.LENGTH_SHORT).show();
            clearInviteDataFromIntent();
            return;
        }

        clearInviteDataFromIntent();
        savePendingInviteCode(inviteCode);
        mainViewModel.joinGroupByInviteCode(inviteCode);
    }

    private String extractInviteCode(Intent intent) {
        if (intent == null) {
            return null;
        }

        Uri data = intent.getData();
        if (data == null) {
            return null;
        }

        if (!"scrapbook".equals(data.getScheme()) || !"invite".equals(data.getHost())) {
            return null;
        }

        return data.getQueryParameter("code");
    }

    private void navigateToJoinedGroup(@NonNull com.group04.scrapbookwidget.data.model.JoinByLinkResponse result) {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }

        Bundle args = new Bundle();
        args.putString("GROUP_ID", result.getGroupId());
        args.putString("GROUP_NAME", result.getGroupName());
        navHostFragment.getNavController().navigate(R.id.chatDetailFragment, args);
    }

    private void showInviteLoading() {
        if (loadingDialog == null) {
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.HORIZONTAL);
            int padding = (int) (24 * getResources().getDisplayMetrics().density);
            container.setPadding(padding, padding, padding, padding);

            ProgressBar progressBar = new ProgressBar(this);
            container.addView(progressBar);

            TextView messageView = new TextView(this);
            messageView.setText(R.string.joining_group);
            messageView.setPadding(padding / 2, 0, 0, 0);
            container.addView(messageView);

            loadingDialog = new AlertDialog.Builder(this)
                    .setView(container)
                    .setCancelable(false)
                    .create();
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    private void hideInviteLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void savePendingInviteCode(String inviteCode) {
        getSharedPreferences(TMP_PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PENDING_INVITE_CODE, inviteCode)
                .apply();
    }

    private String getPendingInviteCode() {
        return getSharedPreferences(TMP_PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PENDING_INVITE_CODE, "");
    }

    private void clearPendingInviteCode() {
        getSharedPreferences(TMP_PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PENDING_INVITE_CODE)
                .apply();
    }

    private void clearInviteDataFromIntent() {
        Intent currentIntent = getIntent();
        if (currentIntent == null) {
            return;
        }

        Intent sanitizedIntent = new Intent(currentIntent);
        sanitizedIntent.setData(null);
        setIntent(sanitizedIntent);
    }

    private void syncUserSession(String userId) {
        SharedPreferences preferences = getSharedPreferences(TMP_PREF_NAME, Activity.MODE_PRIVATE);
        String currentUser = preferences.getString("USER_ID", "");
        if (currentUser.isEmpty()) {
            AppWidget.updateWidgetNow(this);
        }
        preferences.edit().putString("USER_ID", userId).apply();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkAuthAndNavigate();
        handleInviteIntent(intent);
    }
}
