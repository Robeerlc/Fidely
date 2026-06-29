package com.fidely.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidely.dao.repository.BusinessRepository;
import com.fidely.dao.repository.EmployeeRepository;
import com.fidely.dao.repository.ScanLogRepository;
import com.fidely.dao.repository.WalletCardRepository;
import com.fidely.domain.dto.CampaignEvent;
import com.fidely.domain.dto.request.*;
import com.fidely.domain.dto.response.*;
import com.fidely.domain.dto.response.statistics.ActivityLogResponse;
import com.fidely.domain.dto.response.statistics.DashboardResponse;
import com.fidely.domain.entity.*;
import com.fidely.domain.exception.AccessForbiddenException;
import com.fidely.domain.exception.DuplicateResourceException;
import com.fidely.domain.exception.InvalidOperationException;
import com.fidely.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final GoogleWalletService googleWalletService;
    private final WalletCardRepository walletCardRepository;
    private final ScanLogRepository scanLogRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Value("${fidely.app.base-url}")
    private String baseUrl;

    public RegisterResponse login(LoginRequest request) {
        Optional<Business> business = businessRepository.findByEmail(request.email());
        if (business.isPresent() && passwordEncoder.matches(request.password(), business.get().getPassword()))
            return new RegisterResponse(jwtService.generateToken(business.get()));

        Optional<Employee> employee = employeeRepository.findByEmail(request.email());
        if (employee.isPresent() && passwordEncoder.matches(request.password(), employee.get().getPassword()))
            return new RegisterResponse(jwtService.generateToken(employee.get()));

        throw new AccessForbiddenException("Credenciales inválidas.");
    }

    @Transactional
    public RegisterResponse registerBusiness(RegisterBusinessRequest request) {
        if (businessRepository.existsByEmail(request.email()))
            throw new DuplicateResourceException("Ya existe un negocio registrado con ese email.");

        Business business = Business.builder()
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .brandName(request.brandName())
                .password(passwordEncoder.encode(request.password()))
                .build();
        businessRepository.save(business);

        String verificationToken = jwtService.generateEmailToken(business);
        emailService.sendVerificationEmail(business.getEmail(), business.getName(),
                baseUrl + "/api/v1/business/verify?token=" + verificationToken);

        return new RegisterResponse(jwtService.generateToken(business));
    }

    @Transactional
    public void verifyEmail(String token) {
        String email = jwtService.getSubject(token);
        String type = jwtService.getType(token);

        if (!"EMAIL_VERIFICATION".equals(type))
            throw new AccessForbiddenException("Token de verificación inválido.");

        Business business = businessRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (business.isEmailVerified())
            throw new InvalidOperationException("El email ya ha sido verificado.");

        business.setEmailVerified(true);
        businessRepository.save(business);
    }

    @Transactional
    public BusinessProfileResponse updateProfile(Long businessId, BusinessProfileRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        business.setBrandName(request.brandName());
        business.setThemeColor(request.themeColor());
        business.setLogoUrl(request.logoUrl());
        business.setHeroImageUrl(request.heroImageUrl());
        business.setRewardDescription(request.rewardDescription());
        business.setBookingUrl(request.bookingUrl());
        business.setInstagramUrl(request.instagramUrl());

        Business saved = businessRepository.save(business);
        googleWalletService.updateGenericClassForBusiness(saved);

        return new BusinessProfileResponse(
                saved.getId(), saved.getInviteCode(), saved.getBrandName(), saved.getThemeColor(),
                saved.getLogoUrl(), saved.getHeroImageUrl(), saved.getRewardDescription(),
                saved.getBookingUrl(), saved.getInstagramUrl()
        );
    }

    @Transactional
    public BusinessConfigResponse updateConfig(Long businessId, BusinessConfigRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (request.defaultMaxStamps() != null)
            business.setDefaultMaxStamps(request.defaultMaxStamps());
        if (request.averageTicketPrice() != null)
            business.setAverageTicketPrice(request.averageTicketPrice());

        businessRepository.save(business);
        return new BusinessConfigResponse(business.getDefaultMaxStamps(), business.getAverageTicketPrice());
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardMetrics(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        long totalCustomers = walletCardRepository.countByBusinessId(businessId);
        long totalStamps = scanLogRepository.countByWalletCardBusinessIdAndScanType(businessId, ScanType.EARN_STAMP);
        long totalRewards = scanLogRepository.countByWalletCardBusinessIdAndScanType(businessId, ScanType.REDEEM_REWARD);

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long cardsThisMonth = walletCardRepository.countByBusinessIdAndCreatedAtBetween(businessId, startOfMonth, LocalDateTime.now());

        List<ScanLog> recentLogs = scanLogRepository.findTop10ByWalletCardBusinessIdOrderByScannedAtDesc(businessId);
        List<ActivityLogResponse> activities = recentLogs.stream()
                .map(l -> new ActivityLogResponse(
                        l.getWalletCard().getCustomer().getName(),
                        l.getScanType() == ScanType.EARN_STAMP ? "Sello añadido" : "Premio canjeado",
                        l.getEmployee() != null ? l.getEmployee().getName() : "Dueño",
                        l.getScannedAt()
                )).toList();

        double ticketMedio = business.getAverageTicketPrice() != null ? business.getAverageTicketPrice() : 15.0;
        double ingresosRetenidos = totalStamps * ticketMedio;

        Double rawAverage = scanLogRepository.calculateAverageDaysBetweenVisits(businessId);
        int averageDays = rawAverage != null ? rawAverage.intValue() : 0;

        return new DashboardResponse(
                totalCustomers, cardsThisMonth, totalStamps, totalRewards,
                ingresosRetenidos, averageDays, activities
        );
    }

    @Transactional(readOnly = true)
    public List<VipCustomerResponse> getVipCustomers(Long businessId) {
        return scanLogRepository.findTopVipCustomers(businessId).stream()
                .map(p -> new VipCustomerResponse(p.getCustomer().getName(), p.getCustomer().getEmail(), p.getVisitCount()))
                .toList();
    }

    public List<AtRiskCustomerResponse> getAtRiskCustomers(Long businessId) {
        return scanLogRepository.findAtRiskCustomers(businessId, LocalDateTime.now().minusDays(60)).stream()
                .map(p -> new AtRiskCustomerResponse(p.getCustomer().getName(), p.getCustomer().getEmail(), p.getLastVisit()))
                .toList();
    }

    @Transactional
    public void undoScanLog(Long businessId, Long logId) {
        ScanLog scanLog = scanLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de escaneo no encontrado."));

        if (!scanLog.getWalletCard().getBusiness().getId().equals(businessId))
            throw new AccessForbiddenException("No tienes permiso para modificar este registro.");

        WalletCard card = scanLog.getWalletCard();
        if (scanLog.getScanType() == ScanType.EARN_STAMP)
            card.setCurrentStamps(Math.max(0, card.getCurrentStamps() - scanLog.getAmount()));
        else if (scanLog.getScanType() == ScanType.REDEEM_REWARD)
            card.setCurrentStamps(card.getMaxStamps());

        walletCardRepository.save(card);
        scanLogRepository.delete(scanLog);
    }

    @Transactional
    public void toggleCard(Long businessId, String secureUuid) {
        WalletCard card = walletCardRepository.findBySecureUuid(secureUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Tarjeta no encontrada."));

        if (!card.getBusiness().getId().equals(businessId))
            throw new AccessForbiddenException("Esta tarjeta no pertenece a tu negocio.");

        card.setIsActive(!card.getIsActive());
        walletCardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public void sendCampaign(Long businessId, CampaignRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));
        String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();

        List<Customer> targets = switch (request.segment()) {
            case AT_RISK -> scanLogRepository
                    .findAtRiskCustomers(businessId, LocalDateTime.now().minusDays(60))
                    .stream().map(ScanLogRepository.AtRiskCustomerProjection::getCustomer).toList();
            case VIP -> scanLogRepository
                    .findTopVipCustomers(businessId)
                    .stream().map(ScanLogRepository.VipCustomerProjection::getCustomer).toList();
            case ALL -> walletCardRepository
                    .findByBusinessId(businessId)
                    .stream().map(WalletCard::getCustomer).toList();
        };

        if (targets.isEmpty())
            throw new InvalidOperationException("No hay clientes en este segmento para enviar la campaña.");

        targets.forEach(customer -> {
            try {
                if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                    log.warn("Cliente {} sin email válido, omitido del envío.", customer.getId());
                    return;
                }
                WalletCard walletCard = walletCardRepository
                        .findByCustomerEmailAndBusinessId(customer.getEmail(), businessId).orElse(null);

                CampaignEvent event = new CampaignEvent(
                        customer.getEmail(), customer.getName(), brandName,
                        request.messageBody(), request.subject(),
                        walletCard != null ? walletCard.getId() : null
                );
                kafkaTemplate.send("campaign-notifications", objectMapper.writeValueAsString(event));
            } catch (Exception e) {
                log.error("Error serializando evento para {}: {}", customer.getEmail(), e.getMessage());
            }
        });
    }
}
