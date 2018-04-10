package com.linkedin.android.shaky;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

class AttachmentListAdapter extends ArrayAdapter<AttachmentData> {

    private final AttachmentViewHolder.OnAttachmentClickListener onAttachmentClickListener;

    AttachmentListAdapter(@NonNull Context context,
                          @NonNull AttachmentViewHolder.OnAttachmentClickListener onAttachmentClickListener) {
        super(context, 0);
        this.onAttachmentClickListener = onAttachmentClickListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.shaky_attachment_view, parent, false);
            AttachmentViewHolder viewHolder = new AttachmentViewHolder(convertView, onAttachmentClickListener);
            convertView.setTag(viewHolder);
        }
        AttachmentViewHolder viewHolder = (AttachmentViewHolder) convertView.getTag();
        if (viewHolder != null) {
            viewHolder.bind(getItem(position));
        }
        return convertView;
    }

    boolean containsAttachmentUri(@NonNull Uri uri) {
        int size = getCount();
        AttachmentData data;
        for (int i = 1; i < size; i++) {
            data = getItem(i);
            if (data != null && uri.equals(data.uri)) {
                return true;
            }
        }
        return false;
    }

    ArrayList<Uri> getAttachmentUriList() {
        int size = getCount();
        AttachmentData data;
        ArrayList<Uri> list = new ArrayList<>(size);
        // Do not add screenshot
        for (int i = 1; i < size; i++) {
            data = getItem(i);
            if (data != null) {
                list.add(data.uri);
            }
        }
        return list;
    }
}
