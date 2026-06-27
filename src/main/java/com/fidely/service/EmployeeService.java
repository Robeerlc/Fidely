package com.fidely.service;

import com.fidely.dto.request.CreateEmployeeRequest;
import com.fidely.entity.Business;
import com.fidely.entity.Employee;
import com.fidely.repository.BusinessRepository;
import com.fidely.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;

    public void createEmployee(String ownerEmail, CreateEmployeeRequest request) {
        Business business = businessRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));

        if (employeeRepository.findByEmail(request.getEmail()).isPresent())
            throw new RuntimeException("Ya existe un empleado con este email.");

        Employee employee = Employee.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .business(business)
                .build();
        employeeRepository.save(employee);
    }
}