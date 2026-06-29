package com.upi.psp.repository;

import com.upi.psp.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.payerVpa = :payerVpa AND t.payeeVpa = :payeeVpa AND t.amountPaise = :amountPaise AND t.createdAt > :cutoffTime AND t.status NOT IN ('FAILED', 'REVERSED')")
    List<Transaction> findDuplicateTransactions(
            @Param("payerVpa") String payerVpa,
            @Param("payeeVpa") String payeeVpa,
            @Param("amountPaise") Long amountPaise,
            @Param("cutoffTime") LocalDateTime cutoffTime
    );
}
