package com.giftforyoube.global.controller;

import com.giftforyoube.global.exception.BaseResponse;
import com.giftforyoube.global.exception.BaseResponseStatus;
import com.giftforyoube.global.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @DeleteMapping("/logout")
    public ResponseEntity<BaseResponse<?>> logout(HttpServletRequest request, HttpServletResponse response) {
        jwtUtil.logout(request, response);
        BaseResponse<Void> baseResponse = new BaseResponse<>(BaseResponseStatus.LOGOUT_SUCCESS);
        return ResponseEntity.ok(baseResponse);
    }
}