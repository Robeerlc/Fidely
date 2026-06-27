package com.fidely.domain.service;

import com.fidely.dao.repository.CustomerRepository;
import com.fidely.domain.entity.Customer;
import com.fidely.ui.dto.business.RegisterBusinessResponse;
import com.fidely.ui.dto.costumer.RegisterCustumerRequest;
import com.fidely.ui.dto.costumer.RegisterCustumerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustumerService {
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;

    public RegisterCustumerResponse registerBusiness(RegisterCustumerRequest request) {
        if(customerRepository.existsByPhoneNumber(request.phoneNumber())) return null;
        Customer customer = Customer.builder()
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .build();
        customerRepository.save(customer);
        return new RegisterCustumerResponse(jwtService.generateToken(customer));
    }
}
