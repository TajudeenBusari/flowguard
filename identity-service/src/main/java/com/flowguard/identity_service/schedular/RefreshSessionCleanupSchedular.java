package com.flowguard.identity_service.schedular;

import com.flowguard.identity_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshSessionCleanupSchedular {
  private final RefreshTokenService refreshTokenService;

  //This runs once per day at 03:00 and deletes refresh sessions whose expires_at is already in the past.
  @Scheduled(cron = "${security.refresh-session.cleanup-cron}") // Every day at 3 AM
  public void cleanupExpiredRefreshSessions() {
    Instant cutoff = Instant.now();
    refreshTokenService
            .cleanupExpiredRefreshSessions(cutoff)
            .subscribe(
                    deletedCount -> log.info("Refresh session cleanup completed: {} expired sessions deleted", deletedCount),
                    error -> log.error("Refresh session cleanup failed", error)
            );
  }
}
