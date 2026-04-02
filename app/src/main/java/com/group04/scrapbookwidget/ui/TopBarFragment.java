package com.group04.scrapbookwidget.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentTopBarBinding;
import com.group04.scrapbookwidget.ui.adapter.CompactGroupListAdapter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TopBarFragment extends Fragment {
    private CompactGroupListViewModel groupListViewModel;
    private SettingViewModel settingViewModel;
    private FragmentTopBarBinding binding;

    @Inject
    FirebaseAuth firebaseAuth;

    public TopBarFragment() {
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Cần requireParentFragment() vì TopBarFragment nằm trong HomeFragment
        groupListViewModel = new ViewModelProvider(requireParentFragment()).get(CompactGroupListViewModel.class);
        
        // SettingViewModel để lấy thông tin user hiện tại (avatar)
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);

        setupUserObserver();
        setupGroupObserver();
        setupClickListeners();
    }

    private void setupUserObserver() {
        settingViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.account_circle_24)
                        .circleCrop()
                        .into(binding.btnSettings);
            } else {
                binding.btnSettings.setImageResource(R.drawable.account_circle_24);
            }
        });
    }

    private void setupGroupObserver() {
        groupListViewModel.getGroupsLiveData().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null && !groups.isEmpty()) {
                int n = groups.size();
                String[] groupNames = new String[n];
                String[] groupAvatars = new String[n];
                for (int i = 0; i < n; i++) {
                    groupNames[i] = groups.get(i).getGroupName();
                    groupAvatars[i] = groups.get(i).getAvatarUrl();
                }

                binding.groupList.setAdapter(new CompactGroupListAdapter(requireContext(), R.layout.compact_group_list, groupNames, groupAvatars));
                binding.groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Bundle args = new Bundle();
                        args.putString("GROUP_ID", groups.get(position).getId());
                        args.putString("PAGE_ID", "");

                        NavHostFragment navHostFragment =
                                (NavHostFragment) getParentFragmentManager().findFragmentById(R.id.home_nav_host_fragment);
                        if (navHostFragment != null) {
                            NavController navController = navHostFragment.getNavController();
                            view.post(() -> {
                                navController.navigate(R.id.scrapbookViewFragment, args);
                            });
                        }
                    }
                });
            } else {
                // Toast.makeText(getContext(), "No groups available.", Toast.LENGTH_SHORT).show();
            }
        });

        groupListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error loading groups", Toast.LENGTH_SHORT).show();
                // logcat
                Log.e("TopBarFragment", "Error loading groups: " + error);
            }
        });
    }

    private void setupClickListeners() {
        binding.btnSettings.setOnClickListener(v -> {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.action_homeFragment_to_settingFragment);
        });

        binding.btnChat.setOnClickListener(v -> {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.action_homeFragment_to_chatFragment);
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_top_bar, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (settingViewModel != null) {
            settingViewModel.refresh();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
