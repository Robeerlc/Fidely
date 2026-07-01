package com.fidely.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business implements User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "invite_code", nullable = false, unique = true, updatable = false)
    private String inviteCode = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone")
    private String phoneNumber;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "theme_color", length = 7)
    private String themeColor;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "hero_image_url", length = 1000)
    private String heroImageUrl;

    @Column(name = "reward_description")
    private String rewardDescription;

    @Column(name = "booking_url", length = 1000)
    private String bookingUrl;

    @Column(name = "instagram_url", length = 1000)
    private String instagramUrl;

    @Builder.Default
    @Column(name = "is_subscription_active", nullable = false)
    private boolean isSubscriptionActive = false;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Builder.Default
    @Column(name = "default_max_stamps", nullable = false)
    private Integer defaultMaxStamps = 10;

    @Builder.Default
    @Column(name = "average_ticket_price", nullable = false)
    private Double averageTicketPrice = 15.0;

    // Si borro la peluquería, borra también a sus empleados
    @OneToMany(mappedBy = "business", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Employee> employees;

    // Si borro la peluquería, borra también sus promociones
    @OneToMany(mappedBy = "business", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Promotion> promotions;

    @OneToMany(mappedBy = "business", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ServiceItem> services;

    @Builder.Default
    @Column(name = "current_multiplier", nullable = false)
    private Integer currentMultiplier = 1;

    @Builder.Default
    @Column(nullable = false, name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public String getTokenSubject() {
        return this.email;
    }

    @Override
    public String getRole() {
        return "BUSINESS";
    }
}