package com.fidely.dao.repository;

import com.fidely.domain.entity.Customer;
import com.fidely.domain.entity.ScanLog;
import com.fidely.domain.entity.ScanType;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT l.walletCard.customer AS customer, COUNT(l) AS visitCount FROM ScanLog l " +
            "WHERE l.walletCard.business.id = :businessId AND l.scanType = 'EARN_STAMP' " +
            "GROUP BY l.walletCard.customer ORDER BY visitCount DESC")
    List<VipCustomerProjection> findTopCustomers(@Param("businessId") Long businessId, Pageable pageable);

    @Query("SELECT l.walletCard.customer AS customer, COUNT(l) AS visitCount FROM ScanLog l " +
            "WHERE l.walletCard.business.id = :businessId AND l.scanType = 'EARN_STAMP' " +
            "AND l.scannedAt BETWEEN :from AND :to " +
            "GROUP BY l.walletCard.customer ORDER BY visitCount DESC")
    List<VipCustomerProjection> findTopCustomersBetween(
            @Param("businessId") Long businessId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query(value = "SELECT COALESCE(AVG(DATEDIFF(last_visit, first_visit) / intervals), 0) " +
            "FROM ( " +
            "  SELECT MAX(s.scanned_at) as last_visit, " +
            "         MIN(s.scanned_at) as first_visit, " +
            "         (COUNT(s.id) - 1) as intervals " +
            "  FROM scan_logs s " +
            "  INNER JOIN wallet_cards w ON s.wallet_card_id = w.id " +
            "  WHERE w.business_id = :businessId AND s.scan_type = 'EARN_STAMP' " +
            "  GROUP BY s.wallet_card_id " +
            "  HAVING COUNT(s.id) > 1" +
            ") as subquery", nativeQuery = true)
    Double calculateAverageDaysBetweenVisits(@Param("businessId") Long businessId);

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
}