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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;

/**
 * Dialog to show while the app is collecting data. Used with {@link CollectDataTask}.
 */
public class CollectDataDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProgressDialog dialog = new ProgressDialog(getActivity(), R.style.AppCompatAlertDialog) {
            @Override
            public void onBackPressed() {
                // don't call super, so the back button won't close the dialog
            }
        };
        dialog.setTitle(R.string.collecting_feedback);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(true);
        dialog.setProgressPercentFormat(null);
        dialog.setProgressNumberFormat(null);
        return dialog;
    }
}
