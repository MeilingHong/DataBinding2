package com.meiling.databinding.camerax.video;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.google.common.util.concurrent.ListenableFuture;
import com.meiling.databinding.R;
import com.meiling.databinding.camerax.picture.LumaListener;
import com.meiling.databinding.camerax.picture.LuminosityAnalyzer;
import com.meiling.databinding.databinding.ActivityCameraxVideoBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class CameraXCaptureVideoActivity extends AppCompatActivity {
    private final String TAG = "AndroidRuntime";

    private ActivityCameraxVideoBinding activityCameraxVideoBinding;

    private ExecutorService cameraExecutor;
    private LocalBroadcastManager localBroadcastManager;
    private DisplayManager displayManager;

    private int mDisplayId = -1;

    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCameraxVideoBinding = DataBindingUtil.setContentView(this, R.layout.activity_camerax_video);

        initExecutor();
        initVolumeReceiver();
        initDisplayManager();
        initOutputPath();

        initSurface();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {//终止线程池
            cameraExecutor.shutdown();
        }
        if (localBroadcastManager != null) {// 重置广播接收
            localBroadcastManager.unregisterReceiver(volumeDownReceiver);
        }
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
    }

    //******************************************************************************************************************

    private void initExecutor() {// 给封装的CameraX来执行对应的拍照任务
        Log.w(TAG, "initExecutor");
        cameraExecutor = Executors.newSingleThreadExecutor();
    }


    private BroadcastReceiver volumeDownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getIntExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_UNKNOWN) == KeyEvent.KEYCODE_VOLUME_DOWN) {
                // 收到了，音量下键的点击
                Log.w(TAG, "onReceive");
                activityCameraxVideoBinding.cameraCaptureButton.performClick();
            }
        }
    };

    private void initVolumeReceiver() {
        Log.w(TAG, "initVolumeReceiver");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.EXTRA_KEY_EVENT);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(volumeDownReceiver, filter);
    }

    private DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {

        }

        @Override
        public void onDisplayRemoved(int displayId) {

        }

        @Override
        public void onDisplayChanged(int displayId) {
            Log.w(TAG, "onDisplayChanged(displayId):" + displayId + "---(mDisplayId)" + mDisplayId);
            if (displayId == mDisplayId) {
                if (activityCameraxVideoBinding != null && activityCameraxVideoBinding.preview != null &&
                        activityCameraxVideoBinding.preview.getDisplay() != null) {

                }
            }
        }
    };

    private void initDisplayManager() {
        Log.w(TAG, "initDisplayManager");
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(displayListener, null);
    }

    private void initOutputPath() {
        Log.w(TAG, "initOutputPath");
        /**
         * todo 当使用非data目录存储文件时，会存在一个访问隔离的问题，如果需要访问到其他文件，需要使用到MANAGE_EXTERNAL_STORAGE权限，否则将会导致使用File进行访问时，
         *  无法访问到该路径下的文件（文件实际存在，但权限隔离导致访问被拒绝）【open failed: EPERM (Operation not permitted)】
         */
        outputDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);// todo 保险起见，使用data目录进行访问【这种方式既保证的向上的兼容，后保证了向下的兼容】
        if (outputDirectory != null && outputDirectory.exists()) {

        } else {
            outputDirectory = getExternalFilesDir(null);
        }
    }

    private void initSurface() {
        activityCameraxVideoBinding.preview.post(new Runnable() {
            @Override
            public void run() {
                mDisplayId = activityCameraxVideoBinding.preview.getDisplay().getDisplayId();

            }
        });
        activityCameraxVideoBinding.preview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
//                Size previewSize = CameraSizes.getPreviewOutputSize(activityCameraxVideoBinding.preview.getDisplay(),);
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

}