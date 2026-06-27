package com.fidely.service;

import com.fidely.dto.BusinessProfileRequest;
import com.fidely.dto.BusinessProfileResponse;
import com.fidely.entity.Business;
import com.fidely.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final GoogleWalletService googleWalletService;

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
}