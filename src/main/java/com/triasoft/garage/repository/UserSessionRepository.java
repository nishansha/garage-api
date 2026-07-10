package com.triasoft.garage.repository;

import com.triasoft.garage.constants.SessionStatusEnum;
import com.triasoft.garage.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionId(String sessionId);

    List<UserSession> findByUserIdAndStatus(Long userId, SessionStatusEnum status);

    boolean existsBySessionIdAndStatus(String sessionId, SessionStatusEnum status);

}
