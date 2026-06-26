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

    // Ahora mapean a las propiedades de tu application.properties
    @Value("${fidely.wallet.google.issuer-id}")
    private String issuerId;

    @Value("${fidely.wallet.google.class-id}")
    private String classIdSuffix;

    @Value("${fidely.wallet.google.credentials-path}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public String generateGoogleWalletLink(WalletCard walletCard) {
        try {
            // 1. Extraer credenciales reales del archivo JSON
            Resource resource = resourceLoader.getResource(credentialsPath);
            try (InputStream is = resource.getInputStream()) {
                ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(is);

                String serviceAccountEmail = credentials.getClientEmail();
                RSAPrivateKey privateKey = (RSAPrivateKey) credentials.getPrivateKey();

                // 2. Definir los identificadores únicos
                String classId = String.format("%s.%s", issuerId, classIdSuffix);
                String objectId = String.format("%s.%s", issuerId, walletCard.getSecureUuid());

                // 3. Construir el esqueleto del PassObject
                Map<String, Object> passObject = new HashMap<>();
                passObject.put("id", objectId);
                passObject.put("classId", classId);
                passObject.put("state", "ACTIVE");

                // Metemos el UUID secreto dentro del QR
                Map<String, Object> barcode = new HashMap<>();
                barcode.put("type", "QR_CODE");
                barcode.put("value", walletCard.getSecureUuid());
                passObject.put("barcode", barcode);

                // Mostramos los sellos actuales
                Map<String, Object> stampsModule = new HashMap<>();
                stampsModule.put("header", "SELLOS ACUMULADOS");
                stampsModule.put("body", walletCard.getCurrentStamps() + " / " + walletCard.getMaxStamps());
                passObject.put("textModulesData", List.of(stampsModule));

                // 4. Montar el Payload del JWT
                Map<String, Object> payloadClaims = new HashMap<>();
                payloadClaims.put("iss", serviceAccountEmail);
                payloadClaims.put("aud", "google");
                payloadClaims.put("origins", List.of());
                payloadClaims.put("typ", "savetowallet");

                Map<String, Object> payloadObjects = new HashMap<>();
                payloadObjects.put("genericObjects", List.of(passObject));
                payloadClaims.put("payload", payloadObjects);

                // 5. Firmar el JWT con la clave real RSA de tu archivo JSON
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