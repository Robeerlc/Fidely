package com.fidely.controller;

import com.fidely.entity.WalletCard;
import com.fidely.repository.WalletCardRepository;
import com.fidely.service.GoogleWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletDownloadController {

    private final WalletCardRepository walletCardRepository;
    private final GoogleWalletService googleWalletService;
    // TODO: private final AppleWalletService appleWalletService;

    @GetMapping("/{secureUuid}/download")
    public ResponseEntity<?> downloadWalletPass(
            @PathVariable String secureUuid,
            @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent) {
        System.out.println("User-Agent: " + userAgent);
        WalletCard walletCard = walletCardRepository.findBySecureUuid(secureUuid)
                .orElse(null);

        if (walletCard == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tarjeta no encontrada.");
        String ua = userAgent.toLowerCase();

        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("mac")) return downloadForApple(walletCard);
        else if (ua.contains("android")) return downloadForGoogle(walletCard);
        else return ResponseEntity.badRequest().body("Por favor, abre este enlace desde tu teléfono móvil.");
    }

    private ResponseEntity<?> downloadForApple(WalletCard walletCard) {
        // TODO: jPasskit lógica
        return ResponseEntity.ok("Aquí irá el archivo .pkpass de Apple para el UUID: " + walletCard.getSecureUuid());
    }

    private ResponseEntity<?> downloadForGoogle(WalletCard walletCard) {
        try {
            String googleLink = googleWalletService.generateGoogleWalletLink(walletCard);
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(googleLink);
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(googleLink)).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al generar el pase de Google Wallet.");
        }
    }
}