package com.group04.scrapbookwidget.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.data.model.Group;
import com.group04.scrapbookwidget.databinding.ItemBackgroundBinding;

import java.util.List;

public class BackgroundSelectionRecyclerAdapter extends RecyclerView.Adapter<BackgroundSelectionRecyclerAdapter.BackgroundViewHolder>{
    private Context context;
    private String[] urls;

    private OnBackgroundSelectListener listener;
    public interface OnBackgroundSelectListener {
        void onBackgroundSelected(String url);
    }
    public BackgroundSelectionRecyclerAdapter(Context context, String[] urls, OnBackgroundSelectListener listener) {
        this.context = context;
        this.urls = urls;
        this.listener = listener;
    }
    @NonNull
    @Override
    public BackgroundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBackgroundBinding binding = ItemBackgroundBinding.inflate(LayoutInflater.from(context), parent, false);
        return new BackgroundViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BackgroundViewHolder holder, int position) {
        String imageUrl = urls[position];
        holder.bind(imageUrl, listener);
    }

    @Override
    public int getItemCount() {
        return urls != null ? urls.length : 0;
    }

    public void updateBackgrounds(String[] urls) {
        this.urls = urls;
        notifyDataSetChanged();
    }

    public static class BackgroundViewHolder extends RecyclerView.ViewHolder {
        private  ItemBackgroundBinding binding;
        public BackgroundViewHolder(@NonNull ItemBackgroundBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String imageUrl, OnBackgroundSelectListener listener) {
            Glide.with(binding.getRoot().getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(binding.imageView);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBackgroundSelected(imageUrl);
                }
            });
        }
    }
}
