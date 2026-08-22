package com.triasoft.garage.company.repository;

import com.triasoft.garage.company.entity.UserCompanyAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCompanyAccessRepository extends JpaRepository<UserCompanyAccess, Long> {
    List<UserCompanyAccess> findByUserId(Long userId);

    List<UserCompanyAccess> findByCompanyId(Long companyId);

    Optional<UserCompanyAccess> findByUserIdAndCompanyId(Long userId, Long companyId);

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    boolean existsByCompanyId(Long companyId);
}
