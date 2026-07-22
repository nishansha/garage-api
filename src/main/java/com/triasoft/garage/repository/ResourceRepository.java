package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByModuleIdAndActiveTrue(Long moduleId);

    Resource findByCodeIgnoreCase(String code);
}
