package com.fidely.service;

import com.fidely.dto.request.OnboardingRequest;
import com.fidely.dto.request.card.CreateCardRequest;
import com.fidely.dto.request.card.RedeemRequest;
import com.fidely.dto.request.card.ScanRequest;
import com.fidely.dto.response.OnboardingResponse;
import com.fidely.dto.response.card.CardInfoResponse;
import com.fidely.dto.response.card.RedeemResponse;
import com.fidely.dto.response.card.ScanResponse;
import com.fidely.entity.*;
import com.fidely.exception.SubscriptionInactiveException;
import com.fidely.repository.BusinessRepository;
import com.fidely.repository.EmployeeRepository;
import com.fidely.repository.ScanLogRepository;
import com.fidely.repository.WalletCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final BusinessRepository businessRepository;
    private final WalletCardRepository walletCardRepository;
    private final ScanLogRepository scanLogRepository;
    private final GoogleWalletService googleWalletService;
    private final CustomerService customerService;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;
    private final SseService sseService;

    @Transactional
    public WalletCard createCardForCustomer(CreateCardRequest request) {
        Business business = businessRepository.findById(request.businessId())
                .orElseThrow(() -> new NoSuchElementException("Error: La peluquería no existe."));

        Customer customer = customerService.getOrCreateCustomer(
                request.customerName(),
                request.customerPhone(),
                request.customerEmail()
        );

        Optional<WalletCard> existingCard = walletCardRepository.findByCustomerAndBusiness(customer, business);
        if (existingCard.isPresent()) return existingCard.get();

        WalletCard newCard = WalletCard.builder()
                .customer(customer)
                .business(business)
                .build();
        return walletCardRepository.save(newCard);
    }

    @Transactional
    public OnboardingResponse silentOnboarding(OnboardingRequest request) {
        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new RuntimeException("El negocio no existe."));

        Customer customer = customerService.getOrCreateCustomer(
                request.getName(),
                request.getPhoneNumber(),
                request.getEmail()
        );

        WalletCard card = walletCardRepository.findByCustomerAndBusiness(customer, business)
                .orElseGet(() -> {
                    WalletCard newCard = WalletCard.builder()
                            .customer(customer)
                            .business(business)
                            .build();
                    return walletCardRepository.save(newCard);
                });
        String walletUrl = googleWalletService.generateGoogleWalletLink(card);

        emailService.sendWelcomeAndCardEmail(
                customer.getEmail(),
                customer.getName(),
                business.getBrandName() != null ? business.getBrandName() : business.getName(),
                walletUrl
        );
        return new OnboardingResponse(walletUrl, "¡Tarjeta generada con éxito!");
    }

    @Transactional
    public ScanResponse processScan(ScanRequest request) {
        WalletCard card = walletCardRepository.findBySecureUuid(request.getSecureUuid())
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada o UUID inválido."));

        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Employee employee = employeeRepository.findByEmail(email).orElse(null);
        Business authBusiness = (employee != null) ? employee.getBusiness() : businessRepository.findByEmail(email).orElseThrow();

        if (!authBusiness.isSubscriptionActive())
            throw new SubscriptionInactiveException("La suscripción del local está inactiva. El escáner ha sido bloqueado temporalmente.");

        if (!card.getBusiness().getId().equals(authBusiness.getId()))
            throw new RuntimeException("Esta tarjeta no pertenece a tu comercio.");

        if (card.getCurrentStamps() >= card.getMaxStamps())
            return new ScanResponse(false, card.getCurrentStamps(), card.getMaxStamps(), "¡La tarjeta ya está completa!");


        int amountToAdd = request.getAmount() != null && request.getAmount() > 0 ? request.getAmount() : 1;
        if (card.getCurrentStamps() + amountToAdd > card.getMaxStamps())
            amountToAdd = card.getMaxStamps() - card.getCurrentStamps();

        card.setCurrentStamps(card.getCurrentStamps() + amountToAdd);
        walletCardRepository.save(card);

        ScanLog scanLog = ScanLog.builder()
                .walletCard(card)
                .scanType(ScanType.EARN_STAMP)
                .amount(amountToAdd)
                .employee(employee)
                .scannedAt(LocalDateTime.now())
                .build();
        scanLogRepository.save(scanLog);

        boolean isCompleted = card.getCurrentStamps().equals(card.getMaxStamps());

        sseService.emitEvent(card.getSecureUuid(), "scan-update",
                Map.of("currentStamps", card.getCurrentStamps(), "isCompleted", isCompleted));

        if (card.getCurrentStamps() == card.getMaxStamps() - 1)
            googleWalletService.updateCardAndTriggerPush(card, "¡Te falta solo 1 sello para tu premio!");
        else if (isCompleted)
            googleWalletService.updateCardAndTriggerPush(card, "¡Enhorabuena! Tienes un premio listo para canjear.");
        else
            googleWalletService.updateCardAndTriggerPush(card, "Sello añadido. Tienes " + card.getCurrentStamps() + " de " + card.getMaxStamps());

        return new ScanResponse(true, card.getCurrentStamps(), card.getMaxStamps(),
                isCompleted ? "¡" + amountToAdd + " sello(s) añadido(s)! Tarjeta completada, premio desbloqueado." : "¡" + amountToAdd + " sello(s) añadido(s) correctamente!");
    }

    @Transactional
    public RedeemResponse redeemReward(RedeemRequest request) {
        WalletCard card = walletCardRepository.findBySecureUuid(request.getSecureUuid())
                .orElseThrow(() -> new RuntimeException("Tarjeta no encontrada o UUID inválido."));

        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Employee employee = employeeRepository.findByEmail(email).orElse(null);
        Business authBusiness = (employee != null) ? employee.getBusiness() : businessRepository.findByEmail(email).orElseThrow();

        if (!card.getBusiness().getId().equals(authBusiness.getId()))
            throw new RuntimeException("Esta tarjeta no pertenece a tu comercio.");

        if (card.getCurrentStamps() < card.getMaxStamps())
            throw new RuntimeException("El cliente no tiene los sellos necesarios.");

        ScanLog scanLog = ScanLog.builder()
                .walletCard(card)
                .scanType(ScanType.REDEEM_REWARD)
                .employee(employee)
                .scannedAt(LocalDateTime.now())
                .build();
        scanLogRepository.save(scanLog);

        card.setCurrentStamps(0);
        walletCardRepository.save(card);
        return new RedeemResponse(true, "¡Premio canjeado!");
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