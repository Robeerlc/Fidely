package com.fidely.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fidely.entity.Business;
import com.fidely.entity.Customer;
import com.fidely.entity.WalletCard;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;

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
                ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(is);
                String serviceAccountEmail = credentials.getClientEmail();
                RSAPrivateKey privateKey = (RSAPrivateKey) credentials.getPrivateKey();

                String classId = String.format("%s.%s", issuerId, classIdSuffix);
                String objectId = String.format("%s.%s", issuerId, walletCard.getSecureUuid());

                Customer customer = walletCard.getCustomer();
                Business business = walletCard.getBusiness();

                Map<String, Object> passObject = new HashMap<>();
                passObject.put("id", objectId);
                passObject.put("classId", classId);
                passObject.put("state", "ACTIVE");

                String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();
                String themeColor = business.getThemeColor() != null ? business.getThemeColor() : "#000000";

                String customerName = customer.getName() != null ? customer.getName() : "Cliente VIP";

                passObject.put("cardTitle", Map.of("defaultValue", Map.of("language", "es-ES", "value", brandName)));
                passObject.put("subheader", Map.of("defaultValue", Map.of("language", "es-ES", "value", "Tarjeta de Cliente")));
                passObject.put("header", Map.of("defaultValue", Map.of("language", "es-ES", "value", customerName)));
                passObject.put("hexBackgroundColor", themeColor);

                if (business.getLogoUrl() != null && !business.getLogoUrl().isBlank()) {
                    Map<String, Object> logo = new HashMap<>();
                    logo.put("sourceUri", Map.of("uri", business.getLogoUrl()));
                    passObject.put("logo", logo);
                }

                if (business.getHeroImageUrl() != null && !business.getHeroImageUrl().isBlank()) {
                    Map<String, Object> heroImage = new HashMap<>();
                    heroImage.put("sourceUri", Map.of("uri", business.getHeroImageUrl()));
                    passObject.put("heroImage", heroImage);
                }

                Map<String, Object> stampsModule = new HashMap<>();
                stampsModule.put("header", "SELLOS ACUMULADOS");
                stampsModule.put("body", walletCard.getCurrentStamps() + " / " + walletCard.getMaxStamps() + " ✂️");

                Map<String, Object> rewardModule = new HashMap<>();
                rewardModule.put("header", "RECOMPENSA");
                String rewardText = business.getRewardDescription() != null ? business.getRewardDescription() : "¡Completa la tarjeta para tu premio!";
                rewardModule.put("body", rewardText);

                passObject.put("textModulesData", List.of(stampsModule, rewardModule));

                if (business.getBookingUrl() != null || business.getInstagramUrl() != null) {
                    Map<String, Object> linksModule = new HashMap<>();
                    ArrayList<Map<String, Object>> uris = new java.util.ArrayList<>();

                    if (business.getBookingUrl() != null)
                        uris.add(Map.of("uri", business.getBookingUrl(), "description", "Reservar Cita Online"));

                    if (business.getInstagramUrl() != null)
                        uris.add(Map.of("uri", business.getInstagramUrl(), "description", "Ver Instagram"));

                    linksModule.put("uris", uris);
                    passObject.put("linksModuleData", linksModule);
                }

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
            throw new RuntimeException("Error generando el enlace de Google Wallet personalizado", e);
        }
    }
}