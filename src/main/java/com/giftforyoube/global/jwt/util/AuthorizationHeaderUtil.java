//package com.giftforyoube.global.jwt.util;
//
//import com.giftforyoube.global.exception.BaseException;
//import com.giftforyoube.global.exception.BaseResponseStatus;
//import com.giftforyoube.global.jwt.constant.GrantType;
//import org.springframework.util.StringUtils;
//
//public class AuthorizationHeaderUtil {
//
//    public static void validateAuthorization(String authorizationHeader) {
//        // Authorization 헤더 존재 여부 확인
//        if (!StringUtils.hasText(authorizationHeader)) {
//            throw new BaseException(BaseResponseStatus.AUTHORIZATION_HEADER_NOT_FOUND);
//        }
//
//        // Bearer 타입 존재 여부 확인
//        String[] authorizations = authorizationHeader.split(" ");
//        if (authorizations.length < 2 || (!GrantType.BEARER.getType().equals(authorizations[0]))) {
//            throw new BaseException(BaseResponseStatus.INVALID_BEARER_GRANT_TYPE);
//        }
//    }
//}