package com.fidely.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fidely.domain.dto.CampaignEvent;
import com.fidely.domain.dto.request.BusinessProfileRequest;
import com.fidely.domain.dto.request.CampaignRequest;
import com.fidely.domain.dto.request.LoginRequest;
import com.fidely.domain.dto.request.RegisterBusinessRequest;
import com.fidely.domain.dto.response.AtRiskCustomerResponse;
import com.fidely.domain.dto.response.BusinessProfileResponse;
import com.fidely.domain.dto.response.RegisterResponse;
import com.fidely.domain.dto.response.VipCustomerResponse;
import com.fidely.domain.dto.response.statistics.ActivityLogResponse;
import com.fidely.domain.dto.response.statistics.CustomerSegmentResponse;
import com.fidely.domain.dto.response.statistics.DashboardResponse;
import com.fidely.domain.entity.*;
import com.fidely.dao.repository.BusinessRepository;
import com.fidely.dao.repository.EmployeeRepository;
import com.fidely.dao.repository.ScanLogRepository;
import com.fidely.dao.repository.WalletCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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

    public RegisterResponse login(LoginRequest request) {
        Optional<Business> business = businessRepository.findByEmail(request.email());
        if (business.isPresent() && passwordEncoder.matches(request.password(), business.get().getPassword()))
            return new RegisterResponse(jwtService.generateToken(business.get()));

        Optional<Employee> employee = employeeRepository.findByEmail(request.email());
        if (employee.isPresent() && passwordEncoder.matches(request.password(), employee.get().getPassword()))
            return new RegisterResponse(jwtService.generateToken(employee.get()));

        throw new RuntimeException("Credenciales inválidas");
    }

    @Transactional
    public RegisterResponse registerBusiness(RegisterBusinessRequest request) {
        if (businessRepository.existsByEmail(request.email())) return null;
        Business business = Business.builder()
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .brandName(request.brandName())
                .password(passwordEncoder.encode(request.password()))
                .build();
        businessRepository.save(business);
        return new RegisterResponse(jwtService.generateToken(business));
    }

    @Transactional
    public BusinessProfileResponse updateProfile(Long businessId, BusinessProfileRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado."));

        business.setBrandName(request.getBrandName());
        business.setThemeColor(request.getThemeColor());
        business.setLogoUrl(request.getLogoUrl());
        business.setHeroImageUrl(request.getHeroImageUrl());
        business.setRewardDescription(request.getRewardDescription());
        business.setBookingUrl(request.getBookingUrl());
        business.setInstagramUrl(request.getInstagramUrl());

        Business updatedBusiness = businessRepository.save(business);
        googleWalletService.updateGenericClassForBusiness(updatedBusiness);

        return BusinessProfileResponse.builder()
                .id(updatedBusiness.getId())
                .brandName(updatedBusiness.getBrandName())
                .themeColor(updatedBusiness.getThemeColor())
                .logoUrl(updatedBusiness.getLogoUrl())
                .heroImageUrl(updatedBusiness.getHeroImageUrl())
                .rewardDescription(updatedBusiness.getRewardDescription())
                .bookingUrl(updatedBusiness.getBookingUrl())
                .instagramUrl(updatedBusiness.getInstagramUrl())
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardMetrics(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado."));

        long totalCustomers = walletCardRepository.countByBusinessId(businessId);
        long totalStamps = scanLogRepository.countByWalletCardBusinessIdAndScanType(businessId, ScanType.EARN_STAMP);
        long totalRewards = scanLogRepository.countByWalletCardBusinessIdAndScanType(businessId, ScanType.REDEEM_REWARD);
        List<ScanLog> recentLogs = scanLogRepository.findTop10ByWalletCardBusinessIdOrderByScannedAtDesc(businessId);

        List<ActivityLogResponse> activities = recentLogs.stream()
                .map(log -> {
                    String action = log.getScanType() == ScanType.EARN_STAMP ? "Sello añadido ✂️" : "Premio canjeado 🎁";
                    return ActivityLogResponse.builder()
                            .customerName(log.getWalletCard().getCustomer().getName())
                            .action(action)
                            .employeeName(log.getEmployee() != null ? log.getEmployee().getName() : "Dueño")
                            .timestamp(log.getScannedAt())
                            .build();
                }).toList();

        double ticketMedio = business.getAverageTicketPrice() != null ? business.getAverageTicketPrice() : 15.0;
        Double ingresosRetenidos = totalStamps * ticketMedio;

        List<ScanLogRepository.VisitStatsProjection> stats = scanLogRepository.findVisitStatsByBusiness(businessId);
        int averageDays = 0;
        if (!stats.isEmpty()) {
            long totalDaysSum = 0;
            long totalValidCustomers = 0;
            for (var stat : stats) {
                long daysBetween = Duration.between(stat.getFirstVisit(), stat.getLastVisit()).toDays();
                long intervals = stat.getVisitCount() - 1;
                if (intervals > 0) {
                    totalDaysSum += (daysBetween / intervals);
                    totalValidCustomers++;
                }
            }
            averageDays = totalValidCustomers > 0 ? (int) (totalDaysSum / totalValidCustomers) : 0;
        }

        return DashboardResponse.builder()
                .totalCustomers(totalCustomers)
                .totalStampsGiven(totalStamps)
                .totalRewardsRedeemed(totalRewards)
                .estimatedRetainedRevenue(ingresosRetenidos)
                .averageDaysBetweenVisits(averageDays)
                .recentActivity(activities)
                .build();
    }

    @Transactional(readOnly = true)
    public List<VipCustomerResponse> getVipCustomers(Long businessId) {
        return scanLogRepository.findTopVipCustomers(businessId).stream()
                .map(p -> new VipCustomerResponse(
                        p.getCustomer().getName(),
                        p.getCustomer().getEmail(),
                        p.getVisitCount()
                ))
                .toList();
    }



    @Transactional
    public void undoScanLog(Long businessId, Long logId) {
        ScanLog log = scanLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Registro no encontrado."));

        if (!log.getWalletCard().getBusiness().getId().equals(businessId))
            throw new RuntimeException("No tienes permiso para modificar este registro.");

        WalletCard card = log.getWalletCard();

        if (log.getScanType() == ScanType.EARN_STAMP)
            card.setCurrentStamps(Math.max(0, card.getCurrentStamps() - log.getAmount()));
        else if (log.getScanType() == ScanType.REDEEM_REWARD) card.setCurrentStamps(card.getMaxStamps());

        walletCardRepository.save(card);
        scanLogRepository.delete(log);
    }

    public List<AtRiskCustomerResponse> getAtRiskCustomers(Long businessId) {
        LocalDateTime limitDate = LocalDateTime.now().minusDays(60);
        return scanLogRepository.findAtRiskCustomers(businessId, limitDate).stream()
                .map(p -> new AtRiskCustomerResponse(
                        p.getCustomer().getName(),
                        p.getCustomer().getEmail(),
                        p.getLastVisit()
                ))
                .toList();
    }


    @Transactional(readOnly = true)
    public void sendCampaign(Long businessId, CampaignRequest request) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado."));
        String brandName = business.getBrandName() != null ? business.getBrandName() : business.getName();

        List<Customer> targets = switch (request.getSegment()) {
            case AT_RISK -> scanLogRepository
                    .findAtRiskCustomers(businessId, LocalDateTime.now().minusDays(60))
                    .stream().map(p -> p.getCustomer()).toList();
            case VIP -> scanLogRepository
                    .findTopVipCustomers(businessId)
                    .stream().map(p -> p.getCustomer()).toList();
            case ALL -> walletCardRepository
                    .findByBusinessId(businessId)
                    .stream().map(WalletCard::getCustomer).toList();
            default -> throw new RuntimeException("Segmento no válido.");
        };

        if (targets.isEmpty())
            throw new RuntimeException("No hay clientes en este segmento para enviar la campaña.");

        targets.forEach(customer -> {
            try {
                if (customer.getEmail() == null || customer.getEmail().isBlank()) {
                    log.warn("Cliente {} sin email válido, se omite del envío.", customer.getId());
                    return;
                }

                WalletCard walletCard = walletCardRepository
                        .findByCustomerEmailAndBusinessId(customer.getEmail(), businessId)
                        .orElse(null);

                CampaignEvent event = new CampaignEvent(
                        customer.getEmail(),
                        customer.getName(),
                        brandName,
                        request.getMessageBody(),
                        request.getSubject(),
                        walletCard != null ? walletCard.getId() : null
                );

                kafkaTemplate.send("campaign-notifications", objectMapper.writeValueAsString(event));
            } catch (Exception e) {
                log.error("Error serializando evento para {}: {}", customer.getEmail(), e.getMessage());
            }
        });
    }
}