package com.kbase.backend.user;

import com.kbase.backend.auth.dto.UserResponse;
import com.kbase.backend.exception.InvalidCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .map(UserResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
