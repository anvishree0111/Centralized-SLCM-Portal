package com.slcm.portal;
import com.slcm.portal.auth.*; import com.slcm.portal.user.User; import com.slcm.portal.user.UserRepository; import jakarta.validation.Valid; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService auth; private final UserRepository users;
 public AuthController(AuthService auth,UserRepository users){this.auth=auth;this.users=users;}
 @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request){return auth.login(request);}
  @GetMapping("/me") public MeResponse me(Authentication authentication){User u=users.findByUsername(authentication.getName()).orElseThrow();return new MeResponse(u.getId(),u.getUsername(),u.getRole(),u.isEnabled());}
}
