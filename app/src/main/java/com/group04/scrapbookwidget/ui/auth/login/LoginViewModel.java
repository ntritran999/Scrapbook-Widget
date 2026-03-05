package com.group04.scrapbookwidget.ui.auth.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LoginViewModel extends ViewModel {

    // =========================
    // Input Fields
    // =========================
    // Changed to public MutableLiveData to support two-way data binding
    public final MutableLiveData<String> email = new MutableLiveData<>("");
    public final MutableLiveData<String> password = new MutableLiveData<>("");

    // =========================
    // Error Fields
    // =========================
    private final MutableLiveData<String> _emailError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _passwordError = new MutableLiveData<>(null);

    public LiveData<String> emailError = _emailError;
    public LiveData<String> passwordError = _passwordError;

    // =========================
    // Click Events
    // =========================
    public void onLoginClick() {
        validate();

        if (_emailError.getValue() == null &&
                _passwordError.getValue() == null) {

            // TODO: Call repository / API
            System.out.println("Login success with: "
                    + email.getValue() + " / "
                    + password.getValue());
        }
    }

    public void onForgotPasswordClick() {
        // TODO: Navigate to Forgot Password screen
        System.out.println("Forgot password clicked");
    }

    public void onRegisterClick() {
        // TODO: Navigate to Register screen
        System.out.println("Register clicked");
    }

    // =========================
    // Validation Logic
    // =========================
    private void validate() {
        String emailValue = email.getValue();
        String passwordValue = password.getValue();

        // Validate Email
        if (emailValue == null || emailValue.isEmpty()) {
            _emailError.setValue("Email is required");
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            _emailError.setValue("Invalid email format");
        } else {
            _emailError.setValue(null);
        }

        // Validate Password
        if (passwordValue == null || passwordValue.isEmpty()) {
            _passwordError.setValue("Password is required");
        } else if (passwordValue.length() < 6) {
            _passwordError.setValue("Password must be at least 6 characters");
        } else {
            _passwordError.setValue(null);
        }
    }
}
