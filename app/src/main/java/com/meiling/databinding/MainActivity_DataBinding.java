package com.meiling.databinding;
/**
 * Created by marisareimu@126.com on 2021-03-11  16:24
 * project DataBinding
 */

import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.ViewDataBinding;

/**
 * Created by huangzhou@ulord.net on 2021-03-11  16:24
 * project DataBinding
 */
public class MainActivity_DataBinding extends ViewDataBinding {

    protected MainActivity_DataBinding(DataBindingComponent bindingComponent, View root, int localFieldCount) {
        super(bindingComponent, root, localFieldCount);
    }

    protected MainActivity_DataBinding(Object bindingComponent, View root, int localFieldCount) {
        super(bindingComponent, root, localFieldCount);
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object value) {
        return false;
    }

    @Override
    protected void executeBindings() {

    }

    @Override
    public void invalidateAll() {

    }

    @Override
    public boolean hasPendingBindings() {
        return false;
    }
}
