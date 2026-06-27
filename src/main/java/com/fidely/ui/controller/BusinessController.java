package com.fidely.ui.controller;

import com.fidely.domain.service.BusinessService;
import com.fidely.ui.dto.business.RegisterBusinessRequest;
import com.fidely.ui.dto.business.RegisterBusinessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping("/register")
    public ResponseEntity<RegisterBusinessResponse> registerCustomer(@Valid @RequestBody RegisterBusinessRequest request) {
        RegisterBusinessResponse response = businessService.registerBusiness(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
