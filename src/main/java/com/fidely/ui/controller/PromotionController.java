package com.fidely.ui.controller;

import com.fidely.domain.dto.request.CreatePromotionRequest;
import com.fidely.domain.dto.response.promotion.PromotionRankingResponse;
import com.fidely.domain.dto.response.promotion.PromotionResponse;
import com.fidely.domain.entity.Business;
import com.fidely.domain.exception.AccessForbiddenException;
import com.fidely.domain.exception.ResourceNotFoundException;
import com.fidely.domain.exception.SubscriptionInactiveException;
import com.fidely.dao.repository.BusinessRepository;
import com.fidely.domain.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/business/{businessId}/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final BusinessRepository businessRepository;

    @PostMapping
    public ResponseEntity<PromotionResponse> createPromotion(
            @PathVariable Long businessId,
            @Valid @RequestBody CreatePromotionRequest request) {
        validateOwnership(businessId);
        return new ResponseEntity<>(promotionService.createPromotion(businessId, request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getPromotions(@PathVariable Long businessId) {
        validateOwnership(businessId);
        return ResponseEntity.ok(promotionService.getPromotions(businessId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PromotionResponse>> getActivePromotions(@PathVariable Long businessId) {
        validateOwnership(businessId);
        return ResponseEntity.ok(promotionService.getActivePromotions(businessId));
    }

    @GetMapping("/{promotionId}/ranking")
    public ResponseEntity<PromotionRankingResponse> getRanking(
            @PathVariable Long businessId, @PathVariable Long promotionId) {
        validateOwnership(businessId);
        return ResponseEntity.ok(promotionService.getRanking(businessId, promotionId));
    }

    @PutMapping("/{promotionId}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long businessId,
            @PathVariable Long promotionId,
            @Valid @RequestBody CreatePromotionRequest request) {
        validateOwnership(businessId);
        return ResponseEntity.ok(promotionService.updatePromotion(businessId, promotionId, request));
    }

    @PatchMapping("/{promotionId}/toggle")
    public ResponseEntity<PromotionResponse> togglePromotion(
            @PathVariable Long businessId, @PathVariable Long promotionId) {
        validateOwnership(businessId);
        return ResponseEntity.ok(promotionService.togglePromotion(businessId, promotionId));
    }

    @DeleteMapping("/{promotionId}")
    public ResponseEntity<Void> deletePromotion(
            @PathVariable Long businessId, @PathVariable Long promotionId) {
        validateOwnership(businessId);
        promotionService.deletePromotion(businessId, promotionId);
        return ResponseEntity.noContent().build();
    }

    private void validateOwnership(Long businessId) {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        Business business = businessRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (!business.getId().equals(businessId))
            throw new AccessForbiddenException("No tienes permiso para acceder a los datos de este negocio.");

        if (!business.isSubscriptionActive())
            throw new SubscriptionInactiveException("Tu suscripción está inactiva. Por favor, actualiza tu método de pago.");
    }
}
