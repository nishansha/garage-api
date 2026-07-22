package com.triasoft.garage.repository;

import com.triasoft.garage.entity.RolePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePrivilegeRepository extends JpaRepository<RolePrivilege, Long> {
    List<RolePrivilege> findByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);
}
