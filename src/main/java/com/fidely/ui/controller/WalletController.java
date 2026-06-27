package com.fidely.ui.controller;


import com.fidely.domain.entity.Business;
import com.fidely.domain.entity.WalletCard;
import com.fidely.dao.repository.BusinessRepository;
import com.fidely.domain.service.GoogleWalletService;
import com.fidely.domain.service.WalletService;
import com.fidely.ui.dto.card.CardResponse;
import com.fidely.ui.dto.card.CreateCardRequest;
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