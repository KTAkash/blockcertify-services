package com.akash.credential_verification.Service;

import com.akash.credential_verification.Dto.AuthResponse;
import com.akash.credential_verification.Dto.LoginRequest;
import com.akash.credential_verification.Dto.StudentSignupRequest;
import com.akash.credential_verification.Model.StudentAuth;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Model.User;
import com.akash.credential_verification.Repository.UniversityRepository;
import com.akash.credential_verification.Repository.UserRepository;
import com.akash.credential_verification.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final StudentAuthService studentAuthService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest req) {
        // 1. Try Super Admin
        var userOpt = userRepository.findByUsername(req.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
                String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getRole().name());
                return AuthResponse.builder()
                        .token(token)
                        .role(user.getRole().name())
                        .userId(user.getId())
                        .name(user.getUsername())
                        .build();
            }
        }

        // 2. Try University Admin
        var uniOpt = universityRepository.findByUsername(req.getUsername());
        if (uniOpt.isPresent()) {
            University uni = uniOpt.get();
            if (!uni.isActive()) {
                throw new IllegalArgumentException("University account is deactivated. Please contact super admin.");
            }
            if (passwordEncoder.matches(req.getPassword(), uni.getPasswordHash())) {
                String token = jwtUtil.generateToken(uni.getId(), uni.getMspId(), "UNIVERSITY");
                return AuthResponse.builder()
                        .token(token)
                        .role("UNIVERSITY")
                        .userId(uni.getId())
                        .name(uni.getName())
                        .build();
            }
        }

        // 3. Try Student (login by email)
        var studentAuthOpt = studentAuthService.findByEmail(req.getUsername());
        if (studentAuthOpt.isPresent()) {
            StudentAuth studentAuth = studentAuthOpt.get();
            if (studentAuthService.verifyPassword(studentAuth, req.getPassword())) {
                String token = jwtUtil.generateToken(studentAuth.getId(), "STUDENT", "STUDENT");
                String studentName = studentAuth.getFirstName() + " " + studentAuth.getLastName();
                return AuthResponse.builder()
                        .token(token)
                        .role("STUDENT")
                        .userId(studentAuth.getId())
                        .name(studentName)
                        .build();
            }
        }

        throw new IllegalArgumentException("Invalid username or password");
    }

    public AuthResponse signupStudent(StudentSignupRequest req) {
        return studentAuthService.signup(req);
    }
}
