package com.levelup.levelup.auth;

import com.levelup.levelup.auth.dto.AuthResponse;
import com.levelup.levelup.auth.dto.LoginRequest;
import com.levelup.levelup.auth.dto.RegisterRequest;
import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(
      @Valid @RequestBody RegisterRequest request,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
    return sessionResponse(authService.register(request, userAgent));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @Valid @RequestBody LoginRequest request,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
    return sessionResponse(authService.login(request, userAgent));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @CookieValue(name = AuthService.REFRESH_COOKIE, required = false) String refreshToken,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw ApiException.unauthorized("No hay una sesión activa.");
    }
    return sessionResponse(authService.refresh(refreshToken, userAgent));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = AuthService.REFRESH_COOKIE, required = false) String refreshToken) {
    authService.logout(refreshToken);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
        .build();
  }

  private ResponseEntity<AuthResponse> sessionResponse(AuthService.Session session) {
    AuthResponse body = new AuthResponse(session.accessToken(), UserResponse.from(session.user()));
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE, authService.refreshCookie(session.refreshToken()).toString())
        .body(body);
  }
}
