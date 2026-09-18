package com.dynamicdashboard.cockpit.audit.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente le nombre d'exécutions d'une requête (ou de toute autre cible auditée),
 * calculé par une agrégation SQL (GROUP BY) au niveau base de données plutôt que
 * par une boucle Java sur tous les événements bruts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTargetCountDto {
    private UUID targetId;
    private String label;
    private Long total;
}