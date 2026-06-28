package com.fidely.repository;

import com.fidely.entity.Customer;
import com.fidely.entity.ScanLog;
import com.fidely.entity.ScanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScanLogRepository extends JpaRepository<ScanLog, Long> {

    long countByWalletCardBusinessIdAndScanType(Long businessId, ScanType scanType);

    List<ScanLog> findTop10ByWalletCardBusinessIdOrderByScannedAtDesc(Long businessId);

    List<ScanLog> findByWalletCard_Business_Id(Long walletCardBusinessId);

    // Clientes VIP: los que más sellos han ganado ordenados de mayor a menor
    interface VipCustomerProjection {
        Customer getCustomer();
        Long getVisitCount();
    }

    @Query("SELECT l.walletCard.customer AS customer, COUNT(l) AS visitCount FROM ScanLog l " +
            "WHERE l.walletCard.business.id = :businessId AND l.scanType = 'EARN_STAMP' " +
            "GROUP BY l.walletCard.customer " +
            "ORDER BY visitCount DESC")
    List<VipCustomerProjection> findTopVipCustomers(@Param("businessId") Long businessId);

    // Clientes en Riesgo: cuya última visita es anterior a una fecha límite
    interface AtRiskCustomerProjection {
        Customer getCustomer();
        LocalDateTime getLastVisit();
    }

    @Query("SELECT l.walletCard.customer AS customer, MAX(l.scannedAt) AS lastVisit FROM ScanLog l " +
            "WHERE l.walletCard.business.id = :businessId " +
            "GROUP BY l.walletCard.customer " +
            "HAVING MAX(l.scannedAt) < :limitDate " +
            "ORDER BY lastVisit ASC")
    List<AtRiskCustomerProjection> findAtRiskCustomers(@Param("businessId") Long businessId, @Param("limitDate") LocalDateTime limitDate);
}