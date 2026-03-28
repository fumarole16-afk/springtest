package com.stockanalyzer.common;

import java.util.HashMap;
import java.util.Map;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private Map<String, Object> meta;

    private ApiResponse(boolean success, T data, Map<String, Object> meta) {
        this.success = success;
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, meta);
    }

    public static <T> ApiResponse<T> error(String message) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("error", message);
        return new ApiResponse<>(false, null, meta);
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public Map<String, Object> getMeta() { return meta; }
}
