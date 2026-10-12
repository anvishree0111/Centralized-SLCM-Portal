package com.slcm.portal.auth;
import com.slcm.portal.user.UserRole;
public record MeResponse(Long id, String username, UserRole role, boolean enabled) {}
