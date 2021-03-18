package com.meiling.databinding.camerax.video;

import androidx.annotation.NonNull;

public class SmartSize {
    public int mLong = 0;
    public int mShort = 0;

    public SmartSize(int width, int height) {
        mLong = Math.max(width, height);
        mShort = Math.min(width, height);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("SmartSize(%d x %d)", mLong, mShort);
    }
}
