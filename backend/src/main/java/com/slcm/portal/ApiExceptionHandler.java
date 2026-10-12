package com.slcm.portal;
import jakarta.validation.ConstraintViolationException; import org.springframework.http.*; import org.springframework.security.authentication.BadCredentialsException; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.time.Instant; import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler {
 record ErrorBody(Instant timestamp,int status,String error,String message,String path){}
 @ExceptionHandler({BadCredentialsException.class}) ResponseEntity<ErrorBody> bad(BadCredentialsException e, jakarta.servlet.http.HttpServletRequest r){return error(401,"Unauthorized","Invalid username or password",r);}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException e,jakarta.servlet.http.HttpServletRequest r){String msg=e.getBindingResult().getFieldErrors().stream().map(x->x.getDefaultMessage()).findFirst().orElse("Invalid request");return error(400,"Bad Request",msg,r);}
  @ExceptionHandler(ConstraintViolationException.class) ResponseEntity<ErrorBody> constraint(ConstraintViolationException e,jakarta.servlet.http.HttpServletRequest r){return error(400,"Bad Request","Invalid request",r);}
  @ExceptionHandler(ResponseStatusException.class) ResponseEntity<ErrorBody> status(ResponseStatusException e,jakarta.servlet.http.HttpServletRequest r){int status=e.getStatusCode().value();String message=status>=500?"Request could not be completed":(e.getReason()==null?"Request failed":e.getReason());return error(status,HttpStatus.valueOf(status).getReasonPhrase(),message,r);}
  @ExceptionHandler(Exception.class) ResponseEntity<ErrorBody> unexpected(Exception e,jakarta.servlet.http.HttpServletRequest r){return error(500,"Internal Server Error","Request could not be completed",r);}
 private ResponseEntity<ErrorBody> error(int s,String e,String m,jakarta.servlet.http.HttpServletRequest r){return ResponseEntity.status(s).body(new ErrorBody(Instant.now(),s,e,m,r.getRequestURI()));}
}
