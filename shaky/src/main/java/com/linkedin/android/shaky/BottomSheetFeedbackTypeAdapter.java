/**
 * Copyright (C) 2016 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.shaky;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * RecyclerView.Adapter for the select type of feedback view.
 */
class BottomSheetFeedbackTypeAdapter extends RecyclerView.Adapter<BottomSheetFeedbackTypeAdapter.RowViewHolder> {

    public static final String ACTION_FEEDBACK_TYPE_SELECTED = "ActionFeedbackTypeSelected";
    public static final String EXTRA_FEEDBACK_TYPE = "ExtraFeedbackType";
    public static final String FEEDBACK_ACTION = "FeedbackAction";

    private final LayoutInflater inflater;
    private final BottomSheetFeedbackItem[] itemsList;

    BottomSheetFeedbackTypeAdapter(@NonNull LayoutInflater inflater, @NonNull BottomSheetFeedbackItem[] itemsList) {
        this.inflater = inflater;
        this.itemsList = itemsList;
    }

    @Override
    public int getItemCount() {
        return itemsList == null ? 0 : itemsList.length;
    }

    @Override
    public RowViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
        return new RowViewHolder(inflater.inflate(R.layout.shaky_single_row, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RowViewHolder rowViewHolder, int position) {
        final BottomSheetFeedbackItem item = itemsList[position];

        rowViewHolder.titleView.setText(String.valueOf(item.title));
        rowViewHolder.descriptionView.setText(String.valueOf(item.description));
        rowViewHolder.imageView.setImageResource(item.icon);
        rowViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_FEEDBACK_TYPE_SELECTED);
                intent.putExtra(EXTRA_FEEDBACK_TYPE, item.feedbackType);
                intent.putExtra(FEEDBACK_ACTION, item.action);
                LocalBroadcastManager.getInstance(v.getContext()).sendBroadcast(intent);
            }
        });
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;
        public final TextView titleView;
        public final TextView descriptionView;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.shaky_row_icon);
            this.titleView = (TextView) itemView.findViewById(R.id.shaky_row_title);
            this.descriptionView = (TextView) itemView.findViewById(R.id.shaky_row_description);
        }
    }
}
