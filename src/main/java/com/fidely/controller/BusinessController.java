package com.fidely.controller;

import com.fidely.dto.request.BusinessProfileRequest;
import com.fidely.dto.request.CampaignRequest;
import com.fidely.dto.request.LoginRequest;
import com.fidely.dto.request.RegisterBusinessRequest;
import com.fidely.dto.response.BusinessProfileResponse;
import com.fidely.dto.response.RegisterResponse;
import com.fidely.dto.response.statistics.ActivityLogResponse;
import com.fidely.dto.response.statistics.CustomerSegmentResponse;
import com.fidely.dto.response.statistics.DashboardResponse;
import com.fidely.entity.Business;
import com.fidely.entity.ScanLog;
import com.fidely.repository.BusinessRepository;
import com.fidely.repository.ScanLogRepository;
import com.fidely.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;
    private final BusinessRepository businessRepository;
    private final ScanLogRepository scanLogRepository;

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

    @GetMapping("/{businessId}/segments/vip")
    public ResponseEntity<List<CustomerSegmentResponse>> getVipCustomers(@PathVariable Long businessId) {
        validateBusinessOwnership(businessId);
        return ResponseEntity.ok(businessService.getVipCustomers(businessId));
    }

    @GetMapping("/{businessId}/segments/at-risk")
    public ResponseEntity<List<CustomerSegmentResponse>> getAtRiskCustomers(@PathVariable Long businessId) {
        validateBusinessOwnership(businessId);
        return ResponseEntity.ok(businessService.getAtRiskCustomers(businessId));
    }

    @PostMapping("/{businessId}/campaigns")
    public ResponseEntity<String> launchCampaign(@PathVariable Long businessId, @Valid @RequestBody CampaignRequest request) {
        validateBusinessOwnership(businessId);
        businessService.launchCampaign(businessId, request);
        return ResponseEntity.ok("Campaña iniciada con éxito. Los correos se están enviando en segundo plano.");
    }

    @DeleteMapping("/{businessId}/logs/{logId}")
    public ResponseEntity<String> undoScan(@PathVariable Long businessId, @PathVariable Long logId) {
        validateBusinessOwnership(businessId);
        businessService.undoScanLog(businessId, logId);
        return ResponseEntity.ok("Acción anulada correctamente. La tarjeta del cliente ha sido actualizada.");
    }

    private void validateBusinessOwnership(Long businessId) {
        String ownerEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Business business = businessRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado."));

        if (!business.getId().equals(businessId))
            throw new RuntimeException("Acceso denegado. No tienes permiso para ver los datos de este negocio.");
    }
}