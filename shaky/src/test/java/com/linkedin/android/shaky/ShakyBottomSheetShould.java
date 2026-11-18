/**
 * Copyright (C) 2026 LinkedIn Corp.
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
import android.content.Intent;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavior tests for closing the bottom sheet without picking a row.
 */
@RunWith(RobolectricTestRunner.class)
public class ShakyBottomSheetShould {

    private static final int DISMISS_ROW_POSITION = 2;
    private static final long TIMEOUT_MS = 10_000;

    Activity activity;
    RecordingDelegate delegate;
    Shaky shaky;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        delegate = new RecordingDelegate();
        shaky = new Shaky(activity, delegate, null);
        shaky.setActivity(activity);
    }

    @Test
    public void openFeedbackFormAfterSheetIsCancelled() {
        shaky.startShakeBottomSheetFlowManually();
        Dialog sheet = ShadowDialog.getLatestDialog();
        assertNotNull(sheet);

        // Back press, an outside tap and a swipe down all cancel the dialog.
        sheet.cancel();
        idleMainLooper();

        assertManualFlowOpensFeedbackForm();
    }

    @Test
    public void openFeedbackFormAfterDismissRowIsTapped() {
        shaky.startShakeBottomSheetFlowManually();
        Dialog sheet = ShadowDialog.getLatestDialog();
        assertNotNull(sheet);
        idleMainLooper();

        RecyclerView list = sheet.findViewById(R.id.shaky_recyclerView_bottomsheet);
        list.measure(0, 0);
        list.layout(0, 0, 1000, 2000);
        RecyclerView.ViewHolder dismissRow = list.findViewHolderForAdapterPosition(DISMISS_ROW_POSITION);
        assertNotNull(dismissRow);
        dismissRow.itemView.performClick();
        idleMainLooper();

        assertManualFlowOpensFeedbackForm();
    }

    private void assertManualFlowOpensFeedbackForm() {
        shaky.startFeedbackFlow();

        Intent started = null;
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (started == null && delegate.submitCount == 0 && System.currentTimeMillis() < deadline) {
            idleMainLooper();
            started = shadowOf(activity).getNextStartedActivity();
        }

        assertEquals("feedback was submitted without showing the form", 0, delegate.submitCount);
        assertNotNull("FeedbackActivity was not started", started);
        assertEquals(FeedbackActivity.class.getName(), started.getComponent().getClassName());
    }

    private static void idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static class RecordingDelegate extends ShakeDelegate {
        int submitCount;

        @Override
        public void submit(@NonNull Activity activity, @NonNull Result result) {
            submitCount++;
        }
    }
}
