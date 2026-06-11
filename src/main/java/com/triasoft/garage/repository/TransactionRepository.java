package com.triasoft.garage.repository;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByPaymentAccountIdOrderByTransactionDateDescCreatedAtDesc(Long accountId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.paymentAccount.id = :accountId AND t.direction = :direction")
    BigDecimal sumAmountByAccountAndDirection(@Param("accountId") Long accountId, @Param("direction") TransactionDirectionEnum direction);

    Optional<Transaction> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    boolean existsByReversalOfId(Long transactionId);

    boolean existsByPaymentAccountId(Long accountId);

}
