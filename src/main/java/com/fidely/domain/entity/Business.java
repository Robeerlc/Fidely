package com.fidely.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business implements User{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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