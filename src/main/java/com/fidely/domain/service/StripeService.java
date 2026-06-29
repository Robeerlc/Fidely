package com.fidely.domain.service;

import com.fidely.domain.entity.Business;
import com.fidely.domain.exception.InvalidOperationException;
import com.fidely.domain.exception.ResourceNotFoundException;
import com.fidely.dao.repository.BusinessRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SubscriptionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final StripeClient stripeClient;
    private final BusinessRepository businessRepository;

    @Value("${stripe.price-id}")
    private String priceId;

    public String createClient(String email, String name) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .build();

        String stripeCustomerId = stripeClient.v1().customers().create(params).getId();

        Business business = businessRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));
        business.setStripeCustomerId(stripeCustomerId);
        businessRepository.save(business);

        return stripeCustomerId;
    }

    public String subscribe(String customerId, String paymentMethodId) throws StripeException {
        stripeClient.v1().paymentMethods().attach(
                paymentMethodId,
                PaymentMethodAttachParams.builder().setCustomer(customerId).build()
        );

        stripeClient.v1().customers().update(
                customerId,
                CustomerUpdateParams.builder()
                        .setInvoiceSettings(
                                CustomerUpdateParams.InvoiceSettings.builder()
                                        .setDefaultPaymentMethod(paymentMethodId)
                                        .build()
                        ).build()
        );

        Subscription sub = stripeClient.v1().subscriptions().create(
                SubscriptionCreateParams.builder()
                        .setCustomer(customerId)
                        .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                        .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                        .addExpand("latest_invoice")
                        .build()
        );

        // Persistir el subscription ID en el negocio
        businessRepository.findByStripeCustomerId(customerId).ifPresent(business -> {
            business.setStripeSubscriptionId(sub.getId());
            businessRepository.save(business);
        });

        Invoice invoice = sub.getLatestInvoiceObject();
        if (invoice == null || invoice.getRawJsonObject() == null)
            throw new InvalidOperationException("La factura de Stripe no está disponible.");

        String paymentIntentId = invoice.getRawJsonObject().get("payment_intent").getAsString();
        PaymentIntent paymentIntent = stripeClient.v1().paymentIntents().retrieve(paymentIntentId);
        return paymentIntent.getClientSecret();
    }

    public void cancelForBusiness(String ownerEmail) throws StripeException {
        Business business = businessRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (business.getStripeSubscriptionId() == null)
            throw new InvalidOperationException("Este negocio no tiene ninguna suscripción activa.");

        stripeClient.v1().subscriptions().cancel(business.getStripeSubscriptionId());

        business.setStripeSubscriptionId(null);
        business.setSubscriptionActive(false);
        businessRepository.save(business);
    }

    public void processWebhook(String payload, String signature, String webhookSecret) throws StripeException {
        Event event = Webhook.constructEvent(payload, signature, webhookSecret);

        switch (event.getType()) {
            case "invoice.payment_succeeded" -> {
                Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
                if (invoice != null && invoice.getCustomer() != null)
                    businessRepository.findByStripeCustomerId(invoice.getCustomer()).ifPresent(b -> {
                        b.setSubscriptionActive(true);
                        businessRepository.save(b);
                        log.info("Suscripción ACTIVADA para: {}", b.getEmail());
                    });
            }
            case "invoice.payment_failed", "customer.subscription.deleted" -> {
                Object obj = event.getDataObjectDeserializer().getObject().orElse(null);
                String customerId = null;
                if (obj instanceof Invoice inv) customerId = inv.getCustomer();
                else if (obj instanceof Subscription sub) customerId = sub.getCustomer();

                if (customerId != null)
                    businessRepository.findByStripeCustomerId(customerId).ifPresent(b -> {
                        b.setSubscriptionActive(false);
                        b.setStripeSubscriptionId(null);
                        businessRepository.save(b);
                        log.warn("Suscripción DESACTIVADA para: {}", b.getEmail());
                    });
            }
        }
    }
}
