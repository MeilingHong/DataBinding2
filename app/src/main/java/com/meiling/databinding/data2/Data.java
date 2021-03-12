package com.meiling.databinding.data2;
/**
 * Created by marisareimu@126.com on 2021-03-11  15:56
 * project DataBinding
 */

import java.io.Serializable;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

/**
 * todo 相对而言该方式限制较大
 *
 * Created by huangzhou@ulord.net on 2021-03-11  15:56
 * project DataBinding
 */
public class Data extends BaseObservable implements Serializable {
    /**
     * todo 10、数据绑定
     *  BaseObservable
     *   notifyChange()会刷新全部值域
     *   notifyPropertyChanged() 只更新对应 BR 的 flag --- BR 的生成通过注释 @Bindable
     *
     * todo
     *   public 修饰的属性值，可以直接使用@Bindable注解
     *   private 修饰的，需要在getter方法上用@Bindable注解进行修饰
     */
    private String name;
    @Bindable// public
    public int age;

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyChange();//表示在设置name后将更新全部数据
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
