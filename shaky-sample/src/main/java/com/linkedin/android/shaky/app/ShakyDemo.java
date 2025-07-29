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
package com.linkedin.android.shaky.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.linkedin.android.shaky.ActionConstants;
import com.linkedin.android.shaky.Shaky;

import java.util.Random;

public class ShakyDemo extends FragmentActivity {

    private static final int RGB_MAX = 256;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        ViewCompat.setOnApplyWindowInsetsListener(
            getWindow().findViewById(R.id.demo_background),
            (v, insets) -> {
                Insets systemBars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return WindowInsetsCompat.CONSUMED;
            }
        );
        View tv = findViewById(R.id.demo_background);
        Random random = new Random();
        int color = Color.rgb(random.nextInt(RGB_MAX), random.nextInt(RGB_MAX), random.nextInt(RGB_MAX));
        tv.setBackgroundColor(color);

        findViewById(R.id.theme_checkbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) v;
                    ShakyApplication application = (ShakyApplication) getApplication();
                    if (checkBox.isChecked()) {
                        application.setShakyTheme(R.style.ShakyChristmasTheme);
                        application.setShakyPopupTheme(R.style.ShakyChristmasPopupTheme);
                    } else {
                        application.setShakyTheme(null);
                        application.setShakyPopupTheme(null);
                    }
                }
            }
        });

        findViewById(R.id.toast_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), R.string.toast_text, Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.open_bottom_sheet_button).setOnClickListener(v -> {
            SampleBottomSheetDialog bottomSheetDialogFragment = new SampleBottomSheetDialog();
            bottomSheetDialogFragment.show(this.getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        });

        findViewById(R.id.demo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShakyApplication) getApplication()).getShaky().startFeedbackFlow();
            }
        });

        findViewById(R.id.demo_bug_report_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShakyApplication) getApplication()).getShaky()
                        .startFeedbackFlow(ActionConstants.ACTION_START_BUG_REPORT);
            }
        });

        findViewById(R.id.demo_bottom_sheet_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShakyApplication) getApplication()).getShaky().startShakeBottomSheetFlowManually();
            }
        });

        ((ShakyApplication)getApplication()).getShaky().setUseMediaProjection(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Shaky shaky = ((ShakyApplication) getApplication()).getShaky();
        if (!shaky.handleActivityResult(requestCode, resultCode, data)) {
            // If Shaky did not handle the result, call the super method
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
