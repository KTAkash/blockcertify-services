package com.akash.credential_verification.Service;

import com.akash.credential_verification.Model.Student;
import com.akash.credential_verification.Repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public Student create(Student student) {
        student.setId(null);
        return studentRepository.save(student);
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
                    existingStudent.setAttachment(student.getAttachment());
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
}
