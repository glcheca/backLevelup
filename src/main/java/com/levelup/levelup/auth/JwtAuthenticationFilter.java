package com.levelup.levelup.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER_PREFIX)
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			jwtService.parse(header.substring(BEARER_PREFIX.length())).ifPresent(principal -> {
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						principal, null, List.of());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			});
		}

		filterChain.doFilter(request, response);
	}
}
