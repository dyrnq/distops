package com.dyrnq.distops.controller;

import org.noear.solon.annotation.Note;
import org.noear.solon.core.handle.Result;

public class PageResult<T> extends Result<T> {
    private Number total;

    public PageResult(T data, Number total) {
        super(data);
        this.total = total;
    }

    public PageResult() {
        super();
    }

    public PageResult(T data) {
        super(data);
    }


    public PageResult(int code, String description) {
        super(code, description);
    }

    public PageResult(int code, String description, T data) {
        super(code, description, data);
    }

    @Note("成功的结果")
    public static <T> PageResult<T> succeed(T data, Number total) {
        return new PageResult<>(data, total);
    }

    public static <T> PageResult<T> failure(String description) {
        return new PageResult<>(FAILURE_CODE, description);
    }

    public Number getTotal() {
        return total;
    }

    public void setTotal(Number total) {
        this.total = total;
    }
}
