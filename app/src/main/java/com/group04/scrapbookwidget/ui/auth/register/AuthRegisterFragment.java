package com.group04.scrapbookwidget.ui.auth.register;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

        viewModel.getOtpInfoMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getOtpPromptEmail().observe(getViewLifecycleOwner(), email -> {
            if (email != null && !email.isEmpty()) {
                showOtpDialog(email);
                viewModel.onOtpPromptShown();
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

    private void showOtpDialog(@NonNull String email) {
        if (!isAdded()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_otp_input, null, false);
        EditText[] digitFields = new EditText[] {
                dialogView.findViewById(R.id.otpDigit1),
                dialogView.findViewById(R.id.otpDigit2),
                dialogView.findViewById(R.id.otpDigit3),
                dialogView.findViewById(R.id.otpDigit4),
                dialogView.findViewById(R.id.otpDigit5),
                dialogView.findViewById(R.id.otpDigit6)
        };

        ((android.widget.TextView) dialogView.findViewById(R.id.tvOtpMessage))
                .setText("Check " + email + " for the verification code.");

        setupOtpInputs(digitFields);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Verify", (dialogInterface, which) ->
                        viewModel.submitOtpCode(buildOtpCode(digitFields)))
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(ignored -> digitFields[0].requestFocus());
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void setupOtpInputs(@NonNull EditText[] digitFields) {
        for (int i = 0; i < digitFields.length; i++) {
            EditText current = digitFields[i];
            EditText next = i < digitFields.length - 1 ? digitFields[i + 1] : null;
            EditText previous = i > 0 ? digitFields[i - 1] : null;

            current.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s != null && s.length() == 1 && next != null) {
                        next.requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            current.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && current.getText() != null
                        && current.getText().length() == 0
                        && previous != null) {
                    previous.requestFocus();
                    previous.setSelection(previous.getText() != null ? previous.getText().length() : 0);
                    return true;
                }
                return false;
            });
        }
    }

    @NonNull
    private String buildOtpCode(@NonNull EditText[] digitFields) {
        StringBuilder builder = new StringBuilder();
        for (EditText digitField : digitFields) {
            CharSequence value = digitField.getText();
            if (value != null) {
                builder.append(value.toString().trim());
            }
        }
        return builder.toString();
    }
}
