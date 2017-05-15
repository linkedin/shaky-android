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
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

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

        findViewById(R.id.demo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ShakyApplication) getApplication()).getShaky().startFeedbackFlow();
            }
        });
    }
}
