package com.levelup.levelup.auth;

import com.levelup.levelup.auth.dto.LoginRequest;
import com.levelup.levelup.auth.dto.RegisterRequest;
import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.config.JwtProperties;
import com.levelup.levelup.user.User;
import com.levelup.levelup.user.UserRepository;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  public static final String REFRESH_COOKIE = "refresh_token";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final JwtProperties jwtProperties;
  private final boolean cookieSecure;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      JwtProperties jwtProperties,
      @Value("${app.auth.cookie-secure:false}") boolean cookieSecure) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.jwtProperties = jwtProperties;
    this.cookieSecure = cookieSecure;
  }

  @Transactional
  public Session register(RegisterRequest request, String userAgent) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByEmail(email)) {
      throw ApiException.conflict("Ya existe una cuenta con ese correo.");
    }

    User user =
        new User(
            email,
            passwordEncoder.encode(request.password()),
            request.name().trim(),
            request.timezone());
    userRepository.save(user);
    return startSession(user, userAgent);
  }

  @Transactional
  public Session login(LoginRequest request, String userAgent) {
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> ApiException.unauthorized("Correo o contraseña incorrectos."));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw ApiException.unauthorized("Correo o contraseña incorrectos.");
    }

    return startSession(user, userAgent);
  }

  @Transactional
  public Session refresh(String rawToken, String userAgent) {
    RefreshToken current = refreshTokenService.requireActive(rawToken);
    User user = current.getUser();
    refreshTokenService.revoke(current);
    return startSession(user, userAgent);
  }

  @Transactional
  public void logout(String rawToken) {
    refreshTokenService.revoke(rawToken);
  }

  public ResponseCookie refreshCookie(String rawToken) {
    return ResponseCookie.from(REFRESH_COOKIE, rawToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(jwtProperties.refreshTtl())
        .build();
  }

  public ResponseCookie clearRefreshCookie() {
    return ResponseCookie.from(REFRESH_COOKIE, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(Duration.ZERO)
        .build();
  }

  private Session startSession(User user, String userAgent) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = refreshTokenService.issue(user, userAgent);
    return new Session(accessToken, refreshToken, user);
  }

  public record Session(String accessToken, String refreshToken, User user) {}
}
