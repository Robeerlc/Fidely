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

        WalletCard card = walletCardRepository.findBySecureUuid(secureUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada."));

        if (!card.getIsActive())
            throw new InvalidOperationException("Esta tarjeta ha sido desactivada.");

        String ua = userAgent.toLowerCase();

        if (ua.contains("android"))
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(googleWalletService.generateGoogleWalletLink(card)))
                    .build();

        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("coredata"))
            // Apple Wallet (.pkpass) pendiente — requiere certificados Apple Developer
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Apple Wallet estará disponible próximamente.");

        throw new InvalidOperationException("Por favor, abre este enlace desde tu teléfono móvil.");
    }
}
