package com.group04.scrapbookwidget.ui.auth.register;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RegisterViewModel extends ViewModel {

    // =========================
    // Input Fields
    // =========================
    public final MutableLiveData<String> email = new MutableLiveData<>("");
    public final MutableLiveData<String> password = new MutableLiveData<>("");
    public final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    // =========================
    // Error Fields
    // =========================
    private final MutableLiveData<String> _emailError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _passwordError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _confirmPasswordError = new MutableLiveData<>(null);

    public LiveData<String> emailError = _emailError;
    public LiveData<String> passwordError = _passwordError;
    public LiveData<String> confirmPasswordError = _confirmPasswordError;

    // =========================
    // Click Events
    // =========================
    public void onRegisterClick() {
        validate();

        if (_emailError.getValue() == null &&
                _passwordError.getValue() == null &&
                _confirmPasswordError.getValue() == null) {

            // TODO: Call repository / API
            System.out.println("Register success with: " + email.getValue());
        }
    }

    public void onLoginClick() {
        // TODO: Navigate to Login screen
        System.out.println("Login clicked from Register");
    }

    // =========================
    // Validation Logic
    // =========================
    private void validate() {
        String emailValue = email.getValue();
        String passwordValue = password.getValue();
        String confirmValue = confirmPassword.getValue();

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

        // Validate Confirm Password
        if (confirmValue == null || confirmValue.isEmpty()) {
            _confirmPasswordError.setValue("Please confirm your password");
        } else if (!confirmValue.equals(passwordValue)) {
            _confirmPasswordError.setValue("Passwords do not match");
        } else {
            _confirmPasswordError.setValue(null);
        }
    }
}
