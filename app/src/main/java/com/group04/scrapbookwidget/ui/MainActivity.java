package com.group04.scrapbookwidget.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group04.scrapbookwidget.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private final String TMP_PREF_NAME = "TMP_USER_SESSION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new WindowInsetsControllerCompat(getWindow(),
                getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        checkAuthAndNavigate();
    }

    private void checkAuthAndNavigate() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
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
    }
}
