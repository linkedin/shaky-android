package com.linkedin.android.shaky;


import androidx.annotation.Nullable;

/**
 * Callback interface for screenshot capture and data collection.
 */
public interface DataCollectionCallback {
    /**
     * Called when screenshot capture and data collection is complete.
     *
     * @param result the collected data and screenshot URI, or null if some failure occurred
     */
    void onDataCollected(@Nullable Result result);
}
