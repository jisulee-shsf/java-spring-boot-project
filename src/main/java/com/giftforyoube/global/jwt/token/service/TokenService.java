package com.giftforyoube.global.jwt.token.service;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.Date;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenService {

    private final TokenManager tokenManager;

    public void issueAccessToken(HttpServletRequest httpServletRequest,
                                 HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
        String token = tokenManager.getTokenFromRequest(httpServletRequest);
        String tokenValue = tokenManager.substringToken(token);

        if (tokenManager.isRefreshTokenValid(tokenValue)) {
            Claims tokenClaims = tokenManager.getTokenClaims(tokenValue);
            String email = (String) tokenClaims.get("email");
            Date accessTokenExpireTime = tokenManager.createAccessTokenExpireTime();

            String accessToken = tokenManager.createAccessToken(email, accessTokenExpireTime);
            Cookie jwtCookie = tokenManager.addTokenToCookie(accessToken);
            httpServletResponse.addCookie(jwtCookie);
        } else {
            throw new BaseException(BaseResponseStatus.REFRESH_TOKEN_EXPIRED);
        }
    }
}