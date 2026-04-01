package com.group04.scrapbookwidget.ui.group;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.group04.scrapbookwidget.databinding.FragmentChatTopBarBinding;

public class ChatTopBarFragment extends Fragment {
    private FragmentChatTopBarBinding binding;
    private String title = "Messages";
    private boolean isBackButtonVisible = false;

    public void setTitle(String title) {
        this.title = title;
        if (binding != null) {
            binding.tvTitle.setText(title);
        }
    }

    public void setBackButtonVisible(boolean visible) {
        this.isBackButtonVisible = visible;
        if (binding != null) {
            binding.btnBack.setVisibility(visible ? View.VISIBLE : View.GONE);
            binding.ivGroupAvatar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatTopBarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvTitle.setText(title);
        binding.btnBack.setVisibility(isBackButtonVisible ? View.VISIBLE : View.GONE);
        binding.ivGroupAvatar.setVisibility(isBackButtonVisible ? View.VISIBLE : View.GONE);

        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(requireActivity(), com.group04.scrapbookwidget.R.id.nav_host_fragment)
                    .navigateUp();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
