package com.linkedin.android.shaky;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Subview implements Parcelable {
    @NonNull public final Rect rectangle;
    @Nullable public final String ownerEmail;

    @ColorInt public final int color;

    public boolean isSelected;


    public Subview(@NonNull Rect rectangle, @Nullable String ownerEmail, @ColorInt int color, boolean isSelected) {
        this.rectangle = rectangle;
        this.color = color;
        this.isSelected = isSelected;
        this.ownerEmail = ownerEmail;
    }

    private Subview(Parcel in) {
        rectangle = in.readParcelable(Rect.class.getClassLoader());
        color = in.readInt();
        isSelected = in.readInt() == 0;
        ownerEmail = in.readString();
    }

    public static final Creator<Subview> CREATOR = new Creator<Subview>() {
        @Override
        public Subview createFromParcel(Parcel in) {
            return new Subview(in);
        }

        @Override
        public Subview[] newArray(int size) {
            return new Subview[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(rectangle, i);
        parcel.writeInt(color);
        parcel.writeInt(isSelected ? 0 : 1);
        parcel.writeString(ownerEmail);
    }
}
