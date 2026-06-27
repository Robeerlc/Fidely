package com.fidely.domain.service;

import com.fidely.dao.repository.BusinessRepository;
import com.fidely.domain.entity.Business;
import com.fidely.ui.dto.business.RegisterBusinessRequest;
import com.fidely.ui.dto.business.RegisterBusinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final BusinessRepository businessRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public RegisterBusinessResponse registerBusiness(RegisterBusinessRequest request) {
        if(businessRepository.existsByEmail(request.email())) return null;
        Business business = Business.builder()
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .brandName(request.brandName())
                .password(passwordEncoder.encode(request.password()))
                .build();
        businessRepository.save(business);
        return new RegisterBusinessResponse(jwtService.generateToken(business));
    }
}
