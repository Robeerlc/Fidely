package com.fidely.repository;

import com.fidely.entity.WalletCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletCardRepository extends JpaRepository<WalletCard, Long> {
    Optional<WalletCard> findBySecureUuid(String secureUuid);

    List<WalletCard> findByCustomerId(Long customerId);

    long countByBusinessIdAndIsActiveTrue(Long businessId);
}
