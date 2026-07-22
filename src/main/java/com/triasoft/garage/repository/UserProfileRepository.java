package com.triasoft.garage.repository;

import com.triasoft.garage.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    UserProfile findByUsername(String username);

    @Query("select distinct up from UserProfile up join UserRole ur on ur.userId = up.id join Role r on r.id = ur.roleId where r.code = :code")
    List<UserProfile> findByRoleCode(@Param("code") String code);

}
