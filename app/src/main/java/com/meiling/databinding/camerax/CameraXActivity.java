package com.meiling.databinding.camerax;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import com.google.common.util.concurrent.ListenableFuture;
import com.meiling.databinding.R;
import com.meiling.databinding.databinding.ActivityCameraxBinding;

import java.io.File;
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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class CameraXActivity extends AppCompatActivity {
    private final String TAG = "AndroidRuntime";

    private ActivityCameraxBinding activityCameraxBinding;

    private ExecutorService cameraExecutor;
    private LocalBroadcastManager localBroadcastManager;
    private DisplayManager displayManager;

    private int mDisplayId = -1;

    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCameraxBinding = DataBindingUtil.setContentView(this, R.layout.activity_camerax);

        initExecutor();
        initVolumeReceiver();
        initDisplayManager();
        initOutputPath();

        activityCameraxBinding.preview.post(new Runnable() {
            @Override
            public void run() {
                mDisplayId = activityCameraxBinding.preview.getDisplay().getDisplayId();

                updateCameraUi();// onCreate preview Post

                setUpCamera();// onCreate preview Post
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Redraw the camera UI controls
        updateCameraUi(); // onConfigurationChanged

        // Enable or disable switching between cameras
        updateCameraSwitchButton();// onConfigurationChanged
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
                activityCameraxBinding.cameraCaptureButton.performClick();
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
                if (activityCameraxBinding != null && activityCameraxBinding.preview != null && activityCameraxBinding.preview.getDisplay() != null) {
                    if (imageCapture != null) imageCapture.setTargetRotation(activityCameraxBinding.preview.getDisplay().getRotation());
                    if (imageAnalyzer != null) imageAnalyzer.setTargetRotation(activityCameraxBinding.preview.getDisplay().getRotation());
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
//        outputDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        outputDirectory = getExternalFilesDir(null);// todo 保险起见，使用data目录进行访问
        if (outputDirectory != null && outputDirectory.exists()) {

        } else {
            outputDirectory = getExternalFilesDir(null);
        }
    }

    //******************************************************************************************************************
    private void setUpCamera() {
        Log.w(TAG, "setUpCamera");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.w(TAG, "setUpCamera  addListener");
                    cameraProvider = cameraProviderFuture.get();

                    if (hasBackCamera()) {
                        lensFacing = CameraSelector.LENS_FACING_BACK;
                    } else if (hasFrontCamera()) {
                        lensFacing = CameraSelector.LENS_FACING_FRONT;
                    } else {
                        throw new IllegalStateException("Back and front camera are unavailable");
                    }
                    // Enable or disable switching between cameras
                    updateCameraSwitchButton(); // setUpCamera
                    // Build and bind the camera use cases
                    bindCameraUseCases();// setUpCamera addListener

                    Log.w(TAG, "setUpCamera  addListener --- End");
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(getApplicationContext()));
    }

    //******************************************************************************************************************

    private int lensFacing = CameraSelector.LENS_FACING_BACK;// 后置摄像

    private void bindCameraUseCases() {
        Log.w(TAG, "bindCameraUseCases");
        DisplayMetrics metrics = new DisplayMetrics();
        if (activityCameraxBinding != null && activityCameraxBinding.preview != null && activityCameraxBinding.preview.getDisplay() != null) {
            activityCameraxBinding.preview.getDisplay().getRealMetrics(metrics);// 获取屏幕的宽高
        }
        Log.w(TAG, "bindCameraUseCases RealMetrics:(width)" + metrics.widthPixels + "---(height)" + metrics.heightPixels);
        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);// 计算获取比例
        Log.w(TAG, "bindCameraUseCases  屏幕比例：" + screenAspectRatio + "--- AspectRatio.RATIO_4_3:" + AspectRatio.RATIO_4_3
                + "--- AspectRatio.RATIO_16_9:" + AspectRatio.RATIO_16_9);
        int rotation = 0;
        if (activityCameraxBinding != null && activityCameraxBinding.preview != null && activityCameraxBinding.preview.getDisplay() != null) {
            rotation = activityCameraxBinding.preview.getDisplay().getRotation();//得到预览View的旋转角度
            Log.w(TAG, "bindCameraUseCases （旋转角度）：" + rotation);
        }
        if (this.cameraProvider != null) {
//            ProcessCameraProvider cameraProvider = this.cameraProvider;//
        } else {
            throw new IllegalStateException("Camera initialization failed.");
        }

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        preview = new Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)// 可以修改参数，但最大模式将导致处理的时长边长
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer = new ImageAnalysis.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, new LuminosityAnalyzer(new LumaListener() {
            @Override
            public void analyzeResult(Double result) {
                Log.i(TAG, "图片分析结果(计算的byte的平均值)：" + (result != null ? result.toString() : "null"));
            }
        }));

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer);
            Log.w(TAG, "bindCameraUseCases  赋值Camera");
            // Attach the viewfinder's surface provider to preview use case
            if (activityCameraxBinding != null && activityCameraxBinding.preview != null && activityCameraxBinding.preview.getSurfaceProvider() != null) {
                preview.setSurfaceProvider(activityCameraxBinding.preview.getSurfaceProvider());
                Log.w(TAG, "bindCameraUseCases  设置预览");
            }
            Log.w(TAG, "bindCameraUseCases  ---  End");
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    //******************************************************************************************************************

    private int aspectRatio(int width, int height) {
        Log.w(TAG, "updateCameraUi 计算屏幕比例");
        double previewRatio = 1.0 * Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    //******************************************************************************************************************

    private void updateCameraUi() {
        Log.w(TAG, "updateCameraUi");
        // todo 启动一个线程，读取最近的存储的文件，并展示最新的一个【kotlin使用的是专有的类】，Java考虑可以使用RxJava方式来执行

        activityCameraxBinding.cameraSwitchButton.setEnabled(false);
        activityCameraxBinding.cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 切换
                Log.w(TAG, "updateCameraUi  切换摄像头点击");
                if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                } else {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }
                bindCameraUseCases();// updateCameraUi   切换摄像头点击
            }
        });

        activityCameraxBinding.cameraCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "updateCameraUi  拍照点击");
                doShutter();// 执行拍照
            }
        });
    }

    private void updateCameraSwitchButton() {
        Log.w(TAG, "updateCameraSwitchButton");
        if (activityCameraxBinding != null && activityCameraxBinding.cameraSwitchButton != null) {
            activityCameraxBinding.cameraSwitchButton.setEnabled(hasBackCamera() && hasFrontCamera());
        }
    }

    /**
     * Returns true if the device has an available back camera. False otherwise
     */
    private boolean hasBackCamera() {
        try {
            return cameraProvider != null ? cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) : false;
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns true if the device has an available front camera. False otherwise
     */
    private boolean hasFrontCamera() {
        try {
            return cameraProvider != null ? cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) : false;
        } catch (CameraInfoUnavailableException e) {
            e.printStackTrace();
            return false;
        }
    }

    //******************************************************************************************************************
    private final String FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS";
    private final String PHOTO_EXTENSION = ".jpg";
    private final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private final double RATIO_16_9_VALUE = 16.0 / 9.0;

    private Preview preview = null;
    private ImageCapture imageCapture = null;
    private ImageAnalysis imageAnalyzer = null;
    private Camera camera = null;
    private ProcessCameraProvider cameraProvider = null;

    private void doShutter() {// 执行拍照

    }
}