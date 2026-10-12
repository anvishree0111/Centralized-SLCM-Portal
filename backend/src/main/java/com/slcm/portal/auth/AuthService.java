package com.slcm.portal.auth;

import com.slcm.portal.user.User;
import com.slcm.portal.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt){this.users=users;this.encoder=encoder;this.jwt=jwt;}
    public AuthResponse login(LoginRequest request) {
        User user=users.findByUsername(request.username()).filter(User::isEnabled).orElseThrow(()->new BadCredentialsException("Invalid username or password"));
        if(!encoder.matches(request.password(), user.getPasswordHash())) throw new BadCredentialsException("Invalid username or password");
        return new AuthResponse(jwt.issue(user), user.getUsername(), user.getRole());
    }
}
