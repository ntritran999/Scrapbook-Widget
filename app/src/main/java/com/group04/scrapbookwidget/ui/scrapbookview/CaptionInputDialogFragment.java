package com.group04.scrapbookwidget.ui.scrapbookview;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.databinding.DialogCaptionInputBinding;

public class CaptionInputDialogFragment extends DialogFragment {
    private DialogCaptionInputBinding binding;
    private OnCaptionConfirmedListener listener;

    public interface OnCaptionConfirmedListener {
        void onCaptionConfirmed(String caption);

        void onCaptionCancelled();
    }

    public void setOnCaptionConfirmedListener(OnCaptionConfirmedListener listener) {
        this.listener = listener;
    }

    public static CaptionInputDialogFragment newInstance() {
        return new CaptionInputDialogFragment();
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), 
                R.layout.dialog_caption_input, null, false);

        binding.captionInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.captionInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                confirmCaption();
                return true;
            }
            return false;
        });

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nhập Caption cho ảnh")
                .setView(binding.getRoot())
                .setPositiveButton("OK", (dialogInterface, i) -> confirmCaption())
                .setNegativeButton("Bỏ qua", (dialogInterface, i) -> {
                    if (listener != null) {
                        listener.onCaptionCancelled();
                    }
                });

        return dialog.create();
    }

    private void confirmCaption() {
        String caption = binding.captionInput.getText().toString().trim();
        if (listener != null) {
            listener.onCaptionConfirmed(caption);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
