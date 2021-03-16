package com.meiling.databinding.camerax;

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
import android.view.KeyEvent;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.google.common.util.concurrent.ListenableFuture;
import com.meiling.databinding.R;
import com.meiling.databinding.databinding.ActivityCameraxBinding;

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

public class CameraXCaptureImageActivity extends AppCompatActivity {
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
        outputDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);// todo 保险起见，使用data目录进行访问【这种方式既保证的向上的兼容，后保证了向下的兼容】
//        if (Build.VERSION.SDK_INT >= 29) {
//            outputDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        } else {
//            outputDirectory = Environment.getExternalStorageDirectory();// todo 【这种方式发现在执行时会出现异常，导致拍到的文件无法进行存储】
        /*
        2021-03-16 15:29:28.084 25247-25403/com.meiling.databinding E/AndroidRuntime: Photo capture failed: ${exception.message}
            androidx.camera.core.ImageCaptureException: Cannot save capture result to specified location
                at androidx.camera.core.ImageCapture.lambda$takePicture$5(ImageCapture.java:799)
                at androidx.camera.core.-$$Lambda$ImageCapture$wcrF0gZsLfYB9ZBrY3_kPHJuK5I.run(Unknown Source:2)
                at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
                at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
                at java.lang.Thread.run(Thread.java:764)
         */
//        }
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
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)// todo 可以修改参数，但最大模式将导致处理的时长边长
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)// ---- 实际修改后，发现并没有像想象的那样，文件像素更大
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)// todo 设置闪光灯模式
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
//                Log.i(TAG, "图片分析结果(计算的byte的平均值)：" + (result != null ? result.toString() : "null"));
            }
        }));

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            mCamera = cameraProvider.bindToLifecycle(
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
    private Camera mCamera = null;// 实际上，Camera在赋值后并没有起到什么作用
    private ProcessCameraProvider cameraProvider = null;

    private void doShutter() {// 执行拍照
        Log.w(TAG, "doShutter 拍摄点击---(lensFacing)" + lensFacing + "---前0  后1");

        // 创建输出文件
        File photoFile = new File(outputDirectory, new SimpleDateFormat(FILENAME, Locale.US).format(System.currentTimeMillis()) + PHOTO_EXTENSION);

        // 根据使用的是前/后摄像头，构建metadata
        ImageCapture.Metadata metadata = new ImageCapture.Metadata();
//        metadata.setReversedHorizontal(lensFacing == CameraSelector.LENS_FACING_FRONT);// 前置摄像头需要进行翻转
        metadata.setReversedHorizontal(false);// 前置摄像头需要进行翻转

        // 根据文件，以及metaData创建输出文件参数
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).setMetadata(metadata).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                Uri savedUri = output.getSavedUri() != null ? output.getSavedUri() : Uri.fromFile(photoFile);

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Update the gallery thumbnail with latest picture taken
//                    setGalleryThumbnail(savedUri)
                    // todo 有必要的话，这里加载文件，使得当前页面可以进行预览
                }

                // Implicit broadcasts will be ignored for devices running API level >= 24
                // so if you only target API level 24+ you can remove this statement
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri));
                }

                // If the folder selected is an external media directory, this is
                // unnecessary but otherwise other apps will not be able to access our
                // images unless we scan them using [MediaScannerConnection]
                String mimeType;
                if ("file".equals(savedUri.getScheme())) {
                    File temp = new File(savedUri.getPath());
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(temp.getName().substring(temp.getName().lastIndexOf(".") + 1));
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[]{temp.getAbsolutePath()}, new String[]{mimeType}, new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.e(TAG, "doShutter onScanCompleted ：" + path + "---" + (uri != null ? uri.getPath() : "null"));
                        }
                    });
                }

                shutSuccessFlash();// todo 如果在外面调用的话，将导致不论文件是否创建成功，都会出现闪一下的视觉效果，会给用户拍照成功的错觉
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception);
            }
        });


    }

    private void shutSuccessFlash(){
        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Display flash animation to indicate that photo was captured
            activityCameraxBinding.container.postDelayed(new Runnable() {// 快速的显示屏闪，表示拍照成功
                @Override
                public void run() {
                    activityCameraxBinding.container.setForeground(new ColorDrawable(Color.WHITE));
                    activityCameraxBinding.container.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            activityCameraxBinding.container.setForeground(null);
                        }
                    }, 50);
                }
            }, 100);
        }
    }

    private void setFlashMode(){
        imageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);// todo  似乎只开放了设置闪光灯模式的API，而对焦的话，似乎API直接就封装了自动对焦
    }

}