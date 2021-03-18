package com.meiling.databinding.databind;

import android.os.Bundle;
import android.view.View;

import com.meiling.databinding.R;
import com.meiling.databinding.base.BaseActivity;
import com.meiling.databinding.databinding.ActivityDataBindButtonBinding;
import com.meiling.databinding.databinding.ActivityDataBindEdittextBinding;
import com.meiling.databinding.log.Ulog;
import com.meiling.databinding.viewmodel.data.Data;

import java.util.Random;

public class DataBindButtonActivity extends BaseActivity<ActivityDataBindButtonBinding> {
    private Data data;

    @Override
    protected int layoutViewId() {
        return R.layout.activity_data_bind_button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // todo 这里相当于初始化一个ViewModel，并注入到绑定对象中，当操作这个ViewModel时，关联的View跟着一起改变
        data = new Data();
        data.setName("自定义（DataBindTextView）");
        layoutBinding.setNameEntity(data);


        layoutBinding.tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.setName(new Random().nextInt() + "--随机修改绑定的Data中的值");
                Ulog.i(data.toString());
            }
        });
    }

}