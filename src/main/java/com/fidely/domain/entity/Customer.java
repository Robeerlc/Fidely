package com.fidely.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer implements User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Builder.Default
    @Column(nullable = false, name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public String getTokenSubject() {
        return this.email;
    }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }
}