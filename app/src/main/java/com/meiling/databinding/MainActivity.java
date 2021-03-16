package com.meiling.databinding;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import com.meiling.databinding.data.Data;
import com.meiling.databinding.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

public class MainActivity extends AppCompatActivity {
    private Data data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // todo 6、 通过DataBindingUtil工具类，将Activity与对应的布局进行关联
        //  【ActivityMainBinding】类为在layout执行[Convert to data binding layout]时自动生成的，名称为布局文件名称按照驼峰命名法命名

        // todo 9、如果需要自己指定这个ViewDataBinding对象名称，需要在布局文件中进行声明
        //  <data class="CustomBinding">，其中class指定的名称即为自定义的ViewDataBinding名称
        ActivityMainBinding activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        data = new Data();
        data.setName("自定义名称2");
        // todo 7、而设置绑定的实体对象，是指定的<data>标签中对应的<variable>对象，名称是<variable>中指定的name对应的名称，
        //  这样就完成了布局到对应的Activity类的绑定关系以及绑定的对象的注入
        activityMainBinding.setNameEntity(data);

        // todo 8、ViewDataBinding对象可以使用  【.tvName[布局文件中，声明的值]】来直接获取对应的View组件，并进行对应的操作
        activityMainBinding.tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // todo 需要申请Camera权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CODE);
                } else {
//                    startActivity(new Intent(getApplicationContext(), CameraXCaptureImageActivity.class));
                    getCameraInfo();
                }
            }
        });
    }

    private final int PERMISSIONS_REQUEST_CODE = 10;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == (grantResults == null ? -1 : grantResults[0])) {
                // Take the user to the success fragment when permission is granted
                Toast.makeText(getApplicationContext(), "Permission request granted", Toast.LENGTH_LONG).show();
//                startActivity(new Intent(getApplicationContext(), CameraXCaptureImageActivity.class));
                getCameraInfo();
            } else {
                Toast.makeText(getApplicationContext(), "Permission request denied", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void getCameraInfo() {// 使用Camera2 API
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            if (cameraIdList != null && cameraIdList.length > 0) {
                for (String id : cameraIdList) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                    int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                    List<Integer> capabilitiesList = toArray(capabilities);
//                            Arrays.stream(capabilities).boxed().collect(Collectors.toList());// todo 该方法需要在API 24以及以上版本才可用【Android7 以及以上系统】
                    StreamConfigurationMap cameraConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    if (capabilitiesList.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                        Size[] sizeList = cameraConfig.getOutputSizes(MediaRecorder.class);
                        if (sizeList != null && sizeList.length > 0) {
                            for (Size size : sizeList) {
                                double secondsPerFrame = cameraConfig.getOutputMinFrameDuration(MediaRecorder.class, size) / 10000000.0;
                                int fps = secondsPerFrame > 0 ? (int) (1.0 / secondsPerFrame) : 0;
                                String fpsLabel = fps > 0 ? String.valueOf(fps) : "N/A";
                                Log.e("AndroidRuntime", id + "-" + lensOrientationString(orientation) + "-" + size.getWidth() + "-" + size.getHeight() + "-" + fpsLabel);
                            }
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> toArray(int[] list) {
        List<Integer> mList = new ArrayList<>();
        if (list != null && list.length > 0) {
            int size = list.length;
            for (int i = 0; i < size; i++) {
                mList.add(list[i]);
            }
        }
        return mList;
    }

    private String lensOrientationString(int value) {
        switch (value) {
            case CameraCharacteristics.LENS_FACING_BACK: {
                return "Back";
            }
            case CameraCharacteristics.LENS_FACING_FRONT: {
                return "Front";
            }
            case CameraCharacteristics.LENS_FACING_EXTERNAL: {
                return "External";
            }
            default: {
                return "Back";
            }
        }
    }
}