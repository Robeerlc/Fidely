package com.fidely.domain.service;

import com.fidely.domain.entity.Customer;
import com.fidely.dao.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer getOrCreateCustomer(String name, String phoneNumber, String email) {
        Optional<Customer> existing = customerRepository.findByPhoneNumberOrEmail(phoneNumber, email);

        if (existing.isPresent()) {
            Customer customer = existing.get();
            customer.setName(name);
            customer.setPhoneNumber(phoneNumber);
            customer.setEmail(email);
            return customerRepository.save(customer);
        }

        Customer newCustomer = Customer.builder()
                .name(name)
                .phoneNumber(phoneNumber)
                .email(email)
                .build();
        return customerRepository.save(newCustomer);
    }
}