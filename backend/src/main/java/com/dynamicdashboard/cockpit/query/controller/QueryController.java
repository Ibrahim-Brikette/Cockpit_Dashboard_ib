package com.dynamicdashboard.cockpit.query.controller;

import com.dynamicdashboard.cockpit.audit.application.AuditApplicationService;
import com.dynamicdashboard.cockpit.query.application.QueryApplicationService;
import com.dynamicdashboard.cockpit.query.application.dto.QueryRequestDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto;
import com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/queries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryApplicationService queryApplicationService;
    private final AuditApplicationService auditApplicationService;
    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SYSTEM_ADMIN') or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code) or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).MANAGE_ALL.code)")
    public ResponseEntity<List<QueryResponseDto>> getAllQueries() {
        return ResponseEntity.ok(queryApplicationService.getAllQueries());
    }



    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code)")
    public ResponseEntity<QueryResponseDto> getQueryById(@PathVariable UUID id) {
        return queryApplicationService.getQueryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).CREATE.code) or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).MANAGE_ALL.code)")
    public ResponseEntity<QueryResponseDto> createQuery(@RequestBody QueryRequestDto dto) {
        QueryResponseDto created = queryApplicationService.createQuery(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).EDIT.code)")
    public ResponseEntity<QueryResponseDto> updateQuery(@PathVariable UUID id, @RequestBody QueryRequestDto dto) {
        return queryApplicationService.updateQuery(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).DELETE.code)")
    public ResponseEntity<Void> deleteQuery(@PathVariable UUID id) {
        if (queryApplicationService.deleteQuery(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code) and (hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).CREATE.code) or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).MANAGE_ALL.code))")
    public ResponseEntity<QueryResponseDto> duplicateQuery(@PathVariable UUID id) {
        return queryApplicationService.duplicateQuery(id)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res))
                .orElse(ResponseEntity.notFound().build());
    }

     @RequestMapping(value = "/{id}/execute", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    @PreAuthorize("hasPermission(#id, 'Query', T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).EXECUTE.code)")
    public ResponseEntity<List<java.util.Map<String, Object>>> executeQueryData(@PathVariable UUID id, @RequestBody(required = false) List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto> filters) {
        ResponseEntity<List<java.util.Map<String, Object>>> response = ResponseEntity.ok(queryApplicationService.executeQueryData(id, filters));
        try {
            // IMPORTANT : chaque rendu de widget déclenche cet endpoint (GET), donc le volume est élevé.
            // On journalise dans la table d'AUDIT (purgée périodiquement), PAS dans analytics_event
            // (qui doit rester réservée aux comportements utilisateur réels : vues de dashboard, clics, etc.)
            String targetName = queryApplicationService.getQueryById(id)
                    .map(com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto::getName)
                    .orElse("Requête inconnue");
            auditApplicationService.logEvent(AuditApplicationService.EVENT_QUERY_EXECUTION, "QUERY", id, targetName, null);
        } catch (Exception ignored) {}
        return response;
    }

    @PostMapping("/batch-execute")
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).EXECUTE.code) or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).MANAGE_ALL.code)")
    public ResponseEntity<java.util.Map<String, List<java.util.Map<String, Object>>>> executeBatchQueriesInParallel(@RequestBody java.util.Map<String, List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto>> queryFilterMap) {
        ResponseEntity<java.util.Map<String, List<java.util.Map<String, Object>>>> response = ResponseEntity.ok(queryApplicationService.executeBatchQueriesInParallel(queryFilterMap));
        try {
            // Batch execute is used for preloading, do not artificially inflate analytics here.
        } catch (Exception ignored) {}
        return response;
    }


    @PostMapping("/preview")
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code) or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).MANAGE_ALL.code)")
    public ResponseEntity<List<Map<String, Object>>> previewDraftQuery(@RequestBody QueryRequestDto dto) {
        return ResponseEntity.ok(queryApplicationService.previewDraftQuery(dto));
    }
}