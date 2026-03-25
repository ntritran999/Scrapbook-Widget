package com.group04.scrapbookwidget.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.group04.scrapbookwidget.R;
import com.group04.scrapbookwidget.data.model.Group;

import java.util.List;

public class CompactGroupListAdapter extends ArrayAdapter<String> {
    private String[] groupNames;
    private String[] groupAvatars;
    private Context context;
    private int layout;
    public CompactGroupListAdapter(@NonNull Context context, int resource, String[] groupNames, String[] groupAvatars) {
        super(context, resource, groupNames);
        this.context = context;
        layout = resource;
        this.groupNames = groupNames;
        this.groupAvatars = groupAvatars;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(layout, null);
        TextView groupName = row.findViewById(R.id.group_name);
        ImageView imageView = row.findViewById(R.id.group_avatar);

        groupName.setText(groupNames[position]);
        Glide.with(context)
                .load(groupAvatars[position])
                .circleCrop()
                .into(imageView);
        return row;
    }
}
