package com.akash.credential_verification.Controller;

import com.akash.credential_verification.Dto.LoginRequest;
import com.akash.credential_verification.Dto.SuperAdminRegisterRequest;
import com.akash.credential_verification.Model.User;
import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Repository.UniversityRepository;
import com.akash.credential_verification.Service.UserService;
import com.akash.credential_verification.Util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UniversityRepository universityRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // First try to login as university
        var uniOpt = universityRepository.findByUsername(username);
        if (uniOpt.isPresent()) {
            University uni = uniOpt.get();
            if (passwordEncoder.matches(password, uni.getPasswordHash())) {
                String token = jwtUtil.generateToken(uni.getId(), uni.getMspId(), "UNIVERSITY");
                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "universityId", uni.getId(),
                        "role", "UNIVERSITY"
                ));
            }
        }

        // Then try to login as super admin / user
        var userOpt = userService.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                String token = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getRole().name());
                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "userId", user.getId(),
                        "role", user.getRole().name()
                ));
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @PostMapping("/superadmin/register")
    public ResponseEntity<?> registerSuperAdmin(@RequestBody @Valid SuperAdminRegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        try {
            User user = userService.signUpSuperAdmin(username, password);
            return ResponseEntity.ok(Map.of(
                    "message", "Super admin registered successfully",
                    "userId", user.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}