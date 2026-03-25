package com.group04.scrapbookwidget.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentTopBarBinding;
import com.group04.scrapbookwidget.ui.adapter.CompactGroupListAdapter;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TopBarFragment extends Fragment {
    private CompactGroupListViewModel groupListViewModel;
    private FragmentTopBarBinding binding;

    public TopBarFragment() {

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        groupListViewModel = new ViewModelProvider(requireParentFragment()).get(CompactGroupListViewModel.class);
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
                        NavController navController = navHostFragment.getNavController();
                        view.post(() -> {
                            navController.navigate(R.id.scrapbookViewFragment, args);
                        });
                    }
                });
            }
            else {
                Toast.makeText(getContext(), "Cannot load group list.", Toast.LENGTH_SHORT).show();
            }
        });

        groupListViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), "Cannot load group list.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_top_bar, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}