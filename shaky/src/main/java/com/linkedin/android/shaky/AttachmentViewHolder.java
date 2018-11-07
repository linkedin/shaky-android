package com.linkedin.android.shaky;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class AttachmentViewHolder {

    interface OnAttachmentClickListener {

        void onRemoved(@NonNull Uri fileUri);

        void onClicked(@NonNull Uri fileUri);
    }

    private static final String IMAGE_PREFIX = "image/";
    private final TextView filenameView;
    private final ImageView thumbnailView;
    private final ImageView deleteIcon;
    private final View rootView;

    AttachmentViewHolder(@NonNull View view, @NonNull final OnAttachmentClickListener listener) {
        filenameView = (TextView) view.findViewById(R.id.filename_view);
        deleteIcon = (ImageView) view.findViewById(R.id.delete_icon);
        thumbnailView = (ImageView) view.findViewById(R.id.thumbnail_view);
        rootView = view;
        filenameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() instanceof Uri) {
                    listener.onClicked((Uri) v.getTag());
                }
            }
        });
        thumbnailView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() instanceof Uri) {
                    listener.onClicked((Uri) v.getTag());
                }
            }
        });
        deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag() instanceof Uri) {
                    listener.onRemoved((Uri) v.getTag());
                }
            }
        });
    }

    private void bind(@NonNull Uri fileUri, @Nullable String defaultFilename, boolean removable) {
        filenameView.setText(Html.fromHtml(getFilename(fileUri, defaultFilename)));
        deleteIcon.setVisibility(removable ? View.VISIBLE : View.GONE);
        filenameView.setTag(fileUri);
        deleteIcon.setTag(fileUri);
        thumbnailView.setTag(fileUri);
        String mimeType = Utils.getMimeType(rootView.getContext(), fileUri);
        if (mimeType != null && mimeType.startsWith(IMAGE_PREFIX)) {
            thumbnailView.setVisibility(View.VISIBLE);
            thumbnailView.setImageURI(fileUri);
        } else {
            thumbnailView.setVisibility(View.GONE);
        }
    }

    void bind(@Nullable AttachmentData item) {
        if (item == null) {
            // edge case
            rootView.setVisibility(View.GONE);
        } else {
            rootView.setVisibility(View.VISIBLE);
            bind(item.uri, item.displayName, item.removable);
        }
    }

    private String getFilename(@NonNull Uri fileUri, @Nullable String defaultFilename) {
        return TextUtils.isEmpty(defaultFilename) ? Utils.getFilename(rootView.getContext(), fileUri)
                                                  : defaultFilename;
    }
}