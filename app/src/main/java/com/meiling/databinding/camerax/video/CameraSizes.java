package com.meiling.databinding.camerax.video;

import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 相应的工具类【Camera2拍摄视频用】
 */
public class CameraSizes {
    private static final SmartSize SIZE_1080P = new SmartSize(1920, 1080);

    public static Size getPreviewOutputSize(Display display, CameraCharacteristics characteristics, Class targetClass, Integer format) {
        // Find which is smaller: screen or 1080p
        SmartSize screenSize = getDisplaySmartSize(display);
        boolean hdScreen = screenSize.mLong >= SIZE_1080P.mLong || screenSize.mShort >= SIZE_1080P.mShort;
        SmartSize maxSize = hdScreen ? SIZE_1080P : screenSize;// 当屏幕超过1080P时，取1080P

        // If image format is provided, use it to determine supported sizes; else use target class
        StreamConfigurationMap config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (format == null) {
            assert StreamConfigurationMap.isOutputSupportedFor(targetClass);
        } else {
            assert (config.isOutputSupportedFor(format));
        }
        Size[] allSizes = (format == null) ? config.getOutputSizes(targetClass) : config.getOutputSizes(format);

        // Get available sizes and sort them by area from largest to smallest【按照面积从大到小排序】
        List<SmartSize> validSizes = new ArrayList<>();
        if (allSizes != null && allSizes.length > 0) {
            int size = allSizes.length;
            for (int i = 0; i < size; i++) {
                if (allSizes[i] != null && allSizes[i].getWidth() > 0 && allSizes[i].getHeight() > 0) {// 保证数据有效性
                    validSizes.add(new SmartSize(allSizes[i].getWidth(), allSizes[i].getHeight()));
                }
            }
            Collections.sort(validSizes, new Comparator<SmartSize>() {
                @Override
                public int compare(SmartSize left, SmartSize right) {
                    if (left.equals(right)) {
                        return 0;
                    } else if (left.mLong * left.mShort > right.mLong * right.mShort) {
                        return -1;
                    } else if (left.mLong * left.mShort < right.mLong * right.mShort) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }

        if (validSizes != null && validSizes.size() > 0) {
            int size = validSizes.size();
            for (int i = 0; i < size; i++) {
                if (validSizes.get(i).mLong < maxSize.mLong && validSizes.get(i).mShort < maxSize.mShort) {
                    return new Size(validSizes.get(i).mLong, validSizes.get(i).mShort);
                }
            }
        }
        return null;
    }


    public static SmartSize getDisplaySmartSize(Display display) {
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        return new SmartSize(outPoint.x, outPoint.y);
    }
}
