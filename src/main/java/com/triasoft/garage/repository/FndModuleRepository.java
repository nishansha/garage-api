package com.triasoft.garage.repository;

import com.triasoft.garage.entity.FndModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FndModuleRepository extends JpaRepository<FndModule, Long> {
    List<FndModule> findByActiveTrue();

    FndModule findByCodeIgnoreCase(String code);
}
