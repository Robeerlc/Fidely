package com.fidely.repository;

import com.fidely.entity.ScanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {

    List<ScanLog> findByWalletCardIdOrderByScannedAtDesc(Long walletCardId);

    List<ScanLog> findByWalletCard_Business_Id(Long walletCardId);
}
