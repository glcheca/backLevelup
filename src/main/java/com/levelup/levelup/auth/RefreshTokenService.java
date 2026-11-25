package com.levelup.levelup.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.config.JwtProperties;
import com.levelup.levelup.user.User;

@Service
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository repository;
	private final JwtProperties properties;
	private final SecureRandom random = new SecureRandom();

	public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	@Transactional
	public String issue(User user, String userAgent) {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		RefreshToken token = new RefreshToken(
				user,
				hash(rawToken),
				Instant.now().plus(properties.refreshTtl()),
				truncate(userAgent));
		repository.save(token);
		return rawToken;
	}

	@Transactional
	public RefreshToken requireActive(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw ApiException.unauthorized("No hay una sesión activa.");
		}
		RefreshToken token = repository.findByTokenHash(hash(rawToken))
				.orElseThrow(() -> ApiException.unauthorized("Sesión inválida."));
		if (!token.isActive()) {
			throw ApiException.unauthorized("La sesión ha expirado. Vuelve a iniciar sesión.");
		}
		return token;
	}

	@Transactional
	public void revoke(RefreshToken token) {
		token.revoke();
		repository.save(token);
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		repository.findByTokenHash(hash(rawToken)).ifPresent(this::revoke);
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 no disponible", exception);
		}
	}

	private String truncate(String userAgent) {
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
	}
}
