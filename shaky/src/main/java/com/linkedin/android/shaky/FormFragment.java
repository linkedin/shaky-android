/**
 * Copyright (C) 2016 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.shaky;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * The main form used to send feedback.
 */
public class FormFragment extends Fragment {

    static final String ACTION_SUBMIT_FEEDBACK = "ActionSubmitFeedback";
    static final String ACTION_EDIT_IMAGE = "ActionEditImage";

    static final String EXTRA_USER_MESSAGE = "ExtraUserMessage";
    static final String EXTRA_ATTACHMENTS = "ExtraAttachments";

    private static final String KEY_SCREENSHOT_URI = "ScreenshotUri";
    private static final String KEY_TITLE = "title";
    private static final String KEY_HINT = "hint";
    private static final String KEY_ADD_ATTACHMENT = "addAttachment";
    private static final String ALL = "*/*";
    private static final int ATTACHMENT_REQUEST_CODE = 0x1234;
    private static final String SCREENSHOT = "screenshot.png";
    private final AttachmentViewHolder.OnAttachmentClickListener onAttachmentClickListener
        = new AttachmentViewHolder.OnAttachmentClickListener() {
        @Override
        public void onRemoved(@NonNull Uri fileUri) {
            removeAttachment(fileUri);
        }

        @Override
        public void onClicked(@NonNull Uri fileUri) {
            if (fileUri.equals(screenshotUri)) {
                editScreenshot();
            }
        }
    };
    private Uri screenshotUri;
    private AttachmentListAdapter attachmentListAdapter;

    public static FormFragment newInstance(@NonNull String title,
                                           @NonNull String hint,
                                           @Nullable Uri screenshotUri,
                                           boolean addAttachmentShown) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_SCREENSHOT_URI, screenshotUri);
        args.putString(KEY_TITLE, title);
        args.putString(KEY_HINT, hint);
        args.putBoolean(KEY_ADD_ATTACHMENT, addAttachmentShown);

        FormFragment fragment = new FormFragment();
        fragment.setRetainInstance(true);
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
        ListView attachmentsView = (ListView) view.findViewById(R.id.attachments_view);
        ImageView attachmentImageView = (ImageView) view.findViewById(R.id.shaky_form_attachment);
        Button addAttachmentButton = (Button) view.findViewById(R.id.add_attachment_button);
        View screenshotView = view.findViewById(R.id.screenshot_view);
        screenshotUri = getArguments().getParcelable(KEY_SCREENSHOT_URI);
        boolean addAttachmentShown = getArguments().getBoolean(KEY_ADD_ATTACHMENT, false);
        String title = getArguments().getString(KEY_TITLE);
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(createNavigationClickListener());
        toolbar.inflateMenu(R.menu.shaky_feedback_activity_actions);
        toolbar.setOnMenuItemClickListener(createMenuClickListener(messageEditText));

        String hint = getArguments().getString(KEY_HINT);
        messageEditText.setHint(hint);
        messageEditText.requestFocus();

        if (addAttachmentShown) {
            attachmentsView.setVisibility(View.VISIBLE);
            addAttachmentButton.setVisibility(View.VISIBLE);
            addAttachmentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDocumentPicker();
                }
            });
            screenshotView.setVisibility(View.GONE);
            attachmentListAdapter = new AttachmentListAdapter(getContext(), onAttachmentClickListener);
            attachmentsView.setAdapter(attachmentListAdapter);
            addAttachment(screenshotUri, SCREENSHOT, false);
            if (savedInstanceState == null) {
                // try to restore from activity-scope states
                if (getActivity() instanceof FeedbackActivity) {
                    restoreAttachmentsState(((FeedbackActivity) getActivity()).fragmentStates);
                }
            } else {
                // restore from instance states
                restoreAttachmentsState(savedInstanceState);
            }
        } else {
            screenshotView.setVisibility(View.VISIBLE);
            addAttachmentButton.setVisibility(View.GONE);
            attachmentImageView.setImageURI(screenshotUri);
            attachmentImageView.setOnClickListener(createNavigationClickListener());
            attachmentsView.setVisibility(View.GONE);
        }
    }

    private void showDocumentPicker() {
        Intent intent = new Intent(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                                   ? Intent.ACTION_OPEN_DOCUMENT
                                   : Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(ALL);
        // only choosing from local storage due to permission issues
        // and lack of loading progress when downloading from a network location
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }
        startActivityForResult(intent, ATTACHMENT_REQUEST_CODE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null && attachmentListAdapter != null) {
            saveAttachmentsState(outState);
        }
    }

    private void saveAttachmentsState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(EXTRA_ATTACHMENTS, attachmentListAdapter.getAttachmentUriList());
    }

    private void restoreAttachmentsState(@NonNull Bundle states) {
        ArrayList<Uri> attachments = states.getParcelableArrayList(EXTRA_ATTACHMENTS);
        if (attachments != null) {
            for (Uri uri : attachments) {
                addAttachment(uri, null, true);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == ATTACHMENT_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                List<Uri> list = new ArrayList<>();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN
                    && intent.getClipData() != null) {
                    ClipData clipData = intent.getClipData();
                    for (int i = 0, size = clipData.getItemCount(); i < size; i++) {
                        list.add(clipData.getItemAt(i).getUri());
                    }
                } else if (intent.getData() != null) {
                    list.add(intent.getData());
                }
                Context context = getContext();
                for (Uri uri : list) {
                    addAttachment(uri, null, true);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private void addAttachment(@Nullable Uri fileUri, @Nullable String defaultFilename, boolean removable) {
        if (fileUri == null || attachmentListAdapter == null) {
            return;
        }
        if (!attachmentListAdapter.containsAttachmentUri(fileUri)) {
            // avoid duplicated attachments
            attachmentListAdapter.add(new AttachmentData(fileUri, defaultFilename, removable));
        }
    }

    private void removeAttachment(@NonNull Uri fileUri) {
        if (attachmentListAdapter != null) {
            attachmentListAdapter.remove(new AttachmentData(fileUri));
        }
    }

    @NonNull
    private View.OnClickListener createNavigationClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editScreenshot();
            }
        };
    }

    private void editScreenshot() {
        // replace fragments won't call onSaveInstanceStates() so we want to store the attachments within activity
        if (attachmentListAdapter != null && getActivity() instanceof FeedbackActivity) {
            saveAttachmentsState(((FeedbackActivity) getActivity()).fragmentStates);
        }
        Intent intent = new Intent(ACTION_EDIT_IMAGE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
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
                        if (attachmentListAdapter != null) {
                            intent.putParcelableArrayListExtra(EXTRA_ATTACHMENTS,
                                                               attachmentListAdapter.getAttachmentUriList());
                        }
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                                                                  R.style.Theme_AppCompat_Light_Dialog_Alert);
            builder.setMessage(getString(R.string.shaky_empty_feedback_message));
            builder.setPositiveButton(getString(R.string.shaky_empty_feedback_confirm),
                                      new DialogInterface.OnClickListener() {
                                          public void onClick(DialogInterface dialog, int which) {
                                              dialog.dismiss();
                                          }
                                      });
            builder.show();
            return false;
        }

        return true;
    }
}
