package com.fidely.service;

import com.fidely.dto.RegisterCustomerRequest;
import com.fidely.dto.RegisterResponse;
import com.fidely.entity.Customer;
import com.fidely.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;

    public RegisterResponse registerCustomer(RegisterCustomerRequest request) {
        if (customerRepository.existsByPhoneNumber(request.phoneNumber())) return null;
        Customer customer = Customer.builder()
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .build();
        customerRepository.save(customer);
        return new RegisterResponse(jwtService.generateToken(customer));
    }
}