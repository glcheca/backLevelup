package com.levelup.levelup.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite simple en memoria para los endpoints de autenticación. Suficiente para una instancia; si
 * el backend escala horizontalmente, se sustituye por un almacén compartido (Redis).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Set<String> LIMITED_PATHS =
      Set.of("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/me/password");
  private static final int CLEANUP_EVERY = 500;

  private final RateLimitProperties properties;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final AtomicInteger requests = new AtomicInteger();

  public RateLimitFilter(RateLimitProperties properties) {
    this.properties = properties;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !properties.isEnabled()
        || !"POST".equalsIgnoreCase(request.getMethod())
        || !LIMITED_PATHS.contains(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    String key = clientIp(request) + "|" + path;
    int limit = limitFor(path);
    long now = System.currentTimeMillis();
    long windowMillis = Duration.ofSeconds(properties.getWindowSeconds()).toMillis();

    Window window = windows.computeIfAbsent(key, ignored -> new Window());
    if (!window.tryAcquire(now, windowMillis, limit)) {
      response.setStatus(429);
      response.setContentType("application/problem+json");
      response.setCharacterEncoding("UTF-8");
      response.setHeader("Retry-After", Integer.toString(properties.getWindowSeconds()));
      response
          .getWriter()
          .write(
              """
              {"title":"Demasiadas solicitudes","status":429,\
              "detail":"Has hecho demasiados intentos. Espera un momento y vuelve a probar."}\
              """);
      return;
    }

    if (requests.incrementAndGet() % CLEANUP_EVERY == 0) {
      windows.entrySet().removeIf(entry -> entry.getValue().isStale(now, windowMillis));
    }

    filterChain.doFilter(request, response);
  }

  private int limitFor(String path) {
    return path.endsWith("/password")
        ? properties.getMaxPasswordAttempts()
        : properties.getMaxAttempts();
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private static final class Window {
    private final Deque<Long> hits = new ArrayDeque<>();

    synchronized boolean tryAcquire(long now, long windowMillis, int limit) {
      while (!hits.isEmpty() && now - hits.peekFirst() >= windowMillis) {
        hits.pollFirst();
      }
      if (hits.size() >= limit) {
        return false;
      }
      hits.addLast(now);
      return true;
    }

    synchronized boolean isStale(long now, long windowMillis) {
      return hits.isEmpty() || now - hits.peekLast() >= windowMillis;
    }
  }
}
