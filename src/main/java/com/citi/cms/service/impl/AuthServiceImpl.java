package com.citi.cms.service.impl;

import com.citi.cms.dto.request.LoginRequest;
import com.citi.cms.dto.response.LoginResponse;
import com.citi.cms.entity.User;
import com.citi.cms.repository.UserRepository;
import com.citi.cms.security.JwtTokenProvider;
import com.citi.cms.security.UserPrincipal;
import com.citi.cms.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Override
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            String jwt = tokenProvider.generateToken(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            User user = userRepository.findByUsername(userPrincipal.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUserId(user.getUserId());
            userInfo.setUsername(user.getUsername());
            userInfo.setEmail(user.getEmail());
            userInfo.setFirstName(user.getFirstName());
            userInfo.setLastName(user.getLastName());
            userInfo.setRoles(user.getRoles().stream()
                    .map(role -> role.getRoleCode())
                    .collect(Collectors.toSet()));

            return LoginResponse.builder()
                    .success(true)
                    .message("Authentication successful")
                    .token(jwt)
                    .user(userInfo)
                    .build();

        } catch (AuthenticationException e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsername());
            throw new RuntimeException("Invalid credentials");
        }
    }

    @Override
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    @Override
    public void logout(String token) {
        // In a production system, you might want to blacklist the token
        // For now, we'll just log the logout
        logger.info("User logged out with token: {}", token.substring(0, 10) + "...");
    }
}