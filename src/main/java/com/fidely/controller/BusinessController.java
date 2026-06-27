package com.fidely.controller;

import com.fidely.dto.BusinessProfileRequest;
import com.fidely.dto.BusinessProfileResponse;
import com.fidely.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @PutMapping("/{businessId}/profile")
    public ResponseEntity<BusinessProfileResponse> updateProfile(@PathVariable Long businessId, @Valid @RequestBody BusinessProfileRequest request) {
        BusinessProfileResponse response = businessService.updateProfile(businessId, request);
        return ResponseEntity.ok(response);
    }
}