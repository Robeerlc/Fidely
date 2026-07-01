package com.fidely.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scan_logs", indexes = {
        @Index(name = "idx_scan_business_date", columnList = "wallet_card_id, scanned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ScanLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_card_id", nullable = false)
    private WalletCard walletCard;

    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type", nullable = false)
    private ScanType scanType;

    @Builder.Default
    @Column(name = "scanned_at", nullable = false, updatable = false)
    private LocalDateTime scannedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Builder.Default
    @Column(name = "amount", nullable = false)
    private Integer amount = 1;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "scan_log_services",
            joinColumns = @JoinColumn(name = "scan_log_id"),
            inverseJoinColumns = @JoinColumn(name = "service_item_id")
    )
    private List<ServiceItem> services = new ArrayList<>();

    @Column(name = "estimated_revenue")
    private Double estimatedRevenue;
}