package com.fidely.service;

import com.fidely.entity.WalletCard;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.walletobjects.Walletobjects;
import com.google.api.services.walletobjects.model.Barcode;
import com.google.api.services.walletobjects.model.GenericObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;

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
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            // 1. Autenticación con Google usando tu JSON
            Resource resource = resourceLoader.getResource(credentialsPath);
            GoogleCredential credential;
            try (InputStream is = resource.getInputStream()) {
                credential = GoogleCredential.fromStream(is)
                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/wallet_object.issuer"));
            }

            // 2. Cliente oficial de la API de Google Wallet
            Walletobjects client = new Walletobjects.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName("Fidely")
                    .build();

            // 3. Nombres de Clase y Objeto
            String classId = String.format("%s.%s", issuerId, classIdSuffix);
            String objectId = String.format("%s.%s", issuerId, walletCard.getSecureUuid());

            // 4. Construimos el Objeto Genérico usando el SDK oficial (no mapas crudos)
            GenericObject newObject = new GenericObject()
                    .setId(objectId)
                    .setClassId(classId)
                    .setState("ACTIVE")
                    .setBarcode(new Barcode()
                            .setType("QR_CODE")
                            .setValue(walletCard.getSecureUuid())
                            .setAlternateText(walletCard.getSecureUuid()));

            // 5. ¡AQUÍ ESTÁ LA MAGIA! Intentamos insertarlo directamente en Google.
            // Si hay un error de formato, permisos o clase inexistente, petará en esta línea
            // y te dará el mensaje exacto de por qué Google lo odia.
            try {
                System.out.println("Intentando insertar objeto en Google Wallet: " + objectId);
                client.genericobject().insert(newObject).execute();
                System.out.println("¡INSERCIÓN EXITOSA! Google ha aceptado la tarjeta.");
            } catch (Exception apiError) {
                System.err.println("\n\n❌ ERROR DE GOOGLE REVELADO ❌");
                System.err.println("El motivo exacto del rechazo es:");
                System.err.println(apiError.getMessage());
                System.err.println("❌❌❌\n\n");
                throw apiError; // Lo lanzamos para que lo veas también en el Postman
            }

            // Si llegamos aquí, la tarjeta ya vive en los servidores de Google.
            // Ahora sí podemos generar el JWT básico y enviarlo al usuario sin miedo.
            // (Para esta prueba de debug, simplemente devolvemos un OK si no explotó arriba)
            return "https://pay.google.com/gp/v/save/AQUI_IRIA_EL_JWT_PERO_ESTAMOS_DEBUGGEANDO";

        } catch (Exception e) {
            throw new RuntimeException("Fallo crítico general", e);
        }
    }
}