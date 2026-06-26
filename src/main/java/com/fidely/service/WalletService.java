package com.fidely.service;

import com.fidely.dto.CreateCardRequest;
import com.fidely.entity.Business;
import com.fidely.entity.Customer;
import com.fidely.entity.WalletCard;
import com.fidely.repository.BusinessRepository;
import com.fidely.repository.CustomerRepository;
import com.fidely.repository.WalletCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- IMPORTANTE

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final WalletCardRepository walletCardRepository;

    @Transactional
    public WalletCard createCardForCustomer(CreateCardRequest request) {
        Business business = businessRepository.findById(request.businessId())
                .orElseThrow(() -> new NoSuchElementException("Error: La peluquería no existe."));

        Customer customer = customerRepository.findByPhoneNumber(request.customerPhone())
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .name(request.customerName())
                            .phoneNumber(request.customerPhone())
                            .email(request.customerEmail())
                            .build();
                    return customerRepository.save(newCustomer);
                });

        Optional<WalletCard> existingCard = walletCardRepository.findByCustomerAndBusiness(customer, business);
        if (existingCard.isPresent()) return existingCard.get();

        WalletCard newCard = WalletCard.builder()
                .customer(customer)
                .business(business)
                .build();
        return walletCardRepository.save(newCard);
    }
}