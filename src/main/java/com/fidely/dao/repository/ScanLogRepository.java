package com.fidely.dao.repository;

import com.fidely.domain.entity.Customer;
import com.fidely.domain.entity.ScanLog;
import com.fidely.domain.entity.ScanType;
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

    @Query("SELECT l.walletCard.customer AS customer, COUNT(l) AS visitCount FROM ScanLog l " +
            "WHERE l.walletCard.business.id = :businessId AND l.scanType = 'EARN_STAMP' " +
            "GROUP BY l.walletCard.customer " +
            "ORDER BY visitCount DESC")
    List<VipCustomerProjection> findTopVipCustomers(@Param("businessId") Long businessId);

    @Query("SELECT l.walletCard.customer AS customer, MAX(l.scannedAt) AS lastVisit FROM ScanLog l " +
            "WHERE l.walletCard.business.id = :businessId " +
            "GROUP BY l.walletCard.customer " +
            "HAVING MAX(l.scannedAt) < :limitDate " +
            "ORDER BY lastVisit ASC")
    List<AtRiskCustomerProjection> findAtRiskCustomers(@Param("businessId") Long businessId, @Param("limitDate") LocalDateTime limitDate);

    @Query("SELECT MIN(l.scannedAt) as firstVisit, MAX(l.scannedAt) as lastVisit, COUNT(l) as visitCount " +
            "FROM ScanLog l WHERE l.walletCard.business.id = :businessId AND l.scanType = 'EARN_STAMP' " +
            "GROUP BY l.walletCard.id HAVING COUNT(l) > 1")
    List<VisitStatsProjection> findVisitStatsByBusiness(@Param("businessId") Long businessId);

    // Clientes VIP: los que más sellos han ganado ordenados de mayor a menor
    interface VipCustomerProjection {
        Customer getCustomer();

        Long getVisitCount();
    }

    // Clientes en Riesgo: cuya última visita es anterior a una fecha límite
    interface AtRiskCustomerProjection {
        Customer getCustomer();

        LocalDateTime getLastVisit();
    }

    interface VisitStatsProjection {
        LocalDateTime getFirstVisit();

        LocalDateTime getLastVisit();

        Long getVisitCount();
    }
}