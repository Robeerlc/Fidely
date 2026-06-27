package com.fidely.controller;

import com.fidely.dto.request.CreateEmployeeRequest;
import com.fidely.dto.response.EmployeeResponse;
import com.fidely.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> listEmployees() {
        String ownerEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return ResponseEntity.ok(employeeService.getEmployees(ownerEmail).stream()
                .map(e -> EmployeeResponse.builder().id(e.getId()).name(e.getName()).email(e.getEmail()).build())
                .toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        String ownerEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        employeeService.deleteEmployee(id, ownerEmail);
        return ResponseEntity.ok("Empleado eliminado correctamente");
    }
}