package com.meiling.databinding.camerax;
/**
 * Created by marisareimu@126.com on 2021-03-15  16:29
 * project DataBinding
 */

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

/**
 * Created by huangzhou@ulord.net on 2021-03-15  16:29
 * project DataBinding
 */
public class LuminosityAnalyzer implements ImageAnalysis.Analyzer {

    private int frameRateWindow = 8;
    private ArrayDeque<Long> frameTimestamps = new ArrayDeque<Long>(5);
    private ArrayList<LumaListener> listeners = new ArrayList<LumaListener>();
    private Long lastAnalyzedTimestamp = 0L;
    private Double framesPerSecond = -1.0;

    public LuminosityAnalyzer(LumaListener listener) {
        listeners.add(listener);
    }

    /**
     * Used to add listeners that will be called with each luma computed
     */
    public void onFrameAnalyzed(LumaListener listener) {
        listeners.add(listener);
    }

    /**
     * Helper extension function used to extract a byte array from an image plane buffer
     */
    private byte[] toByteArray(ByteBuffer byteBuffer) {
        byteBuffer.rewind();    // Rewind the buffer to zero
        byte[] data = new byte[byteBuffer.remaining()];
        byteBuffer.get(data);   // Copy the buffer into a byte array
        return data; // Return the byte array
    }

    /**
     * Analyzes an image to produce a result.
     *
     * <p>The caller is responsible for ensuring this analysis method can be executed quickly
     * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
     * images will not be acquired and analyzed.
     *
     * <p>The image passed to this method becomes invalid after this method returns. The caller
     * should not store external references to this image, as these references will become
     * invalid.
     *
     * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
     *              call image.close() on received images when finished using them. Otherwise, new images
     *              may not be received or the camera may stall, depending on back pressure setting.
     */
    @Override
    public void analyze(@NonNull ImageProxy image) {
        // If there are no listeners attached, we don't need to perform analysis
        if (listeners.isEmpty()) {
            image.close();
            return;
        }

        // Keep track of frames analyzed
        long currentTime = System.currentTimeMillis();
        frameTimestamps.push(currentTime);

        // Compute the FPS using a moving average
        while (frameTimestamps.size() >= frameRateWindow) {
            frameTimestamps.removeLast();
        }
        Long timestampFirst = frameTimestamps.peekFirst() != null ? frameTimestamps.peekFirst() : currentTime;
        Long timestampLast = frameTimestamps.peekLast() != null ? frameTimestamps.peekLast() : currentTime;
        framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                frameTimestamps.size() < 1 ? 1.0 : (frameTimestamps.size() * 1.0)) * 1000.0;

        // Analysis could take an arbitrarily long amount of time
        // Since we are running in a different thread, it won't stall other use cases
        if (frameTimestamps.peekFirst() != null) {
            lastAnalyzedTimestamp = frameTimestamps.peekFirst();
        } else {
            throw new NoSuchElementException();
        }

        // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();

        // Extract image data from callback object
        byte[] data = toByteArray(buffer);

        // Convert the data into an array of pixel values ranging 0-255
        int size = data.length;
        byte[] pixels = new byte[data.length];
        for (int i = 0; i < size; i++) {
            pixels[i] = (byte) (data[i] & 0xFF);
        }

        // Compute average luminance for the image
        Double luma = countAverage(pixels);// 计算平均值

        // Call all listeners with new value
        for (LumaListener temp : listeners) {
            temp.analyzeResult(luma);
        }
        image.close();
    }

    private Double countAverage(byte[] pixels) {
        if (pixels.length > Integer.MAX_VALUE) {
            return null;
        }
        double sum = 0.0;
        int count = 0;
        for (byte temp : pixels) {
            sum += temp;
            count++;
            if (count >= Integer.MAX_VALUE) {
                return null;//这里的处理想来想去，还是返回空好了，不然大图在分析处理时肯定会有问题
            }
        }
        return count == 0 ? null : (sum / count);
    }
}
