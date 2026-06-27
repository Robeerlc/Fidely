package com.fidely.service;

import com.fidely.entity.Customer;
import com.fidely.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer getOrCreateCustomer(String name, String phoneNumber, String email) {
        return customerRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .name(name)
                            .phoneNumber(phoneNumber)
                            .email(email)
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }
}