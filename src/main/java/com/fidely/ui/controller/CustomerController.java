package com.fidely.ui.controller;

import com.fidely.domain.entity.Customer;
import com.fidely.domain.service.CustumerService;
import com.fidely.ui.dto.Scan.ScanRequest;
import com.fidely.ui.dto.costumer.RegisterCustumerRequest;
import com.fidely.ui.dto.costumer.RegisterCustumerResponse;
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
    private final CustumerService customerService;

    @PostMapping("/register")
    public ResponseEntity<RegisterCustumerResponse> registerCustomer(@Valid @RequestBody RegisterCustumerRequest request) {
        RegisterCustumerResponse response = customerService.registerBusiness(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
