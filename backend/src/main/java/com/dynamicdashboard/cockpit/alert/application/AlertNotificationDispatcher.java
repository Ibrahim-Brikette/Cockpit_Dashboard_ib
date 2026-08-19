package com.dynamicdashboard.cockpit.alert.application;

import com.dynamicdashboard.cockpit.alert.domain.AlertEventEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AlertChannel;

/**
 * AVANT : evaluateAlerts() créait une ligne dans alert_event_channel pour dire
 * "cette alerte doit partir sur le canal EMAIL/SMS/WHATSAPP", mais AUCUN code
 * n'envoyait jamais rien. Le seul effet observable était un broadcast SSE qui
 * rafraîchissait la liste dans l'UI -> "la notification ne se déclenche pas",
 * exactement le symptôme signalé par Moez.
 *
 * Ceci définit le vrai point de branchement pour l'envoi. L'implémentation par
 * défaut (LoggingAlertNotificationDispatcher) journalise seulement — elle sert de
 * filet de sécurité tant qu'aucun fournisseur réel (SMTP, Twilio, WhatsApp Business
 * API...) n'est configuré. Remplacez-la par une vraie implémentation (une autre
 * classe @Service qui implémente cette interface) dès que les identifiants du
 * fournisseur sont disponibles ; Spring l'injectera automatiquement à la place.
 */
public interface AlertNotificationDispatcher {
    void dispatch(AlertEventEntity event, AlertChannel channel);
}