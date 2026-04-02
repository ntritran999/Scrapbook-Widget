package com.group04.scrapbookwidget.ui;

import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;

public class BindingAdapters {
    @BindingAdapter("app:tint")
    public static void setImageTint(ImageView view, int color) {
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(color));
    }

    @BindingAdapter("imageUrl")
    public static void setImageUrl(ImageView view, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(view.getContext())
                    .load(url)
                    .placeholder(R.drawable.account_circle_24)
                    .circleCrop()
                    .into(view);
        } else {
            view.setImageResource(R.drawable.account_circle_24);
        }
    }
}
