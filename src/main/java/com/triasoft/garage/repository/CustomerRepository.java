package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByMobile(String ownerMobileNo);

}
