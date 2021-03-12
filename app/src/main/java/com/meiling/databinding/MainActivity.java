package com.meiling.databinding;

import android.os.Bundle;

import com.meiling.databinding.data.Data;
import com.meiling.databinding.databinding.ActivityMainBinding;

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
        activityMainBinding.tvName.setText("");
    }
}