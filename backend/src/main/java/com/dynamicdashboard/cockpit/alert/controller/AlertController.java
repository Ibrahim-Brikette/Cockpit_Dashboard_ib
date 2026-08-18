package com.dynamicdashboard.cockpit.alert.controller;

import com.dynamicdashboard.cockpit.alert.application.AlertApplicationService;
import com.dynamicdashboard.cockpit.alert.application.dto.AlertEventDto;
import com.dynamicdashboard.cockpit.alert.application.dto.SnoozeAlertRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertApplicationService alertApplicationService;

    @GetMapping
    public ResponseEntity<List<AlertEventDto>> getAllEvents() {
        return ResponseEntity.ok(alertApplicationService.getAllEvents());
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<AlertEventDto> acknowledgeEvent(@PathVariable UUID id) {
        return alertApplicationService.acknowledgeEvent(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/snooze")
    public ResponseEntity<AlertEventDto> snoozeEvent(@PathVariable UUID id, @RequestBody SnoozeAlertRequestDto dto) {
        return alertApplicationService.snoozeEvent(id, dto.getMinutes())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/evaluate")
    public ResponseEntity<List<AlertEventDto>> evaluate() {
        return ResponseEntity.ok(alertApplicationService.evaluateAlerts());
    }
}