package com.fidely.domain.service;

import com.fidely.dao.repository.BusinessRepository;
import com.fidely.dao.repository.PromotionRepository;
import com.fidely.dao.repository.ScanLogRepository;
import com.fidely.domain.dto.request.CreatePromotionRequest;
import com.fidely.domain.dto.response.promotion.PromotionRankingResponse;
import com.fidely.domain.dto.response.promotion.PromotionResponse;
import com.fidely.domain.dto.response.promotion.RankingEntryResponse;
import com.fidely.domain.entity.Promotion;
import com.fidely.domain.exception.AccessForbiddenException;
import com.fidely.domain.exception.InvalidOperationException;
import com.fidely.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final BusinessRepository businessRepository;
    private final ScanLogRepository scanLogRepository;

    @Transactional
    public PromotionResponse createPromotion(Long businessId, CreatePromotionRequest request) {
        var business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio no encontrado."));

        if (!request.permanent()) {
            if (request.startDate() == null || request.endDate() == null)
                throw new InvalidOperationException("Las promociones de duración limitada requieren fecha de inicio y fin.");
            if (!request.endDate().isAfter(request.startDate()))
                throw new InvalidOperationException("La fecha de fin debe ser posterior a la de inicio.");
        }

        Promotion promotion = Promotion.builder()
                .business(business)
                .name(request.name())
                .benefitDescription(request.benefitDescription())
                .topN(request.topN())
                .permanent(request.permanent())
                .startDate(request.permanent() ? null : request.startDate())
                .endDate(request.permanent() ? null : request.endDate())
                .build();

        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> getPromotions(Long businessId) {
        validateBusinessExists(businessId);
        return promotionRepository.findByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> getActivePromotions(Long businessId) {
        validateBusinessExists(businessId);
        return promotionRepository.findByBusinessIdAndActiveTrueOrderByCreatedAtDesc(businessId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PromotionRankingResponse getRanking(Long businessId, Long promotionId) {
        Promotion promotion = promotionRepository.findByIdAndBusinessId(promotionId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada."));

        var pageable = PageRequest.of(0, promotion.getTopN());

        List<ScanLogRepository.VipCustomerProjection> ranked;
        if (promotion.isPermanent()) {
            ranked = scanLogRepository.findTopCustomers(businessId, pageable);
        } else {
            LocalDateTime from = promotion.getStartDate().atStartOfDay();
            LocalDateTime to = promotion.getEndDate().atTime(23, 59, 59);
            ranked = scanLogRepository.findTopCustomersBetween(businessId, from, to, pageable);
        }

        List<RankingEntryResponse> entries = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            var p = ranked.get(i);
            entries.add(new RankingEntryResponse(
                    i + 1,
                    p.getCustomer().getName(),
                    p.getCustomer().getEmail(),
                    p.getVisitCount()
            ));
        }

        return new PromotionRankingResponse(toResponse(promotion), entries);
    }

    @Transactional
    public PromotionResponse updatePromotion(Long businessId, Long promotionId, CreatePromotionRequest request) {
        Promotion promotion = promotionRepository.findByIdAndBusinessId(promotionId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada."));

        if (!request.permanent()) {
            if (request.startDate() == null || request.endDate() == null)
                throw new InvalidOperationException("Las promociones de duración limitada requieren fecha de inicio y fin.");
            if (!request.endDate().isAfter(request.startDate()))
                throw new InvalidOperationException("La fecha de fin debe ser posterior a la de inicio.");
        }

        promotion.setName(request.name());
        promotion.setBenefitDescription(request.benefitDescription());
        promotion.setTopN(request.topN());
        promotion.setPermanent(request.permanent());
        promotion.setStartDate(request.permanent() ? null : request.startDate());
        promotion.setEndDate(request.permanent() ? null : request.endDate());

        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public PromotionResponse togglePromotion(Long businessId, Long promotionId) {
        Promotion promotion = promotionRepository.findByIdAndBusinessId(promotionId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada."));

        promotion.setActive(!promotion.isActive());
        return toResponse(promotionRepository.save(promotion));
    }

    @Transactional
    public void deletePromotion(Long businessId, Long promotionId) {
        Promotion promotion = promotionRepository.findByIdAndBusinessId(promotionId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada."));
        promotionRepository.delete(promotion);
    }

    private void validateBusinessExists(Long businessId) {
        if (!businessRepository.existsById(businessId))
            throw new ResourceNotFoundException("Negocio no encontrado.");
    }

    private boolean isCurrentlyRunning(Promotion promotion) {
        if (!promotion.isActive()) return false;
        if (promotion.isPermanent()) return true;
        LocalDate today = LocalDate.now();
        return !today.isBefore(promotion.getStartDate()) && !today.isAfter(promotion.getEndDate());
    }

    private PromotionResponse toResponse(Promotion p) {
        return new PromotionResponse(
                p.getId(), p.getName(), p.getBenefitDescription(),
                p.getTopN(), p.isPermanent(), p.getStartDate(), p.getEndDate(),
                p.isActive(), isCurrentlyRunning(p), p.getCreatedAt()
        );
    }
}
