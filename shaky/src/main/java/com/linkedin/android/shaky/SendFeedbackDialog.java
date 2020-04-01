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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Auto-dismissing dialog that prompts the user to kick-off the feedback flow.
 */
public class SendFeedbackDialog extends DialogFragment {

    public static final String ACTION_START_FEEDBACK_FLOW = "StartFeedbackFlow";
    public static final String ACTION_DIALOG_DISMISSED_BY_USER = "DialogDismissedByUser";
    public static final String SHOULD_DISPLAY_SETTING_UI = "ShouldDisplaySettingUI";

    private static final long DISMISS_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);

    private Handler handler;
    private Runnable runnable;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                                                              R.style.AppCompatAlertDialog);

        View popupView = View.inflate(getActivity().getApplicationContext(),
                                      R.layout.shaky_popup, null);
        builder.setView(popupView);

        builder.setPositiveButton(R.string.shaky_dialog_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(ACTION_START_FEEDBACK_FLOW);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        });
        builder.setNegativeButton(R.string.shaky_dialog_negative, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(ACTION_DIALOG_DISMISSED_BY_USER);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        });
        if (getArguments().getBoolean(SHOULD_DISPLAY_SETTING_UI, false)) {
            builder.setNeutralButton(getResources().getString(R.string.shaky_setting), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        ShakySettingDialog shakySettingDialog = new ShakySettingDialog();
                        int currentSensitivity = getArguments().getInt(ShakySettingDialog.SHAKY_CURRENT_SENSITIVITY, ShakeDelegate.SENSITIVITY_MEDIUM);
                        Bundle bundle = new Bundle();
                        bundle.putInt(ShakySettingDialog.SHAKY_CURRENT_SENSITIVITY, currentSensitivity);
                        shakySettingDialog.setArguments(bundle);
                        shakySettingDialog.show(activity.getFragmentManager(), ShakySettingDialog.SHAKY_SETTING_DIALOG);
                    }
                }
            });
        }

        final AlertDialog dialog = builder.create();

        // auto-hide after timeout
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        };
        handler.postDelayed(runnable, DISMISS_TIMEOUT_MS);

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
