package com.fidely.domain.service;

import com.fidely.domain.entity.Business;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeService {

    private final StripeClient stripeClient;
    private final BusinessRepository businessRepository;

    @Value("${stripe.price-id}")
    private String priceId;

    public String createClient(String email, String nombre) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(nombre)
                .build();

        String stripeCustomerId = stripeClient.v1().customers().create(params).getId();

        Business business = businessRepository.findByEmail(email).orElseThrow();
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
                        )
                        .build()
        );

        Subscription sub = stripeClient.v1().subscriptions().create(
                SubscriptionCreateParams.builder()
                        .setCustomer(customerId)
                        .addItem(
                                SubscriptionCreateParams.Item.builder()
                                        .setPrice(priceId)
                                        .build()
                        )
                        .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                        .addExpand("latest_invoice")
                        .build()
        );

        Invoice invoice = sub.getLatestInvoiceObject();
        String paymentIntentId = invoice.getRawJsonObject()
                .get("payment_intent")
                .getAsString();

        PaymentIntent pi = stripeClient.v1().paymentIntents().retrieve(paymentIntentId);
        return pi.getClientSecret();
    }

    public void cancel(String subscriptionId) throws StripeException {
        stripeClient.v1().subscriptions().cancel(subscriptionId);
    }


    public void processWebhook(String payload, String signature, String webhookSecret) throws StripeException {
        Event event = Webhook.constructEvent(payload, signature, webhookSecret);

        switch (event.getType()) {
            case "invoice.payment_succeeded" -> {
                Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
                if (invoice != null && invoice.getCustomer() != null) {
                    businessRepository.findByStripeCustomerId(invoice.getCustomer())
                            .ifPresent(business -> {
                                business.setSubscriptionActive(true);
                                businessRepository.save(business);
                                System.out.println("Suscripción ACTIVADA para: " + business.getEmail());
                            });
                }
            }
            case "invoice.payment_failed", "customer.subscription.deleted" -> {
                Object dataObj = event.getDataObjectDeserializer().getObject().orElse(null);
                String customerId = null;

                if (dataObj instanceof Invoice inv) customerId = inv.getCustomer();
                else if (dataObj instanceof Subscription sub) customerId = sub.getCustomer();

                if (customerId != null) {
                    businessRepository.findByStripeCustomerId(customerId)
                            .ifPresent(business -> {
                                business.setSubscriptionActive(false);
                                businessRepository.save(business);
                                System.out.println("Suscripción DESACTIVADA para: " + business.getEmail());
                            });
                }
            }
        }
    }
}