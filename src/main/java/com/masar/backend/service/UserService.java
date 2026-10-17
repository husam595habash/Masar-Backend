package com.masar.backend.service;

import com.masar.backend.dto.user.ChangePasswordRequest;
import com.masar.backend.exception.InvalidPasswordException;
import com.masar.backend.exception.ResourceNotFoundException;
import com.masar.backend.repository.UserRepository;
import com.masar.backend.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Things a user does after getting in (profile, account settings).
 * Register and login live in AuthService.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthVersionService authVersionService;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthVersionService authVersionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authVersionService =  authVersionService;
    }

    public void changePassword(String email,ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password must be different from the current one");
        }
        String newPassword = request.getNewPassword();
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        user.setAuthVersion(user.getAuthVersion() + 1);
        userRepository.save(user);
        authVersionService.evict(email);


    }

    // TODO: getCurrentUser(email)       -> used by GET /api/users/me
    // TODO: updateProfile(email, ...)
    // TODO: changePassword(email, ...)
}
