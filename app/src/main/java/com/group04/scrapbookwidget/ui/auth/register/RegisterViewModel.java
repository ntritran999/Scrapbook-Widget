package com.group04.scrapbookwidget.ui.auth.register;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;
import com.group04.scrapbookwidget.data.service.UserService;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RegisterViewModel extends ViewModel {

    private final IUserRepository userRepository;

    // =========================
    // Input Fields
    // =========================
    public final MutableLiveData<String> name = new MutableLiveData<>("");
    public final MutableLiveData<String> email = new MutableLiveData<>("");
    public final MutableLiveData<String> password = new MutableLiveData<>("");
    public final MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    // =========================
    // State Fields
    // =========================
    private final MutableLiveData<String> _nameError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _emailError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _passwordError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _confirmPasswordError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _registerSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _navigateToLogin = new MutableLiveData<>(false);
    private final MutableLiveData<String> _otpPromptEmail = new MutableLiveData<>(null);
    private final MutableLiveData<String> _otpInfoMessage = new MutableLiveData<>(null);

    private String pendingEmail;
    private String pendingPassword;
    private String pendingDisplayName;

    public LiveData<String> getNameError() { return _nameError; }
    public LiveData<String> getEmailError() { return _emailError; }
    public LiveData<String> getPasswordError() { return _passwordError; }
    public LiveData<String> getConfirmPasswordError() { return _confirmPasswordError; }
    public LiveData<Boolean> getIsLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<Boolean> getRegisterSuccess() { return _registerSuccess; }
    public LiveData<Boolean> getNavigateToLogin() { return _navigateToLogin; }
    public LiveData<String> getOtpPromptEmail() { return _otpPromptEmail; }
    public LiveData<String> getOtpInfoMessage() { return _otpInfoMessage; }

    @Inject
    public RegisterViewModel(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =========================
    // Click Events
    // =========================
    public void onRegisterClick() {
        validate();

        if (_emailError.getValue() == null &&
                _passwordError.getValue() == null &&
                _confirmPasswordError.getValue() == null) {

            _isLoading.setValue(true);
            _errorMessage.setValue(null);

            pendingEmail = email.getValue();
            pendingPassword = password.getValue();
            pendingDisplayName = name.getValue() != null && !name.getValue().isEmpty() ? name.getValue() : email.getValue();

            userRepository.requestRegisterOtp(pendingEmail, new RepositoryCallback<UserService.RegisterOtpResponse>() {
                @Override
                public void onSuccess(UserService.RegisterOtpResponse result) {
                    _isLoading.setValue(false);
                    if (result != null) {
                        _otpInfoMessage.setValue("OTP sent to " + pendingEmail + ". It expires in "
                                + result.expiresInMinutes + " minutes.");
                    }
                    _otpPromptEmail.setValue(pendingEmail);
                }

                @Override
                public void onError(Exception e) {
                    _isLoading.setValue(false);
                    _errorMessage.setValue(e.getMessage());
                }
            });
        }
    }

    public void submitOtpCode(String otpCode) {
        if (pendingEmail == null || pendingPassword == null || pendingDisplayName == null) {
            _errorMessage.setValue("Please start registration again.");
            return;
        }
        if (otpCode == null || otpCode.trim().isEmpty()) {
            _errorMessage.setValue("OTP code is required");
            _otpPromptEmail.setValue(pendingEmail);
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        userRepository.registerWithOtp(
                pendingEmail,
                pendingPassword,
                pendingDisplayName,
                otpCode.trim(),
                new RepositoryCallback<UserService.RegisterResponse>() {
                    @Override
                    public void onSuccess(UserService.RegisterResponse result) {
                        _isLoading.setValue(false);
                        _registerSuccess.setValue(true);
                    }

                    @Override
                    public void onError(Exception e) {
                        _isLoading.setValue(false);
                        _errorMessage.setValue(e.getMessage());
                        _otpPromptEmail.setValue(pendingEmail);
                    }
                }
        );
    }

    public void onOtpPromptShown() {
        _otpPromptEmail.setValue(null);
    }

    public void onLoginClick() {
        _navigateToLogin.setValue(true);
    }

    public void onNavigatedToLogin() {
        _navigateToLogin.setValue(false);
        _registerSuccess.setValue(false);
        _otpPromptEmail.setValue(null);
        _otpInfoMessage.setValue(null);
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
