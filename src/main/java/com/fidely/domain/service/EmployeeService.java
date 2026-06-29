package com.fidely.domain.service;

import com.fidely.domain.dto.request.CreateEmployeeRequest;
import com.fidely.domain.entity.Business;
import com.fidely.domain.entity.Employee;
import com.fidely.dao.repository.BusinessRepository;
import com.fidely.dao.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<Employee> getEmployees(String ownerEmail) {
        Business business = businessRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        return employeeRepository.findByBusinessId(business.getId());
    }

    @Transactional
    public void deleteEmployee(Long employeeId, String ownerEmail) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        if (!employee.getBusiness().getEmail().equals(ownerEmail))
            throw new RuntimeException("No tienes permiso para borrar este empleado");

        employeeRepository.delete(employee);
    }
}