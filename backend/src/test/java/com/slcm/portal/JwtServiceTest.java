package com.slcm.portal;
import com.slcm.portal.auth.JwtService; import com.slcm.portal.user.*; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class JwtServiceTest {
 @Test void issuesAndRejectsTamperedToken(){var service=new JwtService("12345678901234567890123456789012",60); User u=mock(User.class);when(u.getUsername()).thenReturn("student.demo");when(u.getRole()).thenReturn(UserRole.STUDENT);String token=service.issue(u);assertTrue(service.valid(token));assertEquals("student.demo",service.username(token));assertFalse(service.valid(token+"x"));}
 @Test void rejectsExpiredToken(){var service=new JwtService("12345678901234567890123456789012",-1); User u=mock(User.class);when(u.getUsername()).thenReturn("student.demo");when(u.getRole()).thenReturn(UserRole.STUDENT);assertFalse(service.valid(service.issue(u)));}
}
