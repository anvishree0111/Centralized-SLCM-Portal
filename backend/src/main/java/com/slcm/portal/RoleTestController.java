package com.slcm.portal;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/test") public class RoleTestController {
 @GetMapping("/student") @PreAuthorize("hasRole('STUDENT')") public java.util.Map<String,String> student(){return java.util.Map.of("role","STUDENT");}
 @GetMapping("/admin") @PreAuthorize("hasRole('ADMIN')") public java.util.Map<String,String> admin(){return java.util.Map.of("role","ADMIN");}
}
