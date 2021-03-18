package com.meiling.databinding.databind;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.meiling.databinding.R;
import com.meiling.databinding.base.BaseActivity;
import com.meiling.databinding.databinding.ActivityDataBindButtonBinding;
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

        /**
         * todo
         *  从实际测试上发现layout的设置的时间绑定并没有被回调
         */
        layoutBinding.tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.setName(new Random().nextInt() + "--随机修改绑定的Data中的值");
                Ulog.i(data.toString());
            }
        });
        layoutBinding.setUserPresenter(new ClickPresenter());//todo 如果没有设置这个，则无法进行相应的回调处理
    }

    public class ClickPresenter {
        public void doClick(View view, Data data) {
            Toast.makeText(DataBindButtonActivity.this, (data != null ? data.getName() : "空数据") + "---" + (view != null ? "doClick(View view,Data data)---View不为空" : "doClick(View view,Data data)---出入为空"), Toast.LENGTH_SHORT).show();
        }

        public void click(View view) {
            Toast.makeText(DataBindButtonActivity.this, (view != null ? "click---View不为空" : "click---出入为空"), Toast.LENGTH_SHORT).show();
        }
    }

}