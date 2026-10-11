package com.masar.backend.service;

import com.masar.backend.dto.user.RegisterRequest;
import com.masar.backend.dto.user.UserDto;
import com.masar.backend.exception.EmailAlreadyExistsException;
import com.masar.backend.model.User;
import com.masar.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto register(RegisterRequest request) {
        String email = request.getEmail().trim();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = request.toEntity(passwordEncoder.encode(request.getPassword()));

        return UserDto.from(userRepository.save(user));
    }
}
