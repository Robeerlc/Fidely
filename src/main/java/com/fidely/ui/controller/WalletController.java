package com.fidely.ui.controller;

import com.fidely.dao.repository.BusinessRepository;
import com.fidely.domain.dto.request.OnboardingRequest;
import com.fidely.domain.dto.response.OnboardingResponse;
import com.fidely.domain.entity.Business;
import com.fidely.domain.service.GoogleWalletService;
import com.fidely.domain.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final BusinessRepository businessRepository;
    private final GoogleWalletService googleWalletService;

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingResponse> silentOnboarding(@Valid @RequestBody OnboardingRequest request) {
        OnboardingResponse response = walletService.silentOnboarding(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/business/{businessId}/sync-google")
    public ResponseEntity<String> syncBusinessWithGoogleWallet(@PathVariable Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        googleWalletService.createGenericClassForBusiness(business);
        return ResponseEntity.ok("Plantilla de Google Wallet creada/actualizada con éxito para: " + business.getBrandName());
    }
}