package com.group04.scrapbookwidget.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentEditProfileBinding;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

import static android.app.Activity.RESULT_OK;

@AndroidEntryPoint
public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private EditProfileViewModel viewModel;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Intent> cropLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                startCrop(uri);
            }
        });

        cropLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                final Uri resultUri = UCrop.getOutput(result.getData());
                if (resultUri != null) {
                    viewModel.uploadAvatar(new File(resultUri.getPath()));
                }
            } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                final Throwable cropError = UCrop.getError(result.getData());
                String message = (cropError != null) ? cropError.getMessage() : "Unknown error";
                Toast.makeText(requireContext(), "Crop error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupObservers();
        setupClickListeners();

        return binding.getRoot();
    }

    private void setupObservers() {
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.account_circle_24)
                        .circleCrop()
                        .into(binding.imgProfile);
            }
        });

        viewModel.getUpdateSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        View.OnClickListener photoPickerListener = v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        };

        binding.imgProfile.setOnClickListener(photoPickerListener);
        binding.btnChangePhoto.setOnClickListener(photoPickerListener);
    }

    private void startCrop(@NonNull Uri uri) {
        Context context = requireContext();
        String destinationFileName = "avatar_crop_" + UUID.randomUUID().toString() + ".jpg";
        File cacheFile = new File(context.getCacheDir(), destinationFileName);
        Uri destinationUri = Uri.fromFile(cacheFile);

        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setCompressionQuality(80);
        
        options.setToolbarColor(ContextCompat.getColor(context, R.color.black));
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.black));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(context, R.color.locket_yellow));
        options.setToolbarWidgetColor(ContextCompat.getColor(context, R.color.white));

        Intent uCropIntent = UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1)
                .withOptions(options)
                .getIntent(context);
        
        cropLauncher.launch(uCropIntent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
