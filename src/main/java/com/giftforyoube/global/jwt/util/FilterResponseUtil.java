package com.giftforyoube.global.jwt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class FilterResponseUtil {

    public static void sendFilterResponse(HttpServletResponse response,
                                          int statusCode,
                                          BaseResponseStatus status) throws IOException {
        BaseResponse<Void> baseResponse = new BaseResponse<>(status);
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(baseResponse));
    }
}