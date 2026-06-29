package com.fidely.ui.controller;

import com.fidely.domain.dto.request.OnboardingRequest;
import com.fidely.domain.dto.response.OnboardingResponse;
import com.fidely.domain.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final WalletService walletService;

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingResponse> onboarding(@Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.ok(walletService.silentOnboarding(request));
    }
}
