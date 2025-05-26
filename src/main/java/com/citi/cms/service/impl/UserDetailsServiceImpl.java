package com.citi.cms.service.impl;

import com.citi.cms.entity.User;
import com.citi.cms.repository.UserRepository;
import com.citi.cms.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        logger.debug("User found: {} with {} roles", user.getUsername(), user.getRoles().size());
        
        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        logger.debug("Loading user by ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with id: {}", id);
                    return new UsernameNotFoundException("User not found with id: " + id);
                });

        logger.debug("User found by ID: {} - {}", id, user.getUsername());
        
        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserByUserId(Long userId) {
        logger.debug("Loading user by userId: {}", userId);
        
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    logger.error("User not found with userId: {}", userId);
                    return new UsernameNotFoundException("User not found with userId: " + userId);
                });

        logger.debug("User found by userId: {} - {}", userId, user.getUsername());
        
        return UserPrincipal.create(user);
    }
}