package com.levelup.levelup.user;

import com.levelup.levelup.auth.AuthenticatedUser;
import com.levelup.levelup.user.dto.ChangePasswordRequest;
import com.levelup.levelup.user.dto.UpdateProfileRequest;
import com.levelup.levelup.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

  @PostMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(
      @AuthenticationPrincipal AuthenticatedUser principal,
      @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(principal.id(), request);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAccount(@AuthenticationPrincipal AuthenticatedUser principal) {
    userService.deleteAccount(principal.id());
  }
}
