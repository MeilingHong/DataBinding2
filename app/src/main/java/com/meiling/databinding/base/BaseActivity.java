package com.meiling.databinding.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

/**
 * 数据绑定Activity基类
 *
 * Created by marisareimu@126.com on 2021-03-18  10:25
 * project DataBinding
 */
public abstract class BaseActivity<T> extends AppCompatActivity {
    protected T layoutBinding;

    protected abstract int layoutViewId();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutBinding = (T) DataBindingUtil.setContentView(this,layoutViewId());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
