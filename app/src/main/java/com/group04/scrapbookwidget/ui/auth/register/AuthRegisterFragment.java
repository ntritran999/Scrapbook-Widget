package com.group04.scrapbookwidget.ui.auth.register;

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

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentAuthRegisterBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
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
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Bind ViewModel
        binding.setViewModel(viewModel);

        // IMPORTANT: Required for LiveData + DataBinding
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupObservers();

        return binding.getRoot();
    }

    private void setupObservers() {
        viewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                // Navigate back to login
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_authRegisterFragment_to_authLoginFragment);
                viewModel.onNavigatedToLogin();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getNavigateToLogin().observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (Boolean.TRUE.equals(shouldNavigate)) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_authRegisterFragment_to_authLoginFragment);
                viewModel.onNavigatedToLogin();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leak
    }
}
