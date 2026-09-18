package com.dynamicdashboard.cockpit.audit.application;

import com.dynamicdashboard.cockpit.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Premier test unitaire du projet (il n'y en avait aucun jusqu'ici, alors que la CI/CD
 * `staging.yml` exécute `mvn test -B` avant chaque déploiement — cette étape ne
 * vérifiait donc jusqu'ici rien du tout).
 *
 * Couvre la correction : "les exécutions de requêtes doivent pouvoir être purgées
 * périodiquement" (demande explicite de Moez).
 */
@ExtendWith(MockitoExtension.class)
class AuditApplicationServicePurgeTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditApplicationService auditApplicationService;

    @Test
    void purgeOlderThan_deletesEventsBeforeThresholdAndReturnsCount() {
        Instant threshold = Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS);
        when(auditEventRepository.countByOccurredAtBefore(threshold)).thenReturn(42L);

        long deleted = auditApplicationService.purgeOlderThan(threshold);

        assertThat(deleted).isEqualTo(42L);
        verify(auditEventRepository).countByOccurredAtBefore(threshold);
        verify(auditEventRepository).deleteByOccurredAtBefore(threshold);
    }

    @Test
    void purgeOlderThan_doesNothingDestructiveWhenNoOldEvents() {
        Instant threshold = Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS);
        when(auditEventRepository.countByOccurredAtBefore(threshold)).thenReturn(0L);

        long deleted = auditApplicationService.purgeOlderThan(threshold);

        assertThat(deleted).isZero();
        verify(auditEventRepository, times(1)).deleteByOccurredAtBefore(threshold);
    }
}