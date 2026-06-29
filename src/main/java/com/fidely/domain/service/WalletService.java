package com.fidely.domain.service;

import com.fidely.dao.repository.BusinessRepository;
import com.fidely.dao.repository.EmployeeRepository;
import com.fidely.dao.repository.ScanLogRepository;
import com.fidely.dao.repository.WalletCardRepository;
import com.fidely.domain.dto.ScanUpdateEvent;
import com.fidely.domain.dto.request.OnboardingRequest;
import com.fidely.domain.dto.request.card.RedeemRequest;
import com.fidely.domain.dto.request.card.ScanRequest;
import com.fidely.domain.dto.response.OnboardingResponse;
import com.fidely.domain.dto.response.card.CardInfoResponse;
import com.fidely.domain.dto.response.card.RedeemResponse;
import com.fidely.domain.dto.response.card.ScanResponse;
import com.fidely.domain.entity.*;
import com.fidely.domain.exception.AccessForbiddenException;
import com.fidely.domain.exception.InvalidOperationException;
import com.fidely.domain.exception.ResourceNotFoundException;
import com.fidely.domain.exception.SubscriptionInactiveException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final BusinessRepository businessRepository;
    private final WalletCardRepository walletCardRepository;
    private final ScanLogRepository scanLogRepository;
    private final GoogleWalletService googleWalletService;
    private final CustomerService customerService;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;
    private final SseService sseService;
    private final KafkaTemplate<Object, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OnboardingResponse silentOnboarding(OnboardingRequest request) {
        Business business = businessRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Código de invitación inválido o negocio no encontrado."));

        Customer customer = customerService.getOrCreateCustomer(
                request.name(), request.phoneNumber(), request.email()
        );

        WalletCard card = walletCardRepository.findByCustomerAndBusiness(customer, business)
                .orElseGet(() -> walletCardRepository.save(
                        WalletCard.builder()
                                .customer(customer)
                                .business(business)
                                .maxStamps(business.getDefaultMaxStamps())
                                .build()
                ));
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
        WalletCard card = walletCardRepository.findBySecureUuid(request.secureUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada o UUID inválido."));

        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Employee employee = employeeRepository.findByEmail(email).orElse(null);
        Business authBusiness = (employee != null)
                ? employee.getBusiness()
                : businessRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (!authBusiness.isSubscriptionActive())
            throw new SubscriptionInactiveException("La suscripción del local está inactiva. El escáner ha sido bloqueado temporalmente.");

        if (!card.getBusiness().getId().equals(authBusiness.getId()))
            throw new AccessForbiddenException("Esta tarjeta no pertenece a tu comercio.");

        if (!card.getIsActive())
            throw new InvalidOperationException("Esta tarjeta ha sido desactivada por el negocio.");

        if (card.getCurrentStamps() >= card.getMaxStamps())
            return new ScanResponse(false, card.getCurrentStamps(), card.getMaxStamps(), "¡La tarjeta ya está completa!");

        int baseAmount = request.amount() != null && request.amount() > 0 ? request.amount() : 1;
        int amountToAdd = baseAmount * authBusiness.getCurrentMultiplier();
        if (card.getCurrentStamps() + amountToAdd > card.getMaxStamps())
            amountToAdd = card.getMaxStamps() - card.getCurrentStamps();

        card.setCurrentStamps(card.getCurrentStamps() + amountToAdd);
        card.setLastScannedAt(LocalDateTime.now());
        walletCardRepository.save(card);

        scanLogRepository.save(ScanLog.builder()
                .walletCard(card)
                .scanType(ScanType.EARN_STAMP)
                .amount(amountToAdd)
                .employee(employee)
                .scannedAt(LocalDateTime.now())
                .build());

        boolean isCompleted = card.getCurrentStamps().equals(card.getMaxStamps());

        sseService.emitEvent(card.getSecureUuid(), "scan-update",
                Map.of("currentStamps", card.getCurrentStamps(), "isCompleted", isCompleted));

        String pushMessage;
        if (isCompleted)
            pushMessage = "¡Enhorabuena! Tienes un premio listo para canjear.";
        else if (card.getCurrentStamps() == card.getMaxStamps() - 1)
            pushMessage = "¡Te falta solo 1 sello para tu premio!";
        else
            pushMessage = "Sello añadido. Tienes " + card.getCurrentStamps() + " de " + card.getMaxStamps();

        try {
            ScanUpdateEvent event = new ScanUpdateEvent(card.getId(), pushMessage);
            kafkaTemplate.send("scan-updates", objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Error encolando actualización de escaneo: {}", e.getMessage());
        }

        String msg = isCompleted
                ? "¡" + amountToAdd + " sello(s) añadido(s)! Tarjeta completada, premio desbloqueado."
                : "¡" + amountToAdd + " sello(s) añadido(s) correctamente!";
        return new ScanResponse(true, card.getCurrentStamps(), card.getMaxStamps(), msg);
    }

    @Transactional
    public RedeemResponse redeemReward(RedeemRequest request) {
        WalletCard card = walletCardRepository.findBySecureUuid(request.secureUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada o UUID inválido."));

        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Employee employee = employeeRepository.findByEmail(email).orElse(null);
        Business authBusiness = (employee != null)
                ? employee.getBusiness()
                : businessRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (!card.getBusiness().getId().equals(authBusiness.getId()))
            throw new AccessForbiddenException("Esta tarjeta no pertenece a tu comercio.");

        if (!card.getIsActive())
            throw new InvalidOperationException("Esta tarjeta ha sido desactivada por el negocio.");

        if (card.getCurrentStamps() < card.getMaxStamps())
            throw new InvalidOperationException("El cliente no tiene los sellos necesarios para canjear el premio.");

        scanLogRepository.save(ScanLog.builder()
                .walletCard(card)
                .scanType(ScanType.REDEEM_REWARD)
                .employee(employee)
                .scannedAt(LocalDateTime.now())
                .build());

        card.setCurrentStamps(0);
        walletCardRepository.save(card);

        googleWalletService.updateCardAndTriggerPush(card, "¡Premio canjeado! Ya puedes volver a acumular sellos.");

        return new RedeemResponse(true, "¡Premio canjeado!");
    }

    @Transactional(readOnly = true)
    public CardInfoResponse getCardInfo(String secureUuid, Long businessId) {
        WalletCard card = walletCardRepository.findBySecureUuid(secureUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada o UUID inválido."));

        if (!card.getBusiness().getId().equals(businessId))
            throw new AccessForbiddenException("Esta tarjeta no pertenece a tu comercio.");

        boolean isCompleted = card.getCurrentStamps() >= card.getMaxStamps();
        return new CardInfoResponse(
                card.getCustomer().getName() != null ? card.getCustomer().getName() : "Cliente VIP",
                card.getCurrentStamps(),
                card.getMaxStamps(),
                isCompleted
        );
    }
}
