package com.group04.scrapbookwidget.ui;

import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;

public class BindingAdapters {
    @BindingAdapter("app:tint")
    public static void setImageTint(ImageView view, int color) {
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(color));
    }
}
