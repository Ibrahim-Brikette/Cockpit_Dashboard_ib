package com.dynamicdashboard.cockpit.datasource.controller;
import com.dynamicdashboard.cockpit.datasource.application.DbConnectionApplicationService;
import com.dynamicdashboard.cockpit.datasource.application.dto.DataSourceConnectionRequestDto;
import com.dynamicdashboard.cockpit.datasource.application.dto.DbConnectionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/db-connections")
@RequiredArgsConstructor
public class DbConnectionController {
    private final DbConnectionApplicationService dbConnectionApplicationService;
    @PostMapping("/test")
    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).TEST.code)"+
    "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).MANAGE_ALL.code)")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody DataSourceConnectionRequestDto request) {
        Map<String, Object> result = dbConnectionApplicationService.testConnection(request);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).CREATE.code)"+
    "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).MANAGE_ALL.code)")
    @PostMapping
    public ResponseEntity<String> createConnection(@RequestBody DataSourceConnectionRequestDto request) {
        String id = dbConnectionApplicationService.createConnection(request);
        return ResponseEntity.ok(id);
    }

    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).EDIT.code)"+
    "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).MANAGE_ALL.code)")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateConnection(@PathVariable UUID id, @RequestBody DataSourceConnectionRequestDto request) {
        try {
            dbConnectionApplicationService.updateConnection(id, request);
            return ResponseEntity.ok(Map.of("message", "Connexion mise à jour avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erreur lors de la mise à jour : " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).DELETE.code)"+
    "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).MANAGE_ALL.code)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConnection(@PathVariable UUID id) {
        try {
            dbConnectionApplicationService.deleteConnection(id);
            return ResponseEntity.ok(Map.of("message", "Connexion supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Erreur lors de la suppression : " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).VIEW.code)"+
    "or hasAuthority(T(com.dynamicdashboard.cockpit.shared.security.authorization.DbConnection).MANAGE_ALL.code)")
    @GetMapping
    public ResponseEntity<List<DbConnectionResponseDto>> getAllConnections() {
        return ResponseEntity.ok(dbConnectionApplicationService.getAllConnections());
    }
}
