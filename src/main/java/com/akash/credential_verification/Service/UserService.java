package com.akash.credential_verification.Service;

import com.akash.credential_verification.Constants.UserRole;
import com.akash.credential_verification.Model.User;
import com.akash.credential_verification.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User signUpSuperAdmin(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.SUPER_ADMIN)
                .build();

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
