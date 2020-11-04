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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Kicks off the feedback flow, pretty selector to choose the type of feedback.
 */
public class SelectFragment extends Fragment {

    private static final String KEY_THEME = "theme";

    @Nullable private LayoutInflater inflater;

    @NonNull
    static SelectFragment newInstance(@Nullable @StyleRes Integer theme) {
        SelectFragment fragment = new SelectFragment();
        Bundle args = new Bundle();
        if (theme != null) {
            args.putInt(KEY_THEME, theme);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.inflater = Utils.applyThemeToInflater(inflater,
                getArguments().getInt(KEY_THEME, FeedbackActivity.MISSING_RESOURCE));
        return this.inflater.inflate(R.layout.shaky_select, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FeedbackTypeAdapter adapter = new FeedbackTypeAdapter(inflater, getData());

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.shaky_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.shaky_toolbar);
        toolbar.setTitle(R.string.shaky_feedback_title);
        toolbar.setNavigationIcon(R.drawable.shaky_ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }

    @NonNull
    private FeedbackItem[] getData() {
        return new FeedbackItem[]{
                new FeedbackItem(
                        getString(R.string.shaky_row1_title),
                        getString(R.string.shaky_row1_subtitle),
                        R.drawable.shaky_img_magnifying_glass_56dp,
                        FeedbackItem.BUG
                ),
                new FeedbackItem(
                        getString(R.string.shaky_row2_title),
                        getString(R.string.shaky_row2_subtitle),
                        R.drawable.shaky_img_lightbulb_56dp,
                        FeedbackItem.FEATURE
                ),
                new FeedbackItem(
                        getString(R.string.shaky_row3_title),
                        getString(R.string.shaky_row3_subtitle),
                        R.drawable.shaky_img_message_bubbles_56dp,
                        FeedbackItem.GENERAL
                ),
        };
    }
}
