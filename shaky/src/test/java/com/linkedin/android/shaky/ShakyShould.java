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
import android.app.FragmentManager;
import android.os.Looper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Behavior tests for the main library class file, ShakeToFeedback.
 */
@RunWith(RobolectricTestRunner.class)
public class ShakyShould {

    Shaky shaky;
    ShakeDelegate delegate;
    Activity activity;
    FragmentManager fragmentManager;

    @Before
    public void setUp() {
        delegate = mock(ShakeDelegate.class);
        activity = mock(Activity.class);
        fragmentManager = mock(FragmentManager.class);

        when(activity.getFragmentManager()).thenReturn(fragmentManager);
        when(activity.getApplicationContext()).thenReturn(activity);
        when(activity.getMainLooper()).thenReturn(Looper.getMainLooper());

        shaky = new Shaky(activity, delegate);
        shaky.setActivity(activity);
    }

    @Test
    public void coolDownBetweenShakes() {
        assertFalse(shaky.shouldIgnoreShake());
        assertTrue(shaky.shouldIgnoreShake());
    }

    @Test
    public void notStartFeedbackFlowWithNoActivityAttached() {
        shaky.setActivity(null);
        assertFalse(shaky.canStartFeedbackFlow());
    }

    @Test
    public void notStartFeedbackFlowWhileInFeedbackActivity() {
        FeedbackActivity feedbackActivity = new FeedbackActivity();
        shaky.setActivity(feedbackActivity);
        assertFalse(shaky.canStartFeedbackFlow());
    }
}
