package com.giftforyoube.global.jwt.util;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.constant.GrantType;
import org.springframework.util.StringUtils;


public class AuthorizationHeaderUtil {

    public static void validateAuthorization(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new BaseException(BaseResponseStatus.NOT_EXISTS_AUTHORIZATION);
        }

        String[] authorizations = authorizationHeader.split(" ");
        if (authorizations.length < 2 || (!GrantType.BEARER.getType().equals(authorizations[0]))) {
            throw new BaseException(BaseResponseStatus.NOT_VALID_BEARER_GRANT_TYPE);
        }
    }
}