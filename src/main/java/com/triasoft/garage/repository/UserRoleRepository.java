package com.triasoft.garage.repository;

import com.triasoft.garage.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    void deleteByRoleId(Long roleId);

    @Query("select r.code from UserRole ur join Role r on r.id = ur.roleId where ur.userId = :userId")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
}
