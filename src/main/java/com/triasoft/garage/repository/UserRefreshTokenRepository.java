package com.triasoft.garage.repository;

import com.triasoft.garage.entity.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    Optional<UserRefreshToken> findByTokenHash(String tokenHash);

    List<UserRefreshToken> findByUserIdAndRevokedFalse(Long userId);

    @Modifying
    @Query("DELETE FROM UserRefreshToken t WHERE t.expiresAt < :cutoff " +
            "AND t.id NOT IN (SELECT r.parentId FROM UserRefreshToken r WHERE r.parentId IS NOT NULL)")
    int deleteAllExpiredBefore(@Param("cutoff") LocalDateTime cutoff);

}
