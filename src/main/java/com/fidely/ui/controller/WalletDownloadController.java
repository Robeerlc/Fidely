package com.fidely.ui.controller;

import com.fidely.domain.entity.WalletCard;
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

        System.out.println("User-Agent: " + userAgent);
        WalletCard walletCard = walletCardRepository.findBySecureUuid(secureUuid)
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada."));
        String ua = userAgent.toLowerCase();

        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("mac")) return downloadForApple(walletCard);
        else if (ua.contains("android")) return downloadForGoogle(walletCard);
        else throw new RuntimeException("Por favor, abre este enlace desde tu teléfono móvil.");
    }

    private ResponseEntity<?> downloadForGoogle(WalletCard walletCard) {
        String googleLink = googleWalletService.generateGoogleWalletLink(walletCard);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(googleLink)).build();
    }

    private ResponseEntity<?> downloadForApple(WalletCard walletCard) {
        // TODO: jPasskit lógica
        return ResponseEntity.ok("Aquí irá el archivo .pkpass de Apple para el UUID: " + walletCard.getSecureUuid());
    }
}