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

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.util.Random;

public class ShakyDemo extends Activity {

    private static final int RGB_MAX = 256;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        View tv = findViewById(R.id.demo_background);

        Random random = new Random();
        int color = Color.rgb(random.nextInt(RGB_MAX), random.nextInt(RGB_MAX), random.nextInt(RGB_MAX));
        tv.setBackgroundColor(color);

        boolean isApi35OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM;
        findViewById(R.id.theme_checkbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) v;
                    ShakyApplication application = (ShakyApplication) getApplication();
                    if (checkBox.isChecked()) {
                        application.setShakyTheme(isApi35OrAbove ? R.style.ShakyChristmasTheme_Api35: R.style.ShakyChristmasTheme);
                        application.setShakyPopupTheme(isApi35OrAbove ? R.style.ShakyChristmasPopupTheme_Api35 : R.style.ShakyChristmasPopupTheme);
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

        findViewById(R.id.demo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShakyApplication) getApplication()).getShaky().startFeedbackFlow();
            }
        });
    }
}
