package com.citi.cms.dto.response;

import java.util.Set;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private UserInfo user;
    
    public static class UserInfo {
        private Long userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private Set<String> roles;
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public Set<String> getRoles() { return roles; }
        public void setRoles(Set<String> roles) { this.roles = roles; }
    }
    
    // Builder Pattern
    public static LoginResponse.Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private LoginResponse response = new LoginResponse();
        
        public Builder success(boolean success) {
            response.success = success;
            return this;
        }
        
        public Builder message(String message) {
            response.message = message;
            return this;
        }
        
        public Builder token(String token) {
            response.token = token;
            return this;
        }
        
        public Builder user(UserInfo user) {
            response.user = user;
            return this;
        }
        
        public LoginResponse build() {
            return response;
        }
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }
}
