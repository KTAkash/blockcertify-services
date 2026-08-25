package com.akash.credential_verification.Service;

import com.akash.credential_verification.Constants.CertificateStatus;
import com.akash.credential_verification.Dto.StudentCertificateStatusResponse;
import com.akash.credential_verification.Model.Certificate;
import com.akash.credential_verification.Model.Student;
import com.akash.credential_verification.Repository.CertificateRepository;
import com.akash.credential_verification.Repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String STUDENT_ID_PREFIX = "STU-";
    private static final long STARTING_SUFFIX = 101L;

    private final StudentRepository studentRepository;
    private final CertificateRepository certificateRepository;

    public Student create(Student student) {
        student.setId(generateStudentId());
        return studentRepository.insert(student);
    }

    private String generateStudentId() {
        long maxSuffix = studentRepository.findAll().stream()
                .map(Student::getId)
                .filter(id -> id != null && id.startsWith(STUDENT_ID_PREFIX))
                .map(id -> id.substring(STUDENT_ID_PREFIX.length()))
                .map(suffix -> {
                    try {
                        return Long.parseLong(suffix);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .max(Long::compareTo)
                .orElse(STARTING_SUFFIX - 1);

        return STUDENT_ID_PREFIX + (maxSuffix + 1);
    }

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    public Optional<Student> getById(String id) {
        return studentRepository.findById(id);
    }

    public Optional<Student> update(String id, Student student) {
        return studentRepository.findById(id)
                .map(existingStudent -> {
                    existingStudent.setName(student.getName());
                    existingStudent.setEmail(student.getEmail());
                    existingStudent.setIndexNo(student.getIndexNo());
                    return studentRepository.save(existingStudent);
                });
    }

    public boolean delete(String id) {
        if (!studentRepository.existsById(id)) {
            return false;
        }

        studentRepository.deleteById(id);
        return true;
    }

    public List<StudentCertificateStatusResponse> getAllWithCertificateStatus() {
        List<Student> students = studentRepository.findAll();
        List<Certificate> allCertificates = certificateRepository.findAll();

        Map<String, List<Certificate>> certsByStudentId = allCertificates.stream()
                .collect(Collectors.groupingBy(Certificate::getStudentId));

        return students.stream()
                .map(student -> {
                    List<Certificate> studentCerts = certsByStudentId.getOrDefault(student.getId(), List.of());

                    long issuedCount = studentCerts.stream()
                            .filter(c -> c.getStatus() == CertificateStatus.ISSUED).count();
                    long validCount = studentCerts.stream()
                            .filter(c -> c.getStatus() == CertificateStatus.VALID).count();
                    long revokedCount = studentCerts.stream()
                            .filter(c -> c.getStatus() == CertificateStatus.REVOKED).count();
                    long expiredCount = studentCerts.stream()
                            .filter(c -> c.getStatus() == CertificateStatus.EXPIRED).count();

                    return StudentCertificateStatusResponse.builder()
                            .id(student.getId())
                            .name(student.getName())
                            .email(student.getEmail())
                            .indexNo(student.getIndexNo())
                            .totalCertificates(studentCerts.size())
                            .issuedCount(issuedCount)
                            .validCount(validCount)
                            .revokedCount(revokedCount)
                            .expiredCount(expiredCount)
                            .hasIssuedCertificate(issuedCount > 0 || validCount > 0)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
