package com.fidely.ui.controller;

import com.fidely.domain.entity.WalletCard;
import com.fidely.domain.exception.InvalidOperationException;
import com.fidely.domain.exception.ResourceNotFoundException;
import com.fidely.dao.repository.WalletCardRepository;
import com.fidely.domain.service.GoogleWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletDownloadController {

    private final WalletCardRepository walletCardRepository;
    private final GoogleWalletService googleWalletService;

    @GetMapping("/{secureUuid}/download")
    public ResponseEntity<?> downloadWalletPass(
            @PathVariable String secureUuid,
            @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent) {

        WalletCard walletCard = walletCardRepository.findBySecureUuid(secureUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada."));

        String ua = userAgent.toLowerCase();

        if (ua.contains("android"))
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(googleWalletService.generateGoogleWalletLink(walletCard)))
                    .build();

        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("mac"))
            // Apple Wallet (.pkpass) pendiente de implementar con certificados Apple Developer
            return ResponseEntity.ok("Soporte de Apple Wallet próximamente. UUID: " + walletCard.getSecureUuid());

        throw new InvalidOperationException("Por favor, abre este enlace desde tu teléfono móvil.");
    }
}
