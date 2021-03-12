package com.meiling.databinding.data;
/**
 * Created by marisareimu@126.com on 2021-03-11  15:56
 * project DataBinding
 */

import java.io.Serializable;

/**
 * Created by huangzhou@ulord.net on 2021-03-11  15:56
 * project DataBinding
 */
public class Data implements Serializable {
    private String name;
    private String name2;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }
}
