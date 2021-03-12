package com.meiling.databinding;
/**
 * Created by marisareimu@126.com on 2021-03-11  17:15
 * project DataBinding
 */

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.meiling.databinding.databinding.FragmentMainBinding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentMainBinding fragmentMainBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        fragmentMainBinding.tvName2.setText("");
        return fragmentMainBinding.getRoot();
    }
}
