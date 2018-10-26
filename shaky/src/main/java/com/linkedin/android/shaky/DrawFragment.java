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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Fragment to draw on an image.
 * Renders an image in the background and lets the user draw on it with {@link Paper}.
 *
 * TODO: on save, this overrides the given imageUri (preventing full-undo of drawing).
 * TODO: if the user rotates the device while drawing, the paths are not translated.
 */
public class DrawFragment extends Fragment {

    static final String ACTION_DRAWING_COMPLETE = "ActionDrawingComplete";

    private static final String TAG = DrawFragment.class.getSimpleName();

    private static final String KEY_IMAGE_URI = "imageUri";

    private static final int FULL_QUALITY = 100;

    private Paper paper;
    private Uri imageUri;

    /**
     * Creates a new instance with the given image uri.
     */
    public static DrawFragment newInstance(@Nullable Uri imageUri) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_IMAGE_URI, imageUri);

        DrawFragment fragment = new DrawFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.shaky_draw, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        paper = (Paper) view.findViewById(R.id.shaky_paper);
        imageUri = getArguments().getParcelable(KEY_IMAGE_URI);
        if (imageUri != null) {
            try {
                // There seems to be an issue when using setImageUri that causes density to be chosen incorrectly
                // See: https://code.google.com/p/android/issues/detail?id=201491. This is fixed in API 24
                InputStream stream = getActivity().getContentResolver().openInputStream(imageUri);

                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                paper.setImageBitmap(bitmap);
            } catch (FileNotFoundException exception) {
                Log.e("Screenshot error", exception.getMessage(), exception);
            }
        }

        view.findViewById(R.id.shaky_button_clear).setOnClickListener(createClearClickListener());
        view.findViewById(R.id.shaky_button_save).setOnClickListener(createSaveClickListener());
        view.findViewById(R.id.shaky_button_brush).setOnClickListener(createBrushClickListener());
        view.findViewById(R.id.shaky_button_undo).setOnClickListener(createUndoClickListener());

        if (savedInstanceState == null) {
            Toast.makeText(getActivity(), getString(R.string.shaky_draw_hint), Toast.LENGTH_SHORT).show();
        }
    }

    private View.OnClickListener createClearClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paper.clear();
            }
        };
    }

    private View.OnClickListener createBrushClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button brushButton = (Button) v;
                paper.toggleBrush();
                brushButton.setText(getString(paper.isThinBrush() ? R.string.shaky_draw_brush : R.string.shaky_draw_brush_white));
            }
        };
    }

    private View.OnClickListener createSaveClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = paper.capture();
                if (bitmap != null) {
                    saveBitmap(bitmap);
                }
                Intent intent = new Intent(ACTION_DRAWING_COMPLETE);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        };
    }

    private View.OnClickListener createUndoClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paper.undo();
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
