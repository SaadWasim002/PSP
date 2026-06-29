package com.upi.psp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vpa_registry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VpaRegistry {

    @Id
    @Column(name = "vpa", nullable = false, length = 100)
    private String vpa;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "account_holder", nullable = false, length = 100)
    private String accountHolder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
