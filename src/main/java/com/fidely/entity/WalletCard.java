package com.fidely.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "secure_uuid", nullable = false, unique = true, updatable = false)
    private String secureUuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Builder.Default
    @Column(name = "current_stamps", nullable = false)
    private Integer currentStamps = 0;

    @Builder.Default
    @Column(name = "max_stamps", nullable = false)
    private Integer maxStamps = 10;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;
}
