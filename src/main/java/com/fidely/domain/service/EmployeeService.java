package com.fidely.domain.service;

import com.fidely.domain.dto.request.CreateEmployeeRequest;
import com.fidely.domain.entity.Business;
import com.fidely.domain.entity.Employee;
import com.fidely.domain.exception.AccessForbiddenException;
import com.fidely.domain.exception.DuplicateResourceException;
import com.fidely.domain.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (employeeRepository.findByEmail(request.email()).isPresent())
            throw new DuplicateResourceException("Ya existe un empleado con este email.");

        employeeRepository.save(Employee.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .business(business)
                .build());
    }

    public List<Employee> getEmployees(String ownerEmail) {
        Business business = businessRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));
        return employeeRepository.findByBusinessId(business.getId());
    }

    @Transactional
    public void deleteEmployee(Long employeeId, String ownerEmail) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado."));

        if (!employee.getBusiness().getEmail().equals(ownerEmail))
            throw new AccessForbiddenException("No tienes permiso para eliminar este empleado.");

        employeeRepository.delete(employee);
    }
}
