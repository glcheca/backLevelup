package com.levelup.levelup.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.levelup.levelup.config.JwtProperties;
import com.levelup.levelup.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final JwtProperties properties;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		byte[] secret = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < 32) {
			throw new IllegalStateException("app.jwt.secret debe tener al menos 32 caracteres");
		}
		this.key = Keys.hmacShaKeyFor(secret);
	}

	public String generateAccessToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("name", user.getName())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(properties.accessTtl())))
				.signWith(key)
				.compact();
	}

	public Optional<AuthenticatedUser> parse(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			return Optional.of(new AuthenticatedUser(UUID.fromString(claims.getSubject())));
		} catch (JwtException | IllegalArgumentException exception) {
			return Optional.empty();
		}
	}
}
