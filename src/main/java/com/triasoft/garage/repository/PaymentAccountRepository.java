package com.triasoft.garage.repository;

import com.triasoft.garage.entity.PaymentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long> {

    List<PaymentAccount> findAllByIsActiveTrue();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByAccountNoIgnoreCase(String accountNo);

    boolean existsByAccountNoIgnoreCaseAndIdNot(String accountNo, Long id);

}
