package com.akash.credential_verification.Service;

import com.akash.credential_verification.Model.Student;
import com.akash.credential_verification.Repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentServiceTest {

    private StudentRepository studentRepository;
    private StudentService studentService;

    @BeforeEach
    void setUp() {
        studentRepository = Mockito.mock(StudentRepository.class);
        studentService = new StudentService(studentRepository);
    }

    @Test
    void create_shouldAssignStudentIdWithStuPrefix() {
        Student student = new Student();
        student.setName("Test Student");
        student.setEmail("student@example.com");

        when(studentRepository.findAll()).thenReturn(Collections.emptyList());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student created = studentService.create(student);

        assertEquals("STU-101", created.getId());
        verify(studentRepository).save(any(Student.class));
    }
}
