package com.fidely.ui.controller;

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
import com.fidely.domain.entity.Business;
import com.fidely.domain.entity.ScanLog;
import com.fidely.domain.exception.SubscriptionInactiveException;
import com.fidely.dao.repository.BusinessRepository;
import com.fidely.dao.repository.ScanLogRepository;
import com.fidely.domain.service.BusinessService;
import com.fidely.domain.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;
    private final BusinessRepository businessRepository;
    private final ScanLogRepository scanLogRepository;
    private final FileStorageService fileStorageService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerBusiness(@Valid @RequestBody RegisterBusinessRequest request) {
        RegisterResponse response = businessService.registerBusiness(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<RegisterResponse> login(@Valid @RequestBody LoginRequest request) {
        RegisterResponse response = businessService.login(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{businessId}/profile")
    public ResponseEntity<BusinessProfileResponse> updateProfile(@PathVariable Long businessId, @Valid @RequestBody BusinessProfileRequest request) {
        validateBusinessOwnership(businessId);
        BusinessProfileResponse response = businessService.updateProfile(businessId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{businessId}/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardMetrics(@PathVariable Long businessId) {
        validateBusinessOwnership(businessId);
        DashboardResponse response = businessService.getDashboardMetrics(businessId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<ActivityLogResponse>> getBusinessLogs() {
        String ownerEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Business business = businessRepository.findByEmail(ownerEmail).orElseThrow();
        List<ScanLog> logs = scanLogRepository.findByWalletCard_Business_Id(business.getId());

        List<ActivityLogResponse> response = logs.stream().map(log -> ActivityLogResponse.builder()
                .customerName(log.getWalletCard().getCustomer().getName())
                .action(log.getScanType().toString())
                .employeeName(log.getEmployee() != null ? log.getEmployee().getName() : "Dueño")
                .timestamp(log.getScannedAt())
                .build()).toList();
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{businessId}/logs/{logId}")
    public ResponseEntity<String> undoScan(@PathVariable Long businessId, @PathVariable Long logId) {
        validateBusinessOwnership(businessId);
        businessService.undoScanLog(businessId, logId);
        return ResponseEntity.ok("Acción anulada correctamente. La tarjeta del cliente ha sido actualizada.");
    }

    @PostMapping("/{businessId}/upload-image")
    public ResponseEntity<String> uploadImage(@PathVariable Long businessId, @RequestParam("file") MultipartFile file) {
        validateBusinessOwnership(businessId);
        String fileUrl = fileStorageService.storeFile(file);
        return ResponseEntity.ok(fileUrl);
    }

    @GetMapping("/{businessId}/customers/vip")
    public ResponseEntity<List<VipCustomerResponse>> getVipCustomers(@PathVariable Long businessId) {
        validateBusinessOwnership(businessId);
        return ResponseEntity.ok(businessService.getVipCustomers(businessId));
    }

    @GetMapping("/{businessId}/customers/at-risk")
    public ResponseEntity<List<AtRiskCustomerResponse>> getAtRiskCustomers(@PathVariable Long businessId) {
        validateBusinessOwnership(businessId);
        return ResponseEntity.ok(businessService.getAtRiskCustomers(businessId));
    }

    @PostMapping("/{businessId}/campaigns")
    public ResponseEntity<Void> sendCampaign(@PathVariable Long businessId,
                                             @Valid @RequestBody CampaignRequest request) {
        validateBusinessOwnership(businessId);
        businessService.sendCampaign(businessId, request);
        return ResponseEntity.accepted().build();
    }

    private void validateBusinessOwnership(Long businessId) {
        String ownerEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Business business = businessRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado."));

        if (!business.getId().equals(businessId))
            throw new RuntimeException("Acceso denegado. No tienes permiso para ver los datos de este negocio.");

        if (!business.isSubscriptionActive())
            throw new SubscriptionInactiveException("Tu suscripción está inactiva. Por favor, actualiza tu método de pago para ver tus métricas y campañas.");
    }
}