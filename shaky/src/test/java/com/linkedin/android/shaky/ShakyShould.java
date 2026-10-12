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
import android.app.FragmentTransaction;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import androidx.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import com.squareup.seismic.ShakeDetector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(fragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class));
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        when(activity.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(decorView.getRootView()).thenReturn(decorView);
        when(activity.getApplicationContext()).thenReturn(activity);
        when(activity.getMainLooper()).thenReturn(Looper.getMainLooper());
        when(delegate.getSensitivityLevel()).thenReturn(ShakeDelegate.SENSITIVITY_LIGHT);

        shaky = new Shaky(activity, delegate, null);
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

    @Test
    public void defaultToHandlingShake() {
        assertTrue(new ShakeDelegate() {
            @Override
            public void submit(@NonNull Activity activity, @NonNull Result result) { }
        }.shouldHandleShake());
    }

    @Test
    public void ignoreShakeWhenCustomHandlingAndDelegateDeclines() {
        ShakyFlowCallback callback = mock(ShakyFlowCallback.class);
        when(delegate.isCustomHandlingOfShakeEnabled()).thenReturn(true);
        when(delegate.shouldHandleShake()).thenReturn(false);
        Shaky customShaky = new Shaky(activity, delegate, callback);
        customShaky.setActivity(activity);

        customShaky.hearShake();

        verify(callback, never()).onShakyStarted(anyInt());
        assertFalse(customShaky.shouldIgnoreShake());
    }

    @Test
    public void startFlowWhenCustomHandlingAndDelegateAccepts() {
        ShakyFlowCallback callback = mock(ShakyFlowCallback.class);
        when(delegate.isCustomHandlingOfShakeEnabled()).thenReturn(true);
        when(delegate.shouldHandleShake()).thenReturn(true);
        Shaky customShaky = new Shaky(activity, delegate, callback);
        customShaky.setActivity(activity);

        customShaky.hearShake();

        verify(callback).onShakyStarted(ShakyFlowCallback.SHAKY_STARTED_BY_SHAKE);
    }

    @Test
    public void notConsultShouldHandleShakeWithoutCustomHandling() {
        when(delegate.isCustomHandlingOfShakeEnabled()).thenReturn(false);

        shaky.hearShake();

        verify(delegate, never()).shouldHandleShake();
    }

    @Test
    public void respectDelegateSensitivityLevel() {
        assertEquals(shaky.getDetectorSensitivityLevel(), ShakeDetector.SENSITIVITY_LIGHT);
    }
}
