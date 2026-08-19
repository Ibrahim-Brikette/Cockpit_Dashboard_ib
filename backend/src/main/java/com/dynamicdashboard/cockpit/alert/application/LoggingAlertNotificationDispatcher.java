package com.dynamicdashboard.cockpit.alert.application;

import com.dynamicdashboard.cockpit.alert.domain.AlertEventEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implémentation par défaut : journalise l'envoi au lieu de le simuler silencieusement.
 * Permet de vérifier en test E2E que le déclenchement d'une alerte tente bien un envoi
 * par canal, sans dépendre d'identifiants SMTP/SMS/WhatsApp réels non configurés ici.
 */
@Service
@Slf4j
public class LoggingAlertNotificationDispatcher implements AlertNotificationDispatcher {

    @Override
    public void dispatch(AlertEventEntity event, AlertChannel channel) {
        log.warn("[ALERTE-{}] Aucun fournisseur réel configuré pour le canal {} -> "
                        + "règle='{}', message='{}'. Implémentez AlertNotificationDispatcher "
                        + "avec un vrai fournisseur (SMTP/Twilio/WhatsApp Business) pour un envoi effectif.",
                channel, channel, event.getRuleName(), event.getMessage());
    }
}