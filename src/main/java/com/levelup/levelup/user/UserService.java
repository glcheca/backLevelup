package com.levelup.levelup.user;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.user.dto.UpdateProfileRequest;
import com.levelup.levelup.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public UserResponse getProfile(UUID userId) {
    return UserResponse.from(requireUser(userId));
  }

  @Transactional
  public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
    User user = requireUser(userId);
    if (request.name() != null) {
      user.setName(request.name().trim());
    }
    if (request.timezone() != null) {
      user.setTimezone(request.timezone());
    }
    if (request.theme() != null) {
      user.setTheme(request.theme());
    }
    return UserResponse.from(userRepository.save(user));
  }

  User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
  }
}
