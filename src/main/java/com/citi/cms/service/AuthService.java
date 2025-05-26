package com.citi.cms.service;

import com.citi.cms.dto.request.LoginRequest;
import com.citi.cms.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse authenticateUser(LoginRequest loginRequest);
    boolean validateToken(String token);
    void logout(String token);
}