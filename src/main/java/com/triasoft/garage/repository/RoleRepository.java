package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByCodeIgnoreCase(String code);
}
