package com.meiling.databinding.camerax.picture;

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
        if (cameraExecutor != null) {//???????????????
            cameraExecutor.shutdown();
        }
        if (localBroadcastManager != null) {// ??????????????????
            localBroadcastManager.unregisterReceiver(volumeDownReceiver);
        }
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
    }

    //******************************************************************************************************************

    private void initExecutor() {// ????????????CameraX??????????????????????????????
        Log.w(TAG, "initExecutor");
        cameraExecutor = Executors.newSingleThreadExecutor();
    }


    private BroadcastReceiver volumeDownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getIntExtra(Intent.EXTRA_KEY_EVENT, KeyEvent.KEYCODE_UNKNOWN) == KeyEvent.KEYCODE_VOLUME_DOWN) {
                // ?????????????????????????????????
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
         * todo ????????????data??????????????????????????????????????????????????????????????????????????????????????????????????????????????????MANAGE_EXTERNAL_STORAGE?????????????????????????????????File??????????????????
         *  ??????????????????????????????????????????????????????????????????????????????????????????????????????open failed: EPERM (Operation not permitted)???
         */
        outputDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);// todo ?????????????????????data?????????????????????????????????????????????????????????????????????????????????????????????
//        if (Build.VERSION.SDK_INT >= 29) {
//            outputDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        } else {
//            outputDirectory = Environment.getExternalStorageDirectory();// todo ?????????????????????????????????????????????????????????????????????????????????????????????
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

    private int lensFacing = CameraSelector.LENS_FACING_BACK;// ????????????

    private void bindCameraUseCases() {
        Log.w(TAG, "bindCameraUseCases");
        DisplayMetrics metrics = new DisplayMetrics();
        if (activityCameraxBinding != null && activityCameraxBinding.preview != null && activityCameraxBinding.preview.getDisplay() != null) {
            activityCameraxBinding.preview.getDisplay().getRealMetrics(metrics);// ?????????????????????
        }
        Log.w(TAG, "bindCameraUseCases RealMetrics:(width)" + metrics.widthPixels + "---(height)" + metrics.heightPixels);
        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);// ??????????????????
        Log.w(TAG, "bindCameraUseCases  ???????????????" + screenAspectRatio + "--- AspectRatio.RATIO_4_3:" + AspectRatio.RATIO_4_3
                + "--- AspectRatio.RATIO_16_9:" + AspectRatio.RATIO_16_9);
        int rotation = 0;
        if (activityCameraxBinding != null && activityCameraxBinding.preview != null && activityCameraxBinding.preview.getDisplay() != null) {
            rotation = activityCameraxBinding.preview.getDisplay().getRotation();//????????????View???????????????
            Log.w(TAG, "bindCameraUseCases ?????????????????????" + rotation);
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
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)// todo ??????????????????????????????????????????????????????????????????
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)// ---- ????????????????????????????????????????????????????????????????????????
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)// todo ?????????????????????
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
//                Log.i(TAG, "??????????????????(?????????byte????????????)???" + (result != null ? result.toString() : "null"));
            }
        }));

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll();

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            mCamera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer);
            Log.w(TAG, "bindCameraUseCases  ??????Camera");
            // Attach the viewfinder's surface provider to preview use case
            if (activityCameraxBinding != null && activityCameraxBinding.preview != null && activityCameraxBinding.preview.getSurfaceProvider() != null) {
                preview.setSurfaceProvider(activityCameraxBinding.preview.getSurfaceProvider());
                Log.w(TAG, "bindCameraUseCases  ????????????");
            }
            Log.w(TAG, "bindCameraUseCases  ---  End");
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    //******************************************************************************************************************

    private int aspectRatio(int width, int height) {
        Log.w(TAG, "updateCameraUi ??????????????????");
        double previewRatio = 1.0 * Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    //******************************************************************************************************************

    private void updateCameraUi() {
        Log.w(TAG, "updateCameraUi");
        // todo ?????????????????????????????????????????????????????????????????????????????????kotlin??????????????????????????????Java??????????????????RxJava???????????????

        activityCameraxBinding.cameraSwitchButton.setEnabled(false);
        activityCameraxBinding.cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ??????
                Log.w(TAG, "updateCameraUi  ?????????????????????");
                if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                } else {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }
                bindCameraUseCases();// updateCameraUi   ?????????????????????
            }
        });

        activityCameraxBinding.cameraCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG, "updateCameraUi  ????????????");
                doShutter();// ????????????
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
    private Camera mCamera = null;// ????????????Camera???????????????????????????????????????
    private ProcessCameraProvider cameraProvider = null;

    private void doShutter() {// ????????????
        Log.w(TAG, "doShutter ????????????---(lensFacing)" + lensFacing + "---???0  ???1");

        // ??????????????????
        File photoFile = new File(outputDirectory, new SimpleDateFormat(FILENAME, Locale.US).format(System.currentTimeMillis()) + PHOTO_EXTENSION);

        // ?????????????????????/?????????????????????metadata
        ImageCapture.Metadata metadata = new ImageCapture.Metadata();
//        metadata.setReversedHorizontal(lensFacing == CameraSelector.LENS_FACING_FRONT);// ?????????????????????????????????
        metadata.setReversedHorizontal(false);// ?????????????????????????????????

        // ?????????????????????metaData????????????????????????
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).setMetadata(metadata).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                Uri savedUri = output.getSavedUri() != null ? output.getSavedUri() : Uri.fromFile(photoFile);

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Update the gallery thumbnail with latest picture taken
//                    setGalleryThumbnail(savedUri)
                    // todo ???????????????????????????????????????????????????????????????????????????
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
                            Log.e(TAG, "doShutter onScanCompleted ???" + path + "---" + (uri != null ? uri.getPath() : "null"));
                        }
                    });
                }

                shutSuccessFlash();// todo ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
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
            activityCameraxBinding.container.postDelayed(new Runnable() {// ??????????????????????????????????????????
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
        imageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);// todo  ??????????????????????????????????????????API???????????????????????????API??????????????????????????????
    }

    private void doFocus() {// ??????????????????????????????
        if(mCamera!=null){
//            mCamera.getCameraControl().startFocusAndMetering(new FocusMeteringAction.Builder().);
        }
    }

    private void setZoom(){// ????????????
        if(mCamera!=null){
            mCamera.getCameraControl().setZoomRatio(mCamera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio());// todo ???????????????????????????????????????????????????????????????????????????
            mCamera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
            mCamera.getCameraInfo().getZoomState().getValue().getMinZoomRatio();

            mCamera.getCameraControl().setLinearZoom(1);//todo ??? 0 ~ 1 FLoat???

            mCamera.getCameraInfo().getZoomState().getValue().getLinearZoom();
            mCamera.getCameraInfo().getZoomState().getValue().getZoomRatio();
        }
    }

}