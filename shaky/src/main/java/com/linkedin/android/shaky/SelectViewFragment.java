/**
 * Copyright (C) 2016 LinkedIn Corp.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.shaky;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Fragment to select a sub view on an image.
 * Renders an image in the background and lets the user select a subview on it with {@link Slate}.
 * <p>
 * TODO: on save, this overrides the given imageUri (preventing full-undo of selecting).
 * TODO: if the user rotates the device while drawing, the selections are not translated.
 */
public class SelectViewFragment extends Fragment {
    static final String ACTION_SELECT_COMPLETE = "ActionSelectingComplete";
    private static final String TAG = SelectViewFragment.class.getSimpleName();
    private static final String KEY_IMAGE_URI = "imageUri";
    private static final String KEY_SUB_VIEWS_LIST = "subViewsList";
    private static final String KEY_THEME = "theme";
    private static final int FULL_QUALITY = 100;
    private Slate slate;
    private Uri imageUri;

    static SelectViewFragment newInstance(@Nullable Uri imageUri, @Nullable Integer theme, @Nullable ArrayList<Subview> subViewList) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_IMAGE_URI, imageUri);
        args.putParcelableArrayList(KEY_SUB_VIEWS_LIST, subViewList);

        if (theme != null) {
            args.putInt(KEY_THEME, theme);
        }

        SelectViewFragment fragment = new SelectViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        inflater = Utils.applyThemeToInflater(inflater,
                getArguments().getInt(KEY_THEME, FeedbackActivity.MISSING_RESOURCE));
        return inflater.inflate(R.layout.shaky_select_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        slate = (Slate) view.findViewById(R.id.shaky_slate);
        imageUri = getArguments().getParcelable(KEY_IMAGE_URI);
        ArrayList<Subview> subViewList = getArguments().getParcelableArrayList(KEY_SUB_VIEWS_LIST);

        if (imageUri != null) {
            try {
                // There seems to be an issue when using setImageUri that causes density to be chosen incorrectly
                // See: https://code.google.com/p/android/issues/detail?id=201491. This is fixed in API 24
                InputStream stream = getActivity().getContentResolver().openInputStream(imageUri);

                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                slate.setScaleType(ImageView.ScaleType.MATRIX);
                slate.setImageBitmap(bitmap);
            } catch (FileNotFoundException exception) {
                Log.e("Screenshot error", exception.getMessage(), exception);
            }
        }

        slate.setupSubViews(subViewList);
        slate.getViewTreeObserver().addOnGlobalLayoutListener(createViewTreeObserver());

        view.findViewById(R.id.shaky_button_save).setOnClickListener(createSaveClickListener());
    }

    private ViewTreeObserver.OnGlobalLayoutListener createViewTreeObserver() {
        return new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                slate.invalidate();

                // Remove the listener to avoid multiple callbacks
                slate.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        };
    }

    private View.OnClickListener createSaveClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slate.updateSelectedViews();
                Bitmap bitmap = slate.capture();
                if (bitmap != null) {
                    saveBitmap(bitmap);
                }
                Intent intent = new Intent(ACTION_SELECT_COMPLETE);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        };
    }

    private void saveBitmap(@NonNull Bitmap bitmap) {
        OutputStream outputStream = null;
        try {
            outputStream = getActivity().getContentResolver().openOutputStream(imageUri);
            bitmap.compress(Bitmap.CompressFormat.PNG, FULL_QUALITY, outputStream);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to write updated bitmap to disk", e);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to close output stream", e);
            }
        }
    }
}
