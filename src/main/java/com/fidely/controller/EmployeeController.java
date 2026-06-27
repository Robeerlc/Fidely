package com.fidely.controller;

import com.fidely.dto.request.CreateEmployeeRequest;
import com.fidely.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/business/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    @PostMapping("/create")
    public ResponseEntity<String> createEmployee(@RequestBody CreateEmployeeRequest request) {
        String ownerEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        employeeService.createEmployee(ownerEmail, request);
        return ResponseEntity.ok("Empleado " + request.getName() + " registrado correctamente para tu negocio.");
    }
}