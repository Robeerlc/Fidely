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
import com.google.api.services.walletobjects.model.GenericClass;
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

    private final ResourceLoader resourceLoader;
    @Value("${fidely.wallet.google.issuer-id}")
    private String issuerId;
    @Value("${fidely.wallet.google.credentials-path}")
    private String credentialsPath;

    public void createGenericClassForBusiness(Business business) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Resource resource = resourceLoader.getResource(credentialsPath);
            GoogleCredentials credentials;
            try (InputStream is = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(is).createScoped(Collections.singleton("https://www.googleapis.com/auth/wallet_object.issuer"));
            }

            Walletobjects client = new Walletobjects.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials)).setApplicationName("Fidely").build();

            String newClassId = String.format("%s.business_%d_premium", issuerId, business.getId());
            String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();
            GenericClass genericClass = new GenericClass().setId(newClassId);

            try {
                System.out.println("Creando plantilla Premium en Google Wallet para: " + brandName);
                client.genericclass().insert(genericClass).execute();
                System.out.println("Plantilla Premium creada con éxito");
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
                String classId = String.format("%s.business_%d_premium", issuerId, business.getId());
                String objectId = String.format("%s.%s", issuerId, walletCard.getSecureUuid());

                Map<String, Object> passObject = new HashMap<>();
                passObject.put("id", objectId);
                passObject.put("classId", classId);
                passObject.put("state", "ACTIVE");

                passObject.put("hexBackgroundColor", "#FFFFFF");

                if (business.getLogoUrl() != null && !business.getLogoUrl().isBlank()) {
                    Map<String, Object> logo = new HashMap<>();
                    logo.put("sourceUri", Map.of("uri", business.getLogoUrl()));
                    passObject.put("logo", logo);
                }

                String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();
                passObject.put("cardTitle", Map.of("defaultValue", Map.of("language", "es-ES", "value", brandName)));

                String mainTitle = business.getRewardDescription() != null ? business.getRewardDescription() : "¡Consigue tu premio!";
                passObject.put("header", Map.of("defaultValue", Map.of("language", "es-ES", "value", mainTitle)));

                Map<String, Object> stampsModule = new HashMap<>();
                stampsModule.put("header", "Cupones disponibles 🏷️");
                stampsModule.put("body", walletCard.getCurrentStamps() + " / " + walletCard.getMaxStamps());

                Map<String, Object> clientModule = new HashMap<>();
                clientModule.put("header", "Cliente:");
                clientModule.put("body", customer.getName() != null ? customer.getName() : "Cliente VIP");

                Map<String, Object> familyModule = new HashMap<>();
                familyModule.put("header", "Beneficio");
                familyModule.put("body", "Ser de nuestra familia tiene recompensa");

                passObject.put("textModulesData", List.of(stampsModule, clientModule, familyModule));

                if (business.getHeroImageUrl() != null && !business.getHeroImageUrl().isBlank()) {
                    Map<String, Object> heroImage = new HashMap<>();
                    heroImage.put("sourceUri", Map.of("uri", business.getHeroImageUrl()));
                    passObject.put("heroImage", heroImage);
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
            throw new RuntimeException("Error generando el enlace de Google Wallet", e);
        }
    }

    public void updateGenericClassForBusiness(Business business) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            Resource resource = resourceLoader.getResource(credentialsPath);
            GoogleCredentials credentials;
            try (InputStream is = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(is).createScoped(Collections.singleton("https://www.googleapis.com/auth/wallet_object.issuer"));
            }
            Walletobjects client = new Walletobjects.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(credentials)).setApplicationName("Fidely").build();

            String classId = String.format("%s.business_%d_premium", issuerId, business.getId());
            String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();
            GenericClass genericClass = new GenericClass().setId(classId);

            try {
                System.out.println("Actualizando plantilla Premium en Google Wallet para: " + brandName);
                client.genericclass().update(classId, genericClass).execute();
                System.out.println("Plantilla actualizada con éxito");
            } catch (Exception e) {
                System.err.println("Error al actualizar la clase en Google: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error crítico de infraestructura en Google Wallet al actualizar", e);
        }
    }
}