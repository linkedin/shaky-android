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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Drawable view on an image.
 */
public class Paper extends AppCompatImageView {

    private static final float THIN_STROKE_WIDTH = 12f;
    private static final float THICK_STROKE_WIDTH = 48f;
    private static final int PATH_SMOOTH = 50;

    private Path thinPath = new Path();
    private Path thickPath = new Path();

    private Paint thinPaint = new Paint();
    private Paint thickPaint = new Paint();

    private Path currentBrush;

    /**
     * Save the lists of movement events so we can handle saving & restoring the view
     */
    private List<PathEvent> pathEvents = new ArrayList<>();

    public Paper(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);

        // red brush for marking up bugs
        thinPaint.setColor(Color.RED);
        thinPaint.setAntiAlias(true);
        thinPaint.setStrokeWidth(THIN_STROKE_WIDTH);
        thinPaint.setStyle(Paint.Style.STROKE);
        thinPaint.setStrokeJoin(Paint.Join.ROUND);
        thinPaint.setPathEffect(new CornerPathEffect(PATH_SMOOTH));

        // white brush to white-out sensitive information
        thickPaint = new Paint(thinPaint);
        thickPaint.setStrokeWidth(THICK_STROKE_WIDTH);
        thickPaint.setColor(Color.WHITE);

        // set default drawing path
        currentBrush = thinPath;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        PaperSavedState savedState = new PaperSavedState(parcelable);
        savedState.pathEvents = pathEvents;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        PaperSavedState savedState = (PaperSavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        pathEvents = savedState.pathEvents;
        applyEvents();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(thickPath, thickPaint);
        canvas.drawPath(thinPath, thinPaint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentBrush.moveTo(eventX, eventY);
                pathEvents.add(new PathEvent(isThinBrush(), true, eventX, eventY));
                break;
            case MotionEvent.ACTION_MOVE:
                currentBrush.lineTo(eventX, eventY);
                pathEvents.add(new PathEvent(isThinBrush(), false, eventX, eventY));
                break;
            default:
                return false;
        }

        invalidate(); // force repaint
        return true;
    }

    /**
     * Clears the drawing.
     */
    public void clear() {
        thinPath.reset();
        thickPath.reset();
        pathEvents.clear();
        invalidate();
    }

    public void toggleBrush() {
        currentBrush = currentBrush == thinPath ? thickPath : thinPath;
    }

    /**
     * @return true if we're using the thin currentBrush, false otherwise
     */
    public boolean isThinBrush() {
        return currentBrush == thinPath;
    }

    /**
     * Undo's the last draw action.
     */
    public void undo() {
        // undo to the last move event
        for (int i = pathEvents.size() - 1; i >= 0; i--) {
            if (pathEvents.remove(i).isMove) break;
        }
        applyEvents();
        invalidate();
    }

    /**
     * @return the current drawing as a Bitmap
     */
    public Bitmap capture() {
        Window window = null;
        if (getContext() instanceof Activity) {
            window = ((Activity)(getContext())).getWindow();
        } else if (getContext() instanceof ContextThemeWrapper) {
            Context baseContext = ((ContextThemeWrapper)getContext()).getBaseContext();
            if (baseContext instanceof Activity) {
                window = ((Activity)baseContext).getWindow();
            }
        } else {
            return null;
        }
        return Utils.capture(this, window);
    }

    private void applyEvents() {
        thickPath.reset();
        thinPath.reset();
        for (PathEvent event : pathEvents) {
            Path path = event.isThinPath ? thinPath : thickPath;
            if (event.isMove) {
                path.moveTo(event.x, event.y);
            } else {
                path.lineTo(event.x, event.y);
            }
        }
    }

    private static class PaperSavedState extends View.BaseSavedState {
        List<PathEvent> pathEvents;

        public PaperSavedState(Parcel source) {
            super(source);
            int size = source.readInt();
            pathEvents = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                pathEvents.add(new PathEvent(
                        source.readInt() == 0,
                        source.readInt() == 0,
                        source.readFloat(),
                        source.readFloat()
                ));
            }
        }

        public PaperSavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(pathEvents.size());
            for (PathEvent event : pathEvents) {
                out.writeInt(event.isThinPath ? 0 : 1);
                out.writeInt(event.isMove ? 0 : 1);
                out.writeFloat(event.x);
                out.writeFloat(event.y);
            }
        }

        public static final Parcelable.Creator<PaperSavedState> CREATOR
                = new Parcelable.Creator<PaperSavedState>() {
            public PaperSavedState createFromParcel(Parcel in) {
                return new PaperSavedState(in);
            }

            public PaperSavedState[] newArray(int size) {
                return new PaperSavedState[size];
            }
        };
    }

    private static class PathEvent {
        public final boolean isThinPath;
        public final boolean isMove;
        public final float x;
        public final float y;

        private PathEvent(boolean isThinPath, boolean isMove, float x, float y) {
            this.isThinPath = isThinPath;
            this.isMove = isMove;
            this.x = x;
            this.y = y;
        }
    }
}
