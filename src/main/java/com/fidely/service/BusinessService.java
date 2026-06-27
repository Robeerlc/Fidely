package com.fidely.service;

import com.fidely.dto.request.BusinessProfileRequest;
import com.fidely.dto.request.LoginRequest;
import com.fidely.dto.request.RegisterBusinessRequest;
import com.fidely.dto.response.BusinessProfileResponse;
import com.fidely.dto.response.RegisterResponse;
import com.fidely.dto.response.statistics.ActivityLogResponse;
import com.fidely.dto.response.statistics.DashboardResponse;
import com.fidely.entity.*;
import com.fidely.repository.BusinessRepository;
import com.fidely.repository.EmployeeRepository;
import com.fidely.repository.ScanLogRepository;
import com.fidely.repository.WalletCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final GoogleWalletService googleWalletService;
    private final WalletCardRepository walletCardRepository;
    private final ScanLogRepository scanLogRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;

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
        businessRepository.findById(businessId)
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

        return DashboardResponse.builder()
                .totalCustomers(totalCustomers)
                .totalStampsGiven(totalStamps)
                .totalRewardsRedeemed(totalRewards)
                .recentActivity(activities)
                .build();
    }
}