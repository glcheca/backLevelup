package com.levelup.levelup.auth.dto;

import com.levelup.levelup.user.dto.UserResponse;

public record AuthResponse(String accessToken, UserResponse user) {}
