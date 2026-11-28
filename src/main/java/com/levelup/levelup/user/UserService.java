package com.levelup.levelup.user;

import com.levelup.levelup.common.ApiException;
import com.levelup.levelup.user.dto.ChangePasswordRequest;
import com.levelup.levelup.user.dto.UpdateProfileRequest;
import com.levelup.levelup.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
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
    if (request.notifyEvents() != null) {
      user.setNotifyEvents(request.notifyEvents());
    }
    if (request.notifyTasks() != null) {
      user.setNotifyTasks(request.notifyTasks());
    }
    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public void changePassword(UUID userId, ChangePasswordRequest request) {
    User user = requireUser(userId);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw ApiException.badRequest("La contraseña actual no es correcta.");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
  }

  /** Borra la cuenta y, en cascada, todo su contenido (cursos, tareas, apuntes, recordatorios…). */
  @Transactional
  public void deleteAccount(UUID userId) {
    userRepository.delete(requireUser(userId));
  }

  public User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
  }
}
