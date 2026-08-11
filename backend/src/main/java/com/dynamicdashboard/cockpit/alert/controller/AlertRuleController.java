package com.dynamicdashboard.cockpit.alert.controller;

import com.dynamicdashboard.cockpit.alert.application.AlertApplicationService;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertRuleRequestDto;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertRuleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertRuleController {

    private final AlertApplicationService alertApplicationService;

    @GetMapping
    public ResponseEntity<List<AlertRuleResponseDto>> getAllRules() {
        return ResponseEntity.ok(alertApplicationService.getAllRules());
    }

    @PostMapping
    public ResponseEntity<AlertRuleResponseDto> createRule(@RequestBody AlertRuleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertApplicationService.createRule(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertRuleResponseDto> updateRule(@PathVariable UUID id, @RequestBody AlertRuleRequestDto dto) {
        return alertApplicationService.updateRule(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        if (alertApplicationService.deleteRule(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<AlertRuleResponseDto> toggleRule(@PathVariable UUID id) {
        return alertApplicationService.toggleRule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}