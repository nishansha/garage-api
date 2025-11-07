package com.triasoft.garage.repository;

import com.triasoft.garage.entity.LookupMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookupMasterRepository extends JpaRepository<LookupMaster, Long> {
    List<LookupMaster> findByTypeAndEnabledTrue(String type);
}
