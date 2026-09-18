package com.dynamicdashboard.cockpit.audit.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purge périodique de la table audit_event.
 *
 * Répond directement à la demande de Moez : "mets-les [les exécutions de requêtes]
 * dans une table d'audit, afin qu'ils puissent faire un purge tous les 3 mois."
 *
 * Tourne chaque jour à 3h du matin (heure serveur) et supprime tout événement d'audit
 * plus vieux que `cockpit.audit.retention-days` (90 jours = 3 mois par défaut).
 * Configurable via la variable d'environnement COCKPIT_AUDIT_RETENTION_DAYS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditPurgeScheduler {

    private final AuditApplicationService auditApplicationService;

    @Value("${cockpit.audit.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${cockpit.audit.purge-cron:0 0 3 * * *}")
    public void purgeOldAuditEvents() {
        Instant threshold = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        long deletedCount = auditApplicationService.purgeOlderThan(threshold);
        if (deletedCount > 0) {
            log.info("Purge audit_event : {} événements supprimés (antérieurs à {} jours, seuil={})",
                    deletedCount, retentionDays, threshold);
        }
    }
}