package com.betmatrix.theonex.netty.entity;

import lombok.Data;

import java.util.Set;

/**
 * Created by junior on 13:58 2018/1/16.
 */
@Data
public class SubsricbeInfo {
    /**
     * 1:订阅; 2:取消订阅
     */
    private Integer command;

    /**
     * 比赛Id
     */
    private Set<String> matches;

}
