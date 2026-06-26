package com.fidely.controller;

import com.fidely.dto.CardResponse;
import com.fidely.dto.CreateCardRequest;
import com.fidely.entity.WalletCard;
import com.fidely.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

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
}