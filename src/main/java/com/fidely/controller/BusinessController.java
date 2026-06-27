package com.fidely.controller;

import com.fidely.dto.request.BusinessProfileRequest;
import com.fidely.dto.request.LoginRequest;
import com.fidely.dto.request.RegisterBusinessRequest;
import com.fidely.dto.response.BusinessProfileResponse;
import com.fidely.dto.response.RegisterResponse;
import com.fidely.dto.response.statistics.DashboardResponse;
import com.fidely.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerBusiness(@Valid @RequestBody RegisterBusinessRequest request) {
        RegisterResponse response = businessService.registerBusiness(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<RegisterResponse> loginBusiness(@Valid @RequestBody LoginRequest request) {
        RegisterResponse response = businessService.loginBusiness(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{businessId}/profile")
    public ResponseEntity<BusinessProfileResponse> updateProfile(@PathVariable Long businessId, @Valid @RequestBody BusinessProfileRequest request) {
        BusinessProfileResponse response = businessService.updateProfile(businessId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{businessId}/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardMetrics(@PathVariable Long businessId) {
        DashboardResponse response = businessService.getDashboardMetrics(businessId);
        return ResponseEntity.ok(response);
    }
}