package com.linkedin.android.shaky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;

/**
 *  Drawable view on an image to show views in the screen to select
 */
public class Slate extends AppCompatImageView {
    private static final float THIN_STROKE_WIDTH = 12f;
    private final Paint paint = new Paint();
    private ArrayList<Subview> allSubViews = new ArrayList<>();
    private ArrayList<Subview> selectedSubViews = new ArrayList<>();
    private ArrayList<Subview> subViewsToDraw = new ArrayList<>();

    public Slate(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);

        paint.setColor(Color.CYAN);
        paint.setStrokeWidth(THIN_STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        Bundle savedState = new Bundle();
        savedState.putParcelable("superState", superState);
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle savedState = (Bundle) state;
            Parcelable superState = savedState.getParcelable("superState");
            super.onRestoreInstanceState(superState);
        } else {
            super.onRestoreInstanceState(state);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Subview subView : subViewsToDraw) {
            if (subView.isSelected) {
                paint.setColor(ColorUtils.setAlphaComponent(Color.RED, 50));
                paint.setStyle(Paint.Style.FILL);
            } else {
                paint.setColor(subView.color);
                paint.setStyle(Paint.Style.STROKE);
            }
            canvas.drawRect(subView.rectangle, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            for (Subview subView : subViewsToDraw) {
                if (subView.rectangle.contains(x, y)) {
                    subView.isSelected = !subView.isSelected;
                    if(subView.isSelected) {
                        selectedSubViews.add(subView);
                    } else {
                        selectedSubViews.remove(subView);
                    }
                    invalidate();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public void setupSubViews(@NonNull ArrayList<Subview> subViews) {
        allSubViews = new ArrayList<>();
        allSubViews.addAll(subViews);
        subViewsToDraw = allSubViews;
    }

    public void updateSelectedViews() {
        subViewsToDraw = selectedSubViews;
        invalidate();
    }

    /**
     * @return the current selection as a Bitmap
     */
    public Bitmap capture() {
        return Utils.capture(this);
    }
}
