package com.triasoft.garage.repository;

import com.triasoft.garage.entity.ChatOfAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatOfAccountRepository extends JpaRepository<ChatOfAccount, Long> {
    List<ChatOfAccount> findByType(String type);
}
