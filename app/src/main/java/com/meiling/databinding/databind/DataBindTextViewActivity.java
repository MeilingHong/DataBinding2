package com.meiling.databinding.databind;

import android.os.Bundle;
import android.view.View;

import com.meiling.databinding.R;
import com.meiling.databinding.base.BaseActivity;
import com.meiling.databinding.databinding.BindTextView;
import com.meiling.databinding.log.Ulog;
import com.meiling.databinding.viewmodel.data.Data;

import java.util.Random;

public class DataBindTextViewActivity extends BaseActivity<BindTextView> {
    private Data data;
    private com.meiling.databinding.viewmodel.data2.Data data2;
    @Override
    protected int layoutViewId() {
        return R.layout.activity_data_bind_textview;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // todo 6、 通过DataBindingUtil工具类，将Activity与对应的布局进行关联
        //  【ActivityMainBinding】类为在layout执行[Convert to data binding layout]时自动生成的，名称为布局文件名称按照驼峰命名法命名

        // todo 9、如果需要自己指定这个ViewDataBinding对象名称，需要在布局文件中进行声明
        //  <data class="CustomBinding">，其中class指定的名称即为自定义的ViewDataBinding名称
        data = new Data();
        data.setName("自定义（DataBindTextView）");
        data.setName1("自定义（setName1）");
        data.setName2("自定义（setName2）");
        data.setName3("自定义（setName3）");

        data2 = new com.meiling.databinding.viewmodel.data2.Data();
        data2.setName("Name");
        data2.setAge(20);
        // todo 7、而设置绑定的实体对象，是指定的<data>标签中对应的<variable>对象，名称是<variable>中指定的name对应的名称，
        //  这样就完成了布局到对应的Activity类的绑定关系以及绑定的对象的注入
        layoutBinding.setNameEntity(data);
        layoutBinding.setNameEntity2(data2);
        // todo 8、ViewDataBinding对象可以使用  【.tvName[布局文件中，声明的值]】来直接获取对应的View组件，并进行对应的操作
        layoutBinding.tvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.setName(new Random().nextInt() + "--随机修改绑定的Data中的值");
                Ulog.i(data.toString());
            }
        });

        layoutBinding.tvName1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.setName3(new Random().nextInt() + "-- setName3");
                data.setName2(new Random().nextInt() + "-- setName2");
                data.setName1(new Random().nextInt() + "-- setName1");
                data.notifyChange();// todo 12、也可通过ViewModel对象显示调用来通知UI需要进行变更
                Ulog.i(data.toString());
            }
        });

        layoutBinding.tvName2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.setName2(new Random().nextInt() + "-- setName2");
                Ulog.i(data.toString());
            }
        });

        layoutBinding.tvName3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data2.setName(new Random().nextInt() + "-- setName [data2]");
                Ulog.i(data.toString());
            }
        });
    }

}