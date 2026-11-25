package com.levelup.levelup.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.user.dto.UpdateProfileRequest;
import com.levelup.levelup.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public UserResponse getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
		return userService.getProfile(principal.id());
	}

	@PatchMapping
	public UserResponse updateProfile(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody UpdateProfileRequest request) {
		return userService.updateProfile(principal.id(), request);
	}
}
