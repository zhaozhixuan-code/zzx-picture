package com.zzx.zzxpicturebackend.common;

import com.zzx.zzxpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局返回封装
 *
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;

    private T data;

    private String message;


    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
