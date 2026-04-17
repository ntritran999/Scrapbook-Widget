package com.group04.scrapbookwidget.ui.auth.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentAuthLoginBinding;
import com.group04.scrapbookwidget.ui.MainActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AuthLoginFragment extends Fragment {

    private static final String TAG = "AuthLoginFragment";
    private FragmentAuthLoginBinding binding;
    private LoginViewModel viewModel;
    private GoogleSignInClient googleSignInClient;

    @Inject
    FirebaseAuth firebaseAuth;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "Google Sign-In result code: " + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            Log.d(TAG, "Google ID Token obtained, starting Firebase auth");
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Log.e(TAG, "Google ID Token is null. Check Web Client ID in strings.xml");
                            Toast.makeText(requireContext(), "Google Auth Failed: No Token", Toast.LENGTH_SHORT).show();
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed, status code: " + e.getStatusCode(), e);
                        Toast.makeText(requireContext(), "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Google Sign-In cancelled or failed with result code: " + result.getResultCode());
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAuthLoginBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupObservers();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
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

    private void signInWithGoogle() {
        Log.d(TAG, "signInWithGoogle: Forcing account picker by signing out first");
        // Clear the previous session to force account selection popup
        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle: Credentializing with Google ID Token");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "firebaseAuthWithGoogle: Firebase sign-in SUCCESS");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String firebaseIdToken = tokenTask.getResult().getToken();
                                    Log.d(TAG, "firebaseAuthWithGoogle: Obtained Firebase ID Token, calling backend");
                                    viewModel.onGoogleLoginSuccess(firebaseIdToken);
                                } else {
                                    Log.e(TAG, "firebaseAuthWithGoogle: Failed to get Firebase ID token", tokenTask.getException());
                                    Toast.makeText(requireContext(), "Auth Error: Token Retrieval Failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        Log.w(TAG, "firebaseAuthWithGoogle: Firebase sign-in FAILED", task.getException());
                        Toast.makeText(requireContext(), "Firebase Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
