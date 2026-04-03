package com.group04.scrapbookwidget.ui;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.group04.scrapbookwidget.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {
    private CompactGroupListViewModel groupListViewModel;
    private final String TMP_PREF_NAME = "TMP_USER_SESSION";

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (hasScrapbookNavigationArgs(args)) {
            NavHostFragment navHostFragment =
                    (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.home_nav_host_fragment);
            if (navHostFragment != null) {
                NavController navController = navHostFragment.getNavController();
                view.post(() -> navController.navigate(R.id.scrapbookViewFragment, args));
            }
        }

        String userId = getActivity()
                .getSharedPreferences(TMP_PREF_NAME, Activity.MODE_PRIVATE)
                .getString("USER_ID", "");
        groupListViewModel = new ViewModelProvider(this).get(CompactGroupListViewModel.class);
        groupListViewModel.loadGroupList(userId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    private boolean hasScrapbookNavigationArgs(@Nullable Bundle args) {
        if (args == null || args.isEmpty()) {
            return false;
        }

        return args.containsKey("GROUP_ID")
                || args.containsKey("PAGE_ID")
                || args.containsKey("PASTED_IMAGE_PATH")
                || args.containsKey("CAPTION")
                || args.containsKey("FACE_EMBEDDINGS");
    }
}
