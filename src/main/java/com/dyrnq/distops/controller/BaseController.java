package com.dyrnq.distops.controller;

public class BaseController {

    /**
     * 从查询参数列表中安全地截取 count 查询参数（去掉尾部的 LIMIT 参数）
     * 防止 params.size() < 2 时 subList 抛 IndexOutOfBoundsException
     */
    protected Object[] countParams(java.util.List<?> params) {
        int end = params.size() >= 2 ? params.size() - 2 : params.size();
        return params.subList(0, end).toArray();
    }
}
