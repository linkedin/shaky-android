/**
 * Copyright (C) 2016 LinkedIn Corp.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.shaky;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * RecyclerView.Adapter for the select type of feedback view.
 */
class BottomSheetFeedbackTypeAdapter extends RecyclerView.Adapter<BottomSheetFeedbackTypeAdapter.RowViewHolder> {

    public static final String EXTRA_FEEDBACK_TYPE = "ExtraFeedbackType";
    private static final int DISMISS_OPTION_POSITION = 2;
    private final BottomSheetFeedbackItem[] itemsList;
    @NonNull
    private final BottomSheetDialog bottomSheetFeedbackFragment;

    @StyleRes
    private final int theme;

    BottomSheetFeedbackTypeAdapter(@NonNull BottomSheetFeedbackItem[] itemsList,
                                   @NonNull BottomSheetDialog bottomSheetFeedbackFragment,
                                   @StyleRes int theme) {
        this.itemsList = itemsList;
        this.bottomSheetFeedbackFragment = bottomSheetFeedbackFragment;
        this.theme = theme;
    }

    @Override
    public int getItemCount() {
        return itemsList.length;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        LayoutInflater inflater = Utils.applyThemeToInflater(LayoutInflater.from(viewGroup.getContext()), theme);
        View view = inflater.inflate(R.layout.shaky_single_row, viewGroup, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RowViewHolder rowViewHolder, int position) {
        final BottomSheetFeedbackItem item = itemsList[position];

        rowViewHolder.titleView.setText(item.title);
        rowViewHolder.descriptionView.setText(item.description);
        rowViewHolder.imageView.setImageResource(item.icon);
        rowViewHolder.itemView.setOnClickListener(v -> {
            // Dismiss is clicked
            if (position == DISMISS_OPTION_POSITION) {
                bottomSheetFeedbackFragment.dismiss();
                return;
            }
            Intent intent = new Intent(item.action);
            intent.putExtra(EXTRA_FEEDBACK_TYPE, item.feedbackType);
            LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(intent);
            bottomSheetFeedbackFragment.dismiss();
        });
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;
        public final TextView titleView;
        public final TextView descriptionView;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.shaky_row_icon);
            this.titleView = itemView.findViewById(R.id.shaky_row_title);
            this.descriptionView = itemView.findViewById(R.id.shaky_row_description);
        }
    }
}
