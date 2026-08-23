package com.akash.credential_verification.Controller;

import com.akash.credential_verification.Dto.AuthResponse;
import com.akash.credential_verification.Dto.LoginRequest;
import com.akash.credential_verification.Dto.StudentDetailsResponse;
import com.akash.credential_verification.Dto.StudentLoginRequest;
import com.akash.credential_verification.Dto.StudentProfileResponse;
import com.akash.credential_verification.Dto.StudentSignupRequest;
import com.akash.credential_verification.Dto.SuperAdminRegisterRequest;
import com.akash.credential_verification.Model.StudentPrincipal;
import com.akash.credential_verification.Model.User;
import com.akash.credential_verification.Service.AuthService;
import com.akash.credential_verification.Service.StudentAuthService;
import com.akash.credential_verification.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final StudentAuthService studentAuthService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/student/signup")
    public ResponseEntity<AuthResponse> signupStudent(@RequestBody @Valid StudentSignupRequest req) {
        return ResponseEntity.ok(studentAuthService.signup(req));
    }

    @PostMapping("/student/login")
    public ResponseEntity<AuthResponse> loginStudent(@RequestBody @Valid StudentLoginRequest req) {
        return ResponseEntity.ok(studentAuthService.login(req.getEmail(), req.getPassword()));
    }

    @GetMapping("/student/profile")
    public ResponseEntity<StudentProfileResponse> getStudentProfile(@AuthenticationPrincipal StudentPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(studentAuthService.getProfileById(principal.id()));
    }

    @GetMapping("/student/details-by-index")
    public ResponseEntity<StudentDetailsResponse> getStudentDetailsByIndexNo(@RequestParam String indexNo) {
        if (indexNo == null || indexNo.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(studentAuthService.getDetailsByIndexNo(indexNo));
    }

    @PostMapping("/superadmin/register")
    public ResponseEntity<?> registerSuperAdmin(@RequestBody @Valid SuperAdminRegisterRequest req) {
        User user = userService.signUpSuperAdmin(req.getUsername(), req.getPassword());
        Map<String, Object> response = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "createdAt", user.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }
}
