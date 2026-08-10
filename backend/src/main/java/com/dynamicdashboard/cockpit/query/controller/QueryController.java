package com.dynamicdashboard.cockpit.query.controller;
import com.dynamicdashboard.cockpit.query.application.QueryApplicationService;
import com.dynamicdashboard.cockpit.query.application.dto.QueryRequestDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto;
import com.dynamicdashboard.cockpit.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/queries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j

public class QueryController {
    private final QueryApplicationService queryApplicationService;


    @GetMapping
    @PreAuthorize("hasRole('DASHBOARD_VIEWER')")
    public ResponseEntity<List<QueryResponseDto>> getAllQueries() {
        return ResponseEntity.ok(queryApplicationService.getAllQueries());
    }
    @GetMapping("/tenant")
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code)")
    public ResponseEntity<List<QueryResponseDto>> getAllByTenantIdQueries() {
        return ResponseEntity.ok(queryApplicationService.getAllByTenantIdQueries());
    }

    @PreAuthorize("hasPermission(#id,'Query',T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code)")
    @GetMapping("/{id}")
    public ResponseEntity<QueryResponseDto> getQueryById(@PathVariable UUID id) {
        return queryApplicationService.getQueryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).CREATE.code)")
    public ResponseEntity<QueryResponseDto> createQuery(@RequestBody QueryRequestDto dto) {
        QueryResponseDto created = queryApplicationService.createQuery(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id,'Query',T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).EDIT.code)")
    public ResponseEntity<QueryResponseDto> updateQuery(@PathVariable UUID id, @RequestBody QueryRequestDto dto) {
        return queryApplicationService.updateQuery(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id,'Query',T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).DELETE.code)")
    public ResponseEntity<Void> deleteQuery(@PathVariable UUID id) {
        if (queryApplicationService.deleteQuery(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasPermission(#id,'Query',T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code)"
                + "and hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).CREATE.code)"
                )
    public ResponseEntity<QueryResponseDto> duplicateQuery(@PathVariable UUID id) {
        return queryApplicationService.duplicateQuery(id)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res))
                .orElse(ResponseEntity.notFound().build());
    }



    @RequestMapping(value = "/{id}/execute", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    @PreAuthorize("hasPermission(#id,'Query',T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).EXECUTE.code)")
    public ResponseEntity<List<java.util.Map<String, Object>>> executeQueryData(@PathVariable UUID id, @RequestBody(required = false) List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto> filters) {
        return ResponseEntity.ok(queryApplicationService.executeQueryData(id, filters));
    }


    @PostMapping("/batch-execute")
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).EXECUTE.code)")
    public ResponseEntity<java.util.Map<String, List<java.util.Map<String, Object>>>> executeBatchQueriesInParallel(@RequestBody java.util.Map<String, List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto>> queryFilterMap) {
        return ResponseEntity.ok(queryApplicationService.executeBatchQueriesInParallel(queryFilterMap));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.QueryPermission).VIEW.code)")
    public ResponseEntity<List<java.util.Map<String, Object>>> previewDraftQuery(@RequestBody QueryRequestDto dto) {
        return ResponseEntity.ok(queryApplicationService.previewDraftQuery(dto));
    }
}
