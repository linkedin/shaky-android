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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.ArrayRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
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
    static final String EXTRA_SUBCATEGORY = "ExtraSubcategory";
    // If you want to change the send icon, you must create a new menu
    static final @MenuRes int DEFAULT_MENU = R.menu.shaky_feedback_activity_actions;

    private static final String KEY_SCREENSHOT_URI = "ScreenshotUri";
    private static final String KEY_TITLE = "title";
    private static final String KEY_HINT = "hint";
    private static final String KEY_MENU = "menu";
    private static final String KEY_SUBTYPE_LABELS = "subtypeLabels";
    private static final String KEY_SUBTYPES = "subtypes";

    /**
     * Factory method for creating a {@link FormFragment}
     *
     * @param title The title of the form
     * @param hint Hint to display in the feedback text form when it is empty
     * @param screenshotUri {@link Uri} of the screenshot to attach if known
     * @param menu Resource ID of the "Send" icon
     * @param subtypeLabels Resource ID of the array of labels for subcategories to be shown in the
     *                      spinner, if subcategories apply. The resulting array must be the same
     *                      length and order as the "subtypes" param
     * @param subtypes Subtypes of this feedback type, if subtypes apply. Must be the same length and
     *                 order as the subtype labels array from the "subtypeLabels" param
     * @return A new {@link FormFragment}
     */
    public static FormFragment newInstance(@NonNull String title,
                                           @NonNull String hint,
                                           @Nullable Uri screenshotUri,
                                           @MenuRes int menu,
                                           @Nullable @ArrayRes Integer subtypeLabels,
                                           @Nullable String[] subtypes) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_SCREENSHOT_URI, screenshotUri);
        args.putString(KEY_TITLE, title);
        args.putString(KEY_HINT, hint);
        args.putInt(KEY_MENU, menu);
        args.putStringArray(KEY_SUBTYPES, subtypes);
        if (subtypeLabels != null) {
            args.putInt(KEY_SUBTYPE_LABELS, subtypeLabels);
        }

        FormFragment fragment = new FormFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Factory method for creating a {@link FormFragment}
     *
     * @param title The title of the form
     * @param hint Hint to display in the feedback text form when it is empty
     * @param screenshotUri {@link Uri} of the screenshot to attach if known
     * @param menu Resource ID of the "Send" icon
     *
     * @return A new {@link FormFragment}
     */
    public static FormFragment newInstance(@NonNull String title, @NonNull String hint,
        @Nullable Uri screenshotUri, @MenuRes int menu) {
        return newInstance(title, hint, screenshotUri, menu, null, null);
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
        int sendIconResource = getArguments().getInt(KEY_MENU);

        String[] subtypes = getArguments().getStringArray(KEY_SUBTYPES);
        Spinner spinner = null;
        if (subtypes != null) {
            spinner = view.findViewById(R.id.subtype_spinner);
            spinner.setVisibility(View.VISIBLE);
            FeedbackSubtypeAdapter adapter = new FeedbackSubtypeAdapter(getActivity(), subtypes,
                getArguments().getInt(KEY_SUBTYPE_LABELS));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

        String title = getArguments().getString(KEY_TITLE);
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(createNavigationClickListener());
        toolbar.inflateMenu(sendIconResource);
        toolbar.setOnMenuItemClickListener(createMenuClickListener(messageEditText, spinner));

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
    private Toolbar.OnMenuItemClickListener createMenuClickListener(
        @NonNull final EditText messageEditText, @Nullable final Spinner spinner) {
        return new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_submit) {
                    Intent intent = new Intent(ACTION_SUBMIT_FEEDBACK);
                    if (spinner != null) {
                        if (validateSubcategory(spinner.getSelectedItemPosition())) {
                            intent.putExtra(EXTRA_SUBCATEGORY, (String) spinner.getSelectedItem());
                        } else {
                            return false;
                        }
                    }

                    String message = messageEditText.getText().toString();
                    if (validateMessage(message)) {
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
    private boolean validateMessage(@NonNull String message) {
        if (message.trim().length() == 0) {
            showAlertDialog(getString(R.string.shaky_empty_feedback_message));
            return false;
        }
        return true;
    }

    private boolean validateSubcategory(int selectedItemPosition) {
        if (selectedItemPosition == 0) {
            showAlertDialog(getString(R.string.shaky_subcategory_not_selected_message));
            return false;
        }
        return true;
    }

    private void showAlertDialog(@NonNull String message) {
        AlertDialog alertDialog =
            new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialog).create();
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.shaky_general_ok_string),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        alertDialog.show();
    }

    private class FeedbackSubtypeAdapter extends ArrayAdapter<String> {
        @NonNull private CharSequence[] adapterLabels;
        private static final String UNSELECTED = "UNSELECTED";

        public FeedbackSubtypeAdapter(@NonNull Context context, @NonNull String[] subtypes,
            @ArrayRes int subtypeLabelsResource) {
            super(context, R.layout.shaky_spinner_item);
            CharSequence[] subtypeLabels = getResources().getTextArray(subtypeLabelsResource);
            if (subtypeLabels.length != subtypes.length) {
                throw new IllegalArgumentException(
                    "Subtypes array and subtype labels array are not the same length");
            }
            adapterLabels = new CharSequence[subtypeLabels.length + 1];
            adapterLabels[0] = getResources().getString(R.string.shaky_select_subcategory);
            System.arraycopy(subtypeLabels, 0, adapterLabels, 1, subtypeLabels.length);
            add(UNSELECTED);
            addAll(subtypes);
        }

        @Override
        public View getView(int position, @Nullable View convertView,
            @NonNull ViewGroup parent) {
            return makeTextView(adapterLabels[position], convertView, parent,
                R.layout.shaky_spinner_item);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView,
            @NonNull ViewGroup parent) {
            return makeTextView(adapterLabels[position], convertView, parent,
                android.R.layout.simple_spinner_dropdown_item);
        }

        private TextView makeTextView(@NonNull CharSequence adapterLabel, @Nullable View convertView,
            @NonNull ViewGroup parent, @LayoutRes int layoutResource) {
            TextView textView;
            if (convertView != null) {
                textView = (TextView) convertView;
            } else {
                textView = (TextView) LayoutInflater.from(getContext())
                    .inflate(layoutResource, parent, false);
            }
            textView.setText(adapterLabel);
            return textView;
        }
    }
}
