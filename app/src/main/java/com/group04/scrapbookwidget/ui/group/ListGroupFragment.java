package com.group04.scrapbookwidget.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.databinding.FragmentListGroupBinding;
import com.group04.scrapbookwidget.ui.CompactGroupListViewModel;
import com.group04.scrapbookwidget.ui.adapter.ChatGroupAdapter;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ListGroupFragment extends Fragment {

    private FragmentListGroupBinding binding;
    private CompactGroupListViewModel viewModel;
    private ChatGroupAdapter adapter;

    @Inject
    FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListGroupBinding.inflate(inflater, container, false);
        // Changed to requireActivity() to share the ViewModel across fragments
        viewModel = new ViewModelProvider(requireActivity()).get(CompactGroupListViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        adapter = new ChatGroupAdapter(new ArrayList<>(), currentUserId, group -> {
            Bundle args = new Bundle();
            args.putString("GROUP_ID", group.getId());
            args.putString("GROUP_NAME", group.getGroupName());
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.chatDetailFragment, args);
        });

        binding.rvGroupList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvGroupList.setAdapter(adapter);

        viewModel.getGroupsLiveData().observe(getViewLifecycleOwner(), groups -> {
            if (groups != null) {
                adapter.setGroups(groups);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        if (auth.getCurrentUser() != null) {
            viewModel.loadGroupList(auth.getCurrentUser().getUid());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
