package com.meiling.databinding.viewmodel;

import com.google.gson.Gson;

import java.io.Serializable;

import androidx.databinding.BaseObservable;

public class ToString extends BaseObservable implements Serializable {
    protected static Gson gson = new Gson();
    public String toString() {
        return gson.toJson(this);
    }
}
