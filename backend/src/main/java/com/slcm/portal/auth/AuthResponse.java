package com.slcm.portal.auth;
import com.slcm.portal.user.UserRole;
public record AuthResponse(String token, String username, UserRole role) {}
