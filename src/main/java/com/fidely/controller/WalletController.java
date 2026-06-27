package com.fidely.controller;

import com.fidely.dto.response.card.CardResponse;
import com.fidely.dto.request.card.CreateCardRequest;
import com.fidely.entity.Business;
import com.fidely.entity.WalletCard;
import com.fidely.repository.BusinessRepository;
import com.fidely.service.GoogleWalletService;
import com.fidely.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final BusinessRepository businessRepository;
    private final GoogleWalletService googleWalletService;

    @PostMapping("/create")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        WalletCard card = walletService.createCardForCustomer(request);
        CardResponse response = new CardResponse(
                card.getId(),
                card.getSecureUuid(),
                card.getCurrentStamps(),
                card.getMaxStamps(),
                "Tarjeta digital creada correctamente"
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/business/{businessId}/sync-google")
    public ResponseEntity<String> syncBusinessWithGoogleWallet(@PathVariable Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        googleWalletService.createGenericClassForBusiness(business);
        return ResponseEntity.ok("Plantilla de Google Wallet creada/actualizada con éxito para: " + business.getBrandName());
    }
}