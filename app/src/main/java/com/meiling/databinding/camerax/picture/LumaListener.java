package com.meiling.databinding.camerax.picture;

/**
 * Created by marisareimu@126.com on 2021-03-15  16:55
 * project DataBinding
 */

// todo 处理图片分析时，返回的回调，由于分析器的方法没有返回值，所以需要知道结果的话，需要通过回调接口来获取
public interface LumaListener {
    void analyzeResult(Double result);
}
