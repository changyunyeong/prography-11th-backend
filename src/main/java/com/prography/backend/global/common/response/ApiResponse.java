package com.prography.backend.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.prography.backend.global.common.error.ErrorCode;

public record ApiResponse<T>(
    boolean success,
    T data,
    @JsonInclude(JsonInclude.Include.ALWAYS) ErrorInfo error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorInfo(errorCode.getCode(), errorCode.getMessage()));
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message));
    }

    public record ErrorInfo(String code, String message) {
    }
}
