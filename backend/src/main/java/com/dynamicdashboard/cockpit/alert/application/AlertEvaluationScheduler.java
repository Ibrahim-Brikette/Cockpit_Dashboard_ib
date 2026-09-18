package com.dynamicdashboard.cockpit.alert.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AVANT : evaluateAlerts() n'était déclenché QUE par un clic manuel sur le bouton
 * "Évaluer maintenant" côté frontend (voir alerts.component.ts -> evaluateNow()).
 * Résultat : une alerte basée sur de vraies données ERP ne se déclenchait jamais
 * toute seule, même si le seuil était dépassé depuis longtemps -> vérification statique,
 * exactement ce que Moez a signalé.
 *
 * APRÈS : ce job tourne automatiquement toutes les 5 minutes (configurable) et
 * réévalue toutes les règles actives contre les données réelles des sources connectées.
 * Le bouton "Évaluer maintenant" reste disponible pour forcer une vérification immédiate.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEvaluationScheduler {

    private final AlertApplicationService alertApplicationService;

    @Scheduled(fixedRateString = "${cockpit.alerts.evaluation-interval-ms:300000}")
    public void evaluateAlertsPeriodically() {
        log.info("[DIAGNOSTIC] AlertEvaluationScheduler démarre une évaluation à {}", java.time.Instant.now());
        try {
            var triggered = alertApplicationService.evaluateAlerts();
            if (!triggered.isEmpty()) {
                log.info("Évaluation automatique des alertes : {} alerte(s) déclenchée(s)/mise(s) à jour", triggered.size());
            } else {
                log.info("[DIAGNOSTIC] Évaluation terminée, aucune alerte déclenchée (voir logs AlertApplicationService pour le détail par règle).");
            }
        } catch (Exception e) {
            log.warn("Échec de l'évaluation automatique des alertes : {}", e.getMessage());
        }
    }
}