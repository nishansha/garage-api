package com.triasoft.garage.scheduler;

import com.triasoft.garage.repository.UserRefreshTokenRepository;
import com.triasoft.garage.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupScheduler {


    private static final int REFRESH_TOKEN_RETENTION_DAYS = 7;
    private static final int SESSION_RETENTION_DAYS = 7;

    private final UserSessionRepository userSessionRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;

    // Every Sunday at 02:00 server time.
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void purgeEndedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(SESSION_RETENTION_DAYS);
        int deleted = userSessionRepository.deleteEndedSessionsBefore(cutoff);
        log.info("SessionCleanupScheduler - purged {} ended user_session row(s) older than {}", deleted, cutoff);
    }

    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(REFRESH_TOKEN_RETENTION_DAYS);
        int deleted = userRefreshTokenRepository.deleteAllExpiredBefore(cutoff);
        log.info("SessionCleanupScheduler - purged {} expired user_refresh_token row(s) older than {}", deleted, cutoff);
    }
}
