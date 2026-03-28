package com.group04.scrapbookwidget.ui.auth.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.group04.scrapbookwidget.databinding.FragmentAuthLoginBinding;
import com.group04.scrapbookwidget.ui.MainActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AuthLoginFragment extends Fragment {

    private FragmentAuthLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        binding = FragmentAuthLoginBinding.inflate(inflater, container, false);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Bind ViewModel
        binding.setViewModel(viewModel);

        // IMPORTANT: Required for LiveData + DataBinding
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupObservers();

        return binding.getRoot();
    }

    private void setupObservers() {
        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Save user session
                saveUserSession(user.getId());
                
                // Navigate to home
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getNavigateToRegister().observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (Boolean.TRUE.equals(shouldNavigate)) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_authLoginFragment_to_authRegisterFragment);
                viewModel.onNavigatedToRegister();
            }
        });
    }

    private void saveUserSession(String userId) {
        SharedPreferences preferences = requireContext().getSharedPreferences("TMP_USER_SESSION", Context.MODE_PRIVATE);
        preferences.edit().putString("USER_ID", userId).apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leak
    }
}
