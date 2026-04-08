package com.group04.scrapbookwidget.ui.group;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.group04.scrapbookwidget.databinding.ItemSmartReplyBinding;

import java.util.ArrayList;
import java.util.List;

public class SmartReplyAdapter extends RecyclerView.Adapter<SmartReplyAdapter.SmartReplyViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String text);
    }

    private final List<String> suggestions = new ArrayList<>();
    private final OnSuggestionClickListener onSuggestionClickListener;

    public SmartReplyAdapter(OnSuggestionClickListener onSuggestionClickListener) {
        this.onSuggestionClickListener = onSuggestionClickListener;
    }

    public void setSuggestions(List<String> items) {
        suggestions.clear();
        if (items != null) {
            suggestions.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SmartReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSmartReplyBinding binding = ItemSmartReplyBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SmartReplyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SmartReplyViewHolder holder, int position) {
        holder.bind(suggestions.get(position));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    class SmartReplyViewHolder extends RecyclerView.ViewHolder {
        private final ItemSmartReplyBinding binding;

        SmartReplyViewHolder(ItemSmartReplyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String suggestion) {
            binding.tvSmartReply.setText(suggestion);
            binding.tvSmartReply.setOnClickListener(v -> {
                if (onSuggestionClickListener != null) {
                    onSuggestionClickListener.onSuggestionClick(suggestion);
                }
            });
        }
    }
}
