package com.group04.scrapbookwidget.ui.auth.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.group04.scrapbookwidget.databinding.FragmentAuthRegisterBinding;

public class AuthRegisterFragment extends Fragment {

    private FragmentAuthRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        binding = FragmentAuthRegisterBinding.inflate(inflater, container, false);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this)
                .get(RegisterViewModel.class);

        // Bind ViewModel
        binding.setViewModel(viewModel);

        // IMPORTANT: Required for LiveData + DataBinding
        binding.setLifecycleOwner(getViewLifecycleOwner());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leak
    }
}
