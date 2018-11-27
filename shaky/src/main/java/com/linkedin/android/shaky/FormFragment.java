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

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * The main form used to send feedback.
 */
public class FormFragment extends Fragment {

    static final String ACTION_SUBMIT_FEEDBACK = "ActionSubmitFeedback";
    static final String ACTION_EDIT_IMAGE = "ActionEditImage";

    static final String EXTRA_USER_MESSAGE = "ExtraUserMessage";

    private static final String KEY_SCREENSHOT_URI = "ScreenshotUri";
    private static final String KEY_TITLE = "title";
    private static final String KEY_HINT = "hint";

    public static FormFragment newInstance(@NonNull String title,
                                           @NonNull String hint,
                                           @Nullable Uri screenshotUri) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_SCREENSHOT_URI, screenshotUri);
        args.putString(KEY_TITLE, title);
        args.putString(KEY_HINT, hint);

        FormFragment fragment = new FormFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.shaky_form, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.shaky_toolbar);
        EditText messageEditText = (EditText) view.findViewById(R.id.shaky_form_message);
        ImageView attachmentImageView = (ImageView) view.findViewById(R.id.shaky_form_attachment);

        Uri screenshotUri = getArguments().getParcelable(KEY_SCREENSHOT_URI);

        String title = getArguments().getString(KEY_TITLE);
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(createNavigationClickListener());
        toolbar.inflateMenu(R.menu.shaky_feedback_activity_actions);
        toolbar.setOnMenuItemClickListener(createMenuClickListener(messageEditText));

        String hint = getArguments().getString(KEY_HINT);
        messageEditText.setHint(hint);
        messageEditText.requestFocus();

        attachmentImageView.setImageURI(screenshotUri);
        attachmentImageView.setOnClickListener(createNavigationClickListener());
    }

    @NonNull
    private View.OnClickListener createNavigationClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_EDIT_IMAGE);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        };
    }

    @NonNull
    private Toolbar.OnMenuItemClickListener createMenuClickListener(@NonNull final EditText messageEditText) {
        return new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_submit) {
                    String message = messageEditText.getText().toString();

                    if (validate(message)) {
                        Intent intent = new Intent(ACTION_SUBMIT_FEEDBACK);
                        intent.putExtra(EXTRA_USER_MESSAGE, message);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Validates the message and returns true if the form is valid.
     */
    private boolean validate(@NonNull String message) {
        if (message.trim().length() == 0) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setMessage(getString(R.string.shaky_empty_feedback_message));
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.shaky_empty_feedback_confirm),
                                  new DialogInterface.OnClickListener() {
                                      public void onClick(DialogInterface dialog, int which) {
                                          dialog.dismiss();
                                      }
                                  });
            alertDialog.show();
            return false;
        }

        return true;
    }
}
