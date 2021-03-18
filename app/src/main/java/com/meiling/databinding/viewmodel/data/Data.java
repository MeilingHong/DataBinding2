package com.meiling.databinding.viewmodel.data;
/**
 * Created by marisareimu@126.com on 2021-03-11  15:56
 * project DataBinding
 */

import com.meiling.databinding.BR;
import com.meiling.databinding.viewmodel.ToString;

import androidx.databinding.Bindable;

/**
 * Created by huangzhou@ulord.net on 2021-03-11  15:56
 * project DataBinding
 */
public class Data extends ToString {
    @Bindable
    private String name;// todo 直接声明的值在修改时是不会通知View需要修改显示的
    @Bindable
    private String name1;

    private String name2;

    private String name3;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
        // todo 10、通知指定值进行更新，绑定该值的UI会在该值改变时进行变更
        //  【该方法需要配合@Bindable注解进行使用，当使用set方法时，使用BR.该属性名称，来通知UI需要变更该属性对应的值】
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
        notifyChange();// todo 11、通知使用该对象的全部值进行变更，绑定该对象的UI全部都会进行变更
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
        notifyPropertyChanged(BR.name1);//todo
    }

    public String getName3() {
        return name3;
    }

    public void setName3(String name3) {
        this.name3 = name3;
    }
}
