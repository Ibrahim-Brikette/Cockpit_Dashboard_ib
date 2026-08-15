package com.dynamicdashboard.cockpit.query.controller;

import com.dynamicdashboard.cockpit.query.application.QueryApplicationService;
import com.dynamicdashboard.cockpit.query.application.dto.QueryRequestDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto;
import com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanBatchExecuteQueries;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanCreateQuery;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanDeleteQuery;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanDuplicateQuery;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanEditQuery;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanExecuteQuery;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanListQueries;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanPreviewQuery;
import com.dynamicdashboard.cockpit.shared.security.annotation.query.CanViewQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@Slf4j
public class QueryController {

    private final QueryApplicationService queryApplicationService;

    @GetMapping
    public ResponseEntity<List<QueryResponseDto>> getAllQueries() {
        return ResponseEntity.ok(queryApplicationService.getAllQueries());
    }

    @GetMapping("/tenant")
    @CanListQueries
    public ResponseEntity<List<QueryResponseDto>> getAllByTenantIdQueries() {
        return ResponseEntity.ok(queryApplicationService.getAllByTenantIdQueries());
    }

    @GetMapping("/{id}")
    @CanViewQuery
    public ResponseEntity<QueryResponseDto> getQueryById(@PathVariable UUID id) {
        return queryApplicationService.getQueryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @CanCreateQuery
    public ResponseEntity<QueryResponseDto> createQuery(@RequestBody QueryRequestDto dto) {
        QueryResponseDto created = queryApplicationService.createQuery(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @CanEditQuery
    public ResponseEntity<QueryResponseDto> updateQuery(@PathVariable UUID id, @RequestBody QueryRequestDto dto) {
        return queryApplicationService.updateQuery(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @CanDeleteQuery
    public ResponseEntity<Void> deleteQuery(@PathVariable UUID id) {
        if (queryApplicationService.deleteQuery(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/duplicate")
    @CanDuplicateQuery
    public ResponseEntity<QueryResponseDto> duplicateQuery(@PathVariable UUID id) {
        return queryApplicationService.duplicateQuery(id)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res))
                .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/{id}/execute", method = {RequestMethod.GET, RequestMethod.POST})
    @CanExecuteQuery
    public ResponseEntity<List<Map<String, Object>>> executeQueryData(
            @PathVariable UUID id,
            @RequestBody(required = false) List<RuntimeQueryFilterDto> filters) {
        return ResponseEntity.ok(queryApplicationService.executeQueryData(id, filters));
    }

    @PostMapping("/batch-execute")
    @CanBatchExecuteQueries
    public ResponseEntity<Map<String, List<Map<String, Object>>>> executeBatchQueriesInParallel(
            @RequestBody Map<String, List<RuntimeQueryFilterDto>> queryFilterMap) {
        return ResponseEntity.ok(queryApplicationService.executeBatchQueriesInParallel(queryFilterMap));
    }

    @PostMapping("/preview")
    @CanPreviewQuery
    public ResponseEntity<List<Map<String, Object>>> previewDraftQuery(@RequestBody QueryRequestDto dto) {
        return ResponseEntity.ok(queryApplicationService.previewDraftQuery(dto));
    }
}
