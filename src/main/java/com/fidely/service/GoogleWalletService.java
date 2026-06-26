package com.fidely.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fidely.entity.Business;
import com.fidely.entity.Customer;
import com.fidely.entity.WalletCard;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.walletobjects.Walletobjects;
import com.google.api.services.walletobjects.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
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

    @Value("${fidely.wallet.google.credentials-path}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public void createLoyaltyClassForBusiness(Business business) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Resource resource = resourceLoader.getResource(credentialsPath);
            GoogleCredentials credentials;
            try (InputStream is = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(is)
                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/wallet_object.issuer"));
            }

            Walletobjects client = new Walletobjects.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Fidely")
                    .build();

            String newClassId = String.format("%s.business_%d_loyalty", issuerId, business.getId());
            String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();
            String logoUrl = (business.getLogoUrl() != null && !business.getLogoUrl().isBlank())
                    ? business.getLogoUrl()
                    : "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=200&q=80";

            LoyaltyClass loyaltyClass = new LoyaltyClass()
                    .setId(newClassId)
                    .setIssuerName(brandName)
                    .setProgramName(brandName)
                    .setProgramLogo(new Image().setSourceUri(new ImageUri().setUri(logoUrl)))
                    .setReviewStatus("UNDER_REVIEW");

            try {
                System.out.println("Creando plantilla en Google Wallet para: " + brandName);
                client.loyaltyclass().insert(loyaltyClass).execute();
                System.out.println("Plantilla creada con éxito en Google");
            } catch (Exception e) {
                System.err.println("Aviso al crear la clase (puede que ya exista): " + e.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error crítico de infraestructura en Google Wallet", e);
        }
    }

    public String generateGoogleWalletLink(WalletCard walletCard) {
        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            try (InputStream is = resource.getInputStream()) {
                ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(is);
                String serviceAccountEmail = credentials.getClientEmail();
                RSAPrivateKey privateKey = (RSAPrivateKey) credentials.getPrivateKey();

                Customer customer = walletCard.getCustomer();
                Business business = walletCard.getBusiness();

                String classId = String.format("%s.business_%d_loyalty", issuerId, business.getId());
                String objectId = String.format("%s.%s", issuerId, walletCard.getSecureUuid());

                Map<String, Object> passObject = new HashMap<>();
                passObject.put("id", objectId);
                passObject.put("classId", classId);
                passObject.put("state", "ACTIVE");

                passObject.put("accountId", "Socio #" + customer.getId());
                passObject.put("accountName", customer.getName() != null ? customer.getName() : "Cliente VIP");
                passObject.put("hexBackgroundColor", business.getThemeColor() != null ? business.getThemeColor() : "#000000");

                if (business.getHeroImageUrl() != null && !business.getHeroImageUrl().isBlank()) {
                    Map<String, Object> heroImage = new HashMap<>();
                    heroImage.put("sourceUri", Map.of("uri", business.getHeroImageUrl()));
                    passObject.put("heroImage", heroImage);
                }

                Map<String, Object> loyaltyPoints = new HashMap<>();
                loyaltyPoints.put("label", "SELLOS");
                loyaltyPoints.put("balance", Map.of("string", walletCard.getCurrentStamps() + " / " + walletCard.getMaxStamps() + " ✂️"));
                passObject.put("loyaltyPoints", loyaltyPoints);

                Map<String, Object> rewardModule = new HashMap<>();
                rewardModule.put("header", "RECOMPENSA");
                String rewardText = business.getRewardDescription() != null ? business.getRewardDescription() : "¡Completa la tarjeta para tu premio!";
                rewardModule.put("body", rewardText);
                passObject.put("textModulesData", List.of(rewardModule));

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
                payloadObjects.put("loyaltyObjects", List.of(passObject));
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