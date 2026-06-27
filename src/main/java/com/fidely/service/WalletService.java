package com.fidely.service;

import com.fidely.dto.request.card.CreateCardRequest;
import com.fidely.dto.request.card.RedeemRequest;
import com.fidely.dto.request.card.ScanRequest;
import com.fidely.dto.response.card.CardInfoResponse;
import com.fidely.dto.response.card.RedeemResponse;
import com.fidely.dto.response.card.ScanResponse;
import com.fidely.entity.*;
import com.fidely.repository.BusinessRepository;
import com.fidely.repository.CustomerRepository;
import com.fidely.repository.ScanLogRepository;
import com.fidely.repository.WalletCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- IMPORTANTE

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final WalletCardRepository walletCardRepository;
    private final ScanLogRepository scanLogRepository;

    @Transactional
    public WalletCard createCardForCustomer(CreateCardRequest request) {
        Business business = businessRepository.findById(request.businessId())
                .orElseThrow(() -> new NoSuchElementException("Error: La peluquería no existe."));

        Customer customer = customerRepository.findByPhoneNumber(request.customerPhone())
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .name(request.customerName())
                            .phoneNumber(request.customerPhone())
                            .email(request.customerEmail())
                            .build();
                    return customerRepository.save(newCustomer);
                });

        Optional<WalletCard> existingCard = walletCardRepository.findByCustomerAndBusiness(customer, business);
        if (existingCard.isPresent()) return existingCard.get();

        WalletCard newCard = WalletCard.builder()
                .customer(customer)
                .business(business)
                .build();
        return walletCardRepository.save(newCard);
    }

    @Transactional
    public ScanResponse processScan(ScanRequest request) {
        WalletCard card = walletCardRepository.findBySecureUuid(request.getSecureUuid())
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada o UUID inválido."));

        if (!card.getBusiness().getId().equals(request.getBusinessId()))
            throw new RuntimeException("Esta tarjeta no pertenece a tu comercio.");

        if (card.getCurrentStamps() >= card.getMaxStamps()) {
            return new ScanResponse(
                    false,
                    card.getCurrentStamps(),
                    card.getMaxStamps(),
                    "¡La tarjeta ya está completa! El cliente debe canjear su premio."
            );
        }

        card.setCurrentStamps(card.getCurrentStamps() + 1);
        walletCardRepository.save(card);

        ScanLog scanLog = ScanLog.builder()
                .walletCard(card)
                .scanType(ScanType.EARN_STAMP)
                .scannedAt(LocalDateTime.now())
                .build();
        scanLogRepository.save(scanLog);

        boolean isCompleted = card.getCurrentStamps().equals(card.getMaxStamps());
        String message = isCompleted
                ? "¡Sello añadido! Tarjeta completada, premio desbloqueado."
                : "Sello añadido correctamente.";
        return new ScanResponse(true, card.getCurrentStamps(), card.getMaxStamps(), message);
    }

    @Transactional
    public RedeemResponse redeemReward(RedeemRequest request) {
        WalletCard card = walletCardRepository.findBySecureUuid(request.getSecureUuid())
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada o UUID inválido."));

        if (!card.getBusiness().getId().equals(request.getBusinessId()))
            throw new RuntimeException("Esta tarjeta no pertenece a tu comercio.");

        if (card.getCurrentStamps() < card.getMaxStamps())
            throw new RuntimeException("El cliente no tiene los sellos necesarios ("
                    + card.getCurrentStamps() + "/" + card.getMaxStamps() + ").");

        ScanLog scanLog = ScanLog.builder()
                .walletCard(card)
                .scanType(ScanType.REDEEM_REWARD)
                .scannedAt(LocalDateTime.now())
                .build();
        scanLogRepository.save(scanLog);

        card.setCurrentStamps(0);
        walletCardRepository.save(card);
        return new RedeemResponse(true, "¡Premio canjeado con éxito! La tarjeta se ha reiniciado a 0 sellos.");
    }

    @Transactional(readOnly = true)
    public CardInfoResponse getCardInfo(String secureUuid, Long businessId) {
        WalletCard card = walletCardRepository.findBySecureUuid(secureUuid)
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada o UUID inválido."));

        if (!card.getBusiness().getId().equals(businessId))
            throw new RuntimeException("Esta tarjeta no pertenece a tu comercio.");

        boolean isCompleted = card.getCurrentStamps() >= card.getMaxStamps();
        return new CardInfoResponse(
                card.getCustomer().getName() != null ? card.getCustomer().getName() : "Cliente VIP",
                card.getCurrentStamps(),
                card.getMaxStamps(),
                isCompleted
        );
    }
}