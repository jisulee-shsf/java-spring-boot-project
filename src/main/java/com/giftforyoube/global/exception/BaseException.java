package com.giftforyoube.global.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException{

    private BaseResponseStatus status;
    private String message;

    public BaseException(BaseResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }
}
