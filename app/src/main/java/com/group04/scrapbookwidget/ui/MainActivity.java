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

import com.group04.scrapbookwidget.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private final String PREF_NAME = "widget_metadata";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo nhanh SharedPreferences với tên file là TMP_USER_SESSION
        // SharedPreferences sharedPref = getSharedPreferences("TMP_USER_SESSION", Context.MODE_PRIVATE);

        // Mở bộ chỉnh sửa và ghi đè USER_ID
        // SharedPreferences.Editor editor = sharedPref.edit();
        // editor.putString("USER_ID", "test_user1");
        // editor.apply(); // Dùng apply() để lưu ngầm dưới background, tránh đơ UI
        // ------------------------------------------------------------
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new WindowInsetsControllerCompat(getWindow(),
                getWindow().getDecorView()).setAppearanceLightStatusBars(false);

        saveDummyWidgetMetadata();
        saveDummyUserSession();

        navigateToHomeFromWidget();

//        removeDummyWidgetMetadata();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        navigateToHomeFromWidget();
    }

    private void navigateToHomeFromWidget() {
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            navController.setGraph(R.navigation.app_nav, getIntent().getExtras());
        }
    }

    private void saveDummyWidgetMetadata() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("USER_ID", "test_user1");
        editor.commit();
    }

    private void saveDummyUserSession() {
        SharedPreferences preferences = getSharedPreferences("TMP_USER_SESSION", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("USER_ID", "test_user1");
        editor.commit();
    }

    private void removeDummyUserSession() {
        SharedPreferences preferences = getSharedPreferences("TMP_USER_SESSION", Activity.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    private void removeDummyWidgetMetadata() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }
}