package com.group04.scrapbookwidget.ui.auth.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.group04.scrapbookwidget.data.model.User;
import com.group04.scrapbookwidget.data.repository.IUserRepository;
import com.group04.scrapbookwidget.data.repository.RepositoryCallback;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final IUserRepository userRepository;

    // =========================
    // Input Fields
    // =========================
    public final MutableLiveData<String> email = new MutableLiveData<>("");
    public final MutableLiveData<String> password = new MutableLiveData<>("");

    // =========================
    // State Fields
    // =========================
    private final MutableLiveData<String> _emailError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _passwordError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<User> _user = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _navigateToRegister = new MutableLiveData<>(false);

    public LiveData<String> getEmailError() { return _emailError; }
    public LiveData<String> getPasswordError() { return _passwordError; }
    public LiveData<Boolean> getIsLoading() { return _isLoading; }
    public LiveData<String> getErrorMessage() { return _errorMessage; }
    public LiveData<User> getUser() { return _user; }
    public LiveData<Boolean> getNavigateToRegister() { return _navigateToRegister; }

    @Inject
    public LoginViewModel(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =========================
    // Click Events
    // =========================
    public void onLoginClick() {
        validate();

        if (_emailError.getValue() == null && _passwordError.getValue() == null) {
            _isLoading.setValue(true);
            _errorMessage.setValue(null);

            userRepository.login(email.getValue(), password.getValue(), new RepositoryCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    _isLoading.setValue(false);
                    _user.setValue(result);
                }

                @Override
                public void onError(Exception e) {
                    _isLoading.setValue(false);
                    _errorMessage.setValue(e.getMessage());
                }
            });
        }
    }

    public void onGoogleLoginSuccess(String idToken) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        userRepository.loginWithGoogle(idToken, new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                _isLoading.setValue(false);
                _user.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                _isLoading.setValue(false);
                _errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void onForgotPasswordClick() {
        // TODO: Navigate to Forgot Password screen
    }

    public void onRegisterClick() {
        _navigateToRegister.setValue(true);
    }

    public void onNavigatedToRegister() {
        _navigateToRegister.setValue(false);
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
