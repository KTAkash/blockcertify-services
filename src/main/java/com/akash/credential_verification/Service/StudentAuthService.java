package com.akash.credential_verification.Service;

import com.akash.credential_verification.Constants.UserRole;
import com.akash.credential_verification.Dto.AuthResponse;
import com.akash.credential_verification.Dto.StudentDetailsResponse;
import com.akash.credential_verification.Dto.StudentProfileResponse;
import com.akash.credential_verification.Dto.StudentSignupRequest;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Model.Student;
import com.akash.credential_verification.Model.StudentAuth;
import com.akash.credential_verification.Repository.CertificateRepository;
import com.akash.credential_verification.Repository.StudentAuthRepository;
import com.akash.credential_verification.Repository.StudentRepository;
import com.akash.credential_verification.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAuthService {

    private final StudentAuthRepository studentAuthRepository;
    private final StudentRepository studentRepository;
    private final CertificateRepository certificateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse signup(StudentSignupRequest req) {
        if (studentAuthRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (studentAuthRepository.findByIndexNo(req.getIndexNo()).isPresent()) {
            throw new IllegalArgumentException("Index number already exists");
        }

        String fullName = req.getFirstName() + " " + req.getLastName();
        StudentAuth studentAuth = StudentAuth.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .indexNo(req.getIndexNo())
                .mobileNo(req.getMobileNo())
                .gender(req.getGender())
                .role(UserRole.STUDENT)
                .build();

        StudentAuth saved = studentAuthRepository.save(studentAuth);
        String token = jwtUtil.generateToken(saved.getId(), "STUDENT", "STUDENT");

        return AuthResponse.builder()
                .token(token)
                .role("STUDENT")
                .userId(saved.getId())
                .name(fullName)
                .build();
    }

    public Optional<StudentAuth> findByEmail(String email) {
        return studentAuthRepository.findByEmail(email);
    }

    public Optional<StudentAuth> findById(String id) {
        return studentAuthRepository.findById(id);
    }

    public Optional<StudentAuth> findByIndexNo(String indexNo) {
        return studentAuthRepository.findByIndexNo(indexNo);
    }

    public boolean verifyPassword(StudentAuth studentAuth, String rawPassword) {
        return passwordEncoder.matches(rawPassword, studentAuth.getPasswordHash());
    }

    public AuthResponse login(String email, String password) {
        StudentAuth studentAuth = studentAuthRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!verifyPassword(studentAuth, password)) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(studentAuth.getId(), "STUDENT", "STUDENT");
        String fullName = studentAuth.getFirstName() + " " + studentAuth.getLastName();
        return AuthResponse.builder()
                .token(token)
                .role("STUDENT")
                .userId(studentAuth.getId())
                .name(fullName)
                .build();
    }

    public StudentProfileResponse getProfileById(String id) {
        StudentAuth studentAuth = studentAuthRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return StudentProfileResponse.builder()
                .id(studentAuth.getId())
                .firstName(studentAuth.getFirstName())
                .lastName(studentAuth.getLastName())
                .email(studentAuth.getEmail())
                .indexNo(studentAuth.getIndexNo())
                .mobileNo(studentAuth.getMobileNo())
                .gender(studentAuth.getGender())
                .role(studentAuth.getRole() != null ? studentAuth.getRole().name() : null)
                .createdAt(studentAuth.getCreatedAt())
                .build();
    }

    public Optional<StudentDetailsResponse> getDetailsByIndexNo(String indexNo) {
        return studentRepository.findByIndexNo(indexNo)
                .map(student -> {
                    List<Certificate> certificates = certificateRepository.findByStudentId(student.getId());
                    List<StudentDetailsResponse.CertificateInfo> certInfos = certificates.stream()
                            .map(cert -> StudentDetailsResponse.CertificateInfo.builder()
                                    .certificateId(cert.getId())
                                    .certificateTitle(cert.getCertificateTitle())
                                    .cid(cert.getCid())
                                    .hash(cert.getHash())
                                    .issuedBy(cert.getIssuedBy())
                                    .status(cert.getStatus() != null ? cert.getStatus().name() : null)
                                    .issuedAt(cert.getIssuedAt())
                                    .build())
                            .collect(Collectors.toList());

                    return StudentDetailsResponse.builder()
                            .indexNo(student.getIndexNo())
                            .certificates(certInfos)
                            .build();
                });
    }
}
