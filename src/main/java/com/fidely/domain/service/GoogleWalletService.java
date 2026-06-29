package com.fidely.domain.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fidely.domain.entity.Business;
import com.fidely.domain.entity.Customer;
import com.fidely.domain.entity.WalletCard;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GoogleWalletService {

    @Value("${fidely.wallet.google.issuer-id}")
    private String issuerId;

    @Value("${GOOGLE_CREDENTIALS_JSON}")
    private String googleCredentialsJson;

    private GoogleCredentials getCredentials() throws IOException {
        InputStream is = new ByteArrayInputStream(googleCredentialsJson.getBytes(StandardCharsets.UTF_8));
        return GoogleCredentials.fromStream(is).createScoped(Collections.singleton("https://www.googleapis.com/auth/wallet_object.issuer"));
    }

    public void createGenericClassForBusiness(Business business) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            Walletobjects client = new Walletobjects.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(getCredentials())).setApplicationName("Fidely").build();

            String newClassId = String.format("%s.business_%d_premium", issuerId, business.getId());
            String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();
            GenericClass genericClass = new GenericClass().setId(newClassId);

            try {
                System.out.println("Creando plantilla Premium en Google Wallet para: " + brandName);
                client.genericclass().insert(genericClass).execute();
            } catch (Exception e) {
                System.err.println("Aviso al crear la clase: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error crítico de infraestructura en Google Wallet", e);
        }
    }

    public String generateGoogleWalletLink(WalletCard walletCard) {
        try {
            ServiceAccountCredentials saCredentials = (ServiceAccountCredentials) getCredentials();
            String serviceAccountEmail = saCredentials.getClientEmail();
            RSAPrivateKey privateKey = (RSAPrivateKey) saCredentials.getPrivateKey();

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
            passObject.put("header", Map.of("defaultValue", Map.of("language", "es-ES", "value", business.getRewardDescription() != null ? business.getRewardDescription() : "¡Consigue tu premio!")));

            passObject.put("textModulesData", List.of(
                    Map.of("header", "Cupones disponibles 🏷️", "body", walletCard.getCurrentStamps() + " / " + walletCard.getMaxStamps()),
                    Map.of("header", "Cliente:", "body", customer.getName() != null ? customer.getName() : "Cliente VIP"),
                    Map.of("header", "Beneficio", "body", "Ser de nuestra familia tiene recompensa")
            ));

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
            payloadClaims.put("payload", Map.of("genericObjects", List.of(passObject)));

            Algorithm algorithm = Algorithm.RSA256(null, privateKey);
            return "https://pay.google.com/gp/v/save/" + JWT.create()
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                    .withPayload(payloadClaims)
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("Error generando el enlace de Google Wallet", e);
        }
    }

    public void updateGenericClassForBusiness(Business business) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            Walletobjects client = new Walletobjects.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(getCredentials())).setApplicationName("Fidely").build();

            String classId = String.format("%s.business_%d_premium", issuerId, business.getId());
            GenericClass genericClass = new GenericClass().setId(classId);

            try {
                client.genericclass().update(classId, genericClass).execute();
            } catch (Exception e) {
                System.err.println("Error al actualizar la clase en Google: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error crítico al actualizar Google Wallet", e);
        }
    }

    public void updateCardAndTriggerPush(WalletCard card, String pushMessage) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            Walletobjects client = new Walletobjects.Builder(httpTransport, jsonFactory, new HttpCredentialsAdapter(getCredentials()))
                    .setApplicationName("Fidely").build();

            String objectId = String.format("%s.%s", issuerId, card.getSecureUuid());
            GenericObject existingObject = client.genericobject().get(objectId).execute();

            existingObject.setTextModulesData(List.of(
                    new TextModuleData()
                            .setHeader("Cupones disponibles 🏷️")
                            .setBody(card.getCurrentStamps() + " / " + card.getMaxStamps()),

                    new TextModuleData()
                            .setHeader("Cliente:")
                            .setBody(card.getCustomer().getName() != null ? card.getCustomer().getName() : "Cliente VIP")
            ));
            client.genericobject().update(objectId, existingObject).execute();

            Message notificationMessage = new Message()
                    .setHeader("Aviso de " + (card.getBusiness().getBrandName() != null ? card.getBusiness().getBrandName() : card.getBusiness().getName()))
                    .setBody(pushMessage)
                    .set("messageType", "textAndNotify");

            AddMessageRequest messageRequest = new AddMessageRequest().setMessage(notificationMessage);
            client.genericobject().addmessage(objectId, messageRequest).execute();
            System.out.println("Notificación Push nativa disparada para: " + objectId);
        } catch (Exception e) {
            System.err.println("Aviso: No se pudo enviar el Push nativo a Google Wallet: " + e.getMessage());
        }
    }
}