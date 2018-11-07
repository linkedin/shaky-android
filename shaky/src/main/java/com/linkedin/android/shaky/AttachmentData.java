package com.linkedin.android.shaky;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class AttachmentData {

    final Uri uri;
    final String displayName;
    final boolean removable;

    AttachmentData(@NonNull Uri uri) {
        this(uri, null, false);
    }

    AttachmentData(@NonNull Uri uri, @Nullable String displayName, boolean removable) {
        this.uri = uri;
        this.displayName = displayName;
        this.removable = removable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AttachmentData that = (AttachmentData) o;

        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }


}
