package com.dynamicdashboard.cockpit.query.controller;
import com.dynamicdashboard.cockpit.analytics.application.AnalyticsApplicationService;
import com.dynamicdashboard.cockpit.analytics.application.dto.CreateAnalyticsEventRequestDto;
import com.dynamicdashboard.cockpit.query.application.QueryApplicationService;
import com.dynamicdashboard.cockpit.query.application.dto.QueryRequestDto;
import com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/queries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QueryController {
    private final QueryApplicationService queryApplicationService;
    private final AnalyticsApplicationService analyticsApplicationService;
    @GetMapping
    public ResponseEntity<List<QueryResponseDto>> getAllQueries() {
        return ResponseEntity.ok(queryApplicationService.getAllQueries());
    }
    @GetMapping("/{id}")
    public ResponseEntity<QueryResponseDto> getQueryById(@PathVariable UUID id) {
        return queryApplicationService.getQueryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping
    public ResponseEntity<QueryResponseDto> createQuery(@RequestBody QueryRequestDto dto) {
        QueryResponseDto created = queryApplicationService.createQuery(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    @PutMapping("/{id}")
    public ResponseEntity<QueryResponseDto> updateQuery(@PathVariable UUID id, @RequestBody QueryRequestDto dto) {
        return queryApplicationService.updateQuery(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuery(@PathVariable UUID id) {
        if (queryApplicationService.deleteQuery(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<QueryResponseDto> duplicateQuery(@PathVariable UUID id) {
        return queryApplicationService.duplicateQuery(id)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res))
                .orElse(ResponseEntity.notFound().build());
    }
    @RequestMapping(value = "/{id}/execute", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<List<java.util.Map<String, Object>>> executeQueryData(@PathVariable UUID id, @RequestBody(required = false) List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto> filters) {
        ResponseEntity<List<java.util.Map<String, Object>>> response = ResponseEntity.ok(queryApplicationService.executeQueryData(id, filters));
        try {
            String targetName = queryApplicationService.getQueryById(id)
                    .map(com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto::getName)
                    .orElse("Requête inconnue");
            analyticsApplicationService.recordEvent(CreateAnalyticsEventRequestDto.builder()
                .action("QUERY_EXECUTION")
                .target("QUERY")
                .targetId(id)
                .targetName(targetName)
                .build());
        } catch (Exception ignored) {}
        return response;
    }
    @PostMapping("/batch-execute")
    public ResponseEntity<java.util.Map<String, List<java.util.Map<String, Object>>>> executeBatchQueriesInParallel(@RequestBody java.util.Map<String, List<com.dynamicdashboard.cockpit.query.application.dto.RuntimeQueryFilterDto>> queryFilterMap) {
        ResponseEntity<java.util.Map<String, List<java.util.Map<String, Object>>>> response = ResponseEntity.ok(queryApplicationService.executeBatchQueriesInParallel(queryFilterMap));
        try {
            if (queryFilterMap != null) {
                for (String qid : queryFilterMap.keySet()) {
                    UUID qUuid = UUID.fromString(qid);
                    String targetName = queryApplicationService.getQueryById(qUuid)
                            .map(com.dynamicdashboard.cockpit.query.application.dto.QueryResponseDto::getName)
                            .orElse("Requête inconnue");
                    analyticsApplicationService.recordEvent(CreateAnalyticsEventRequestDto.builder()
                        .action("QUERY_EXECUTION")
                        .target("QUERY")
                        .targetId(qUuid)
                        .targetName(targetName)
                        .build());
                }
            }
        } catch (Exception ignored) {}
        return response;
    }
    @PostMapping("/preview")
    public ResponseEntity<List<java.util.Map<String, Object>>> previewDraftQuery(@RequestBody QueryRequestDto dto) {
        return ResponseEntity.ok(queryApplicationService.previewDraftQuery(dto));
    }
}
