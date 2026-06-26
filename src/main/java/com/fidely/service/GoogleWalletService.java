package com.fidely.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fidely.entity.WalletCard;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleWalletService {

    @Value("${fidely.wallet.google.issuer-id}")
    private String issuerId;

    @Value("${fidely.wallet.google.class-id}")
    private String classIdSuffix;

    @Value("${fidely.wallet.google.credentials-path}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public String generateGoogleWalletLink(WalletCard walletCard) {
        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            try (InputStream is = resource.getInputStream()) {
                ServiceAccountCredentials credentials = (ServiceAccountCredentials) ServiceAccountCredentials.fromStream(is);
                String serviceAccountEmail = credentials.getClientEmail();
                RSAPrivateKey privateKey = (RSAPrivateKey) credentials.getPrivateKey();

                String classId = String.format("%s.%s", issuerId, classIdSuffix);
                String objectId = String.format("%s.%s", issuerId, walletCard.getSecureUuid());

                Map<String, Object> passObject = new HashMap<>();
                passObject.put("id", objectId);
                passObject.put("classId", classId);
                passObject.put("state", "ACTIVE");

                passObject.put("cardTitle", Map.of("defaultValue", Map.of("language", "es-ES", "value", "Fidely")));
                passObject.put("subheader", Map.of("defaultValue", Map.of("language", "es-ES", "value", "Tarjeta de Cliente")));
                passObject.put("header", Map.of("defaultValue", Map.of("language", "es-ES", "value", walletCard.getCustomer().getName())));
                passObject.put("hexBackgroundColor", "#000000"); // Color de fondo obligatorio

                Map<String, Object> barcode = new HashMap<>();
                barcode.put("type", "QR_CODE");
                barcode.put("value", walletCard.getSecureUuid());
                passObject.put("barcode", barcode);

                Map<String, Object> payloadClaims = new HashMap<>();
                payloadClaims.put("iss", serviceAccountEmail);
                payloadClaims.put("aud", "google");
                payloadClaims.put("origins", List.of());
                payloadClaims.put("typ", "savetowallet");

                Map<String, Object> payloadObjects = new HashMap<>();
                payloadObjects.put("genericObjects", List.of(passObject));
                payloadClaims.put("payload", payloadObjects);

                Algorithm algorithm = Algorithm.RSA256(null, privateKey);
                long nowMillis = System.currentTimeMillis();
                String jwtToken = JWT.create()
                        .withIssuedAt(new Date(nowMillis))
                        .withExpiresAt(new Date(nowMillis + 3600000))
                        .withPayload(payloadClaims)
                        .sign(algorithm);
                return "https://pay.google.com/gp/v/save/" + jwtToken;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generando el enlace de Google Wallet", e);
        }
    }
}