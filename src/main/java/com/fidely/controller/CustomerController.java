package com.fidely.controller;

import com.fidely.dto.request.RegisterCustomerRequest;
import com.fidely.dto.response.RegisterResponse;
import com.fidely.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        RegisterResponse response = customerService.registerCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}