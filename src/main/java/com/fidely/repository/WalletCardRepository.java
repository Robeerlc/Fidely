package com.fidely.repository;

import com.fidely.entity.Business;
import com.fidely.entity.Customer;
import com.fidely.entity.WalletCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletCardRepository extends JpaRepository<WalletCard, Long> {
    Optional<WalletCard> findBySecureUuid(String secureUuid);

    long countByBusinessId(Long businessId);

    Optional<WalletCard> findByCustomerAndBusiness(Customer customer, Business business);

    List<WalletCard> findByBusinessId(Long businessId);

    Optional<WalletCard> findByCustomerEmailAndBusinessId(String email, Long businessId);
}
