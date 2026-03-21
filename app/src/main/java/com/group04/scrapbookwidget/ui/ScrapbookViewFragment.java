package com.group04.scrapbookwidget.ui;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.FragmentScrapbookViewBinding;

public class ScrapbookViewFragment extends Fragment {

    private FragmentScrapbookViewBinding binding;
    private float dX, dY;
    private ImageView currentPastingView; // Lưu giữ view đang được drag

    public ScrapbookViewFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_scrapbook_view, container, false);

        // Giả lập thời gian load view
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (binding != null) {
                binding.loadingOverlay.setVisibility(View.GONE);
            }
        }, 3000);

        binding.cameraBtn.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_scrapbookViewFragment_to_cameraFragment);
        });

        binding.btnConfirmPaste.setOnClickListener(v -> {
            if (currentPastingView != null) {
                currentPastingView.setOnTouchListener(null);
                currentPastingView = null;
                binding.btnConfirmPaste.setVisibility(View.GONE);
            }
        });

        if (getArguments() != null && getArguments().containsKey("PASTED_IMAGE_PATH")) {
            String imagePath = getArguments().getString("PASTED_IMAGE_PATH");
            if (imagePath != null) {
                addPastedImageToScrapbook(imagePath);
                binding.btnConfirmPaste.setVisibility(View.VISIBLE);
            }
        }

        return binding.getRoot();
    }

    private void addPastedImageToScrapbook(String path) {
        currentPastingView = new ImageView(requireContext());

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        currentPastingView.setImageBitmap(bitmap);

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.45);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, width);
        params.leftMargin = 150;
        params.topMargin = 150;
        currentPastingView.setLayoutParams(params);
        currentPastingView.setElevation(10f);
        setupDragListener(currentPastingView);

        binding.scrapbookFrame.addView(currentPastingView);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragListener(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start();
                    break;

                case MotionEvent.ACTION_MOVE:
                    v.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                    break;

                case MotionEvent.ACTION_UP:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    v.performClick();
                    break;
                default:
                    return false;
            }
            return true;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
