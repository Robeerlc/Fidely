package com.fidely.repository;

import com.fidely.entity.ScanLog;
import com.fidely.entity.ScanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {

    long countByWalletCardBusinessIdAndScanType(Long businessId, ScanType scanType);

    List<ScanLog> findTop10ByWalletCardBusinessIdOrderByScannedAtDesc(Long businessId);

}