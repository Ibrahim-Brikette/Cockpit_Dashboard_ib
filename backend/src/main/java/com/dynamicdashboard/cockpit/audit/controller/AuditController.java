package com.dynamicdashboard.cockpit.audit.controller;
import com.dynamicdashboard.cockpit.audit.application.AuditApplicationService;
import com.dynamicdashboard.cockpit.audit.application.dto.AuditEventDto;
import com.dynamicdashboard.cockpit.audit.application.dto.CreateAuditEventRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
@RequestMapping("/api/audit-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {
    private final AuditApplicationService auditApplicationService;
    @GetMapping
    public ResponseEntity<List<AuditEventDto>> getRecentEvents() {
        return ResponseEntity.ok(auditApplicationService.getRecentEvents());
    }
    @PostMapping
    public ResponseEntity<AuditEventDto> logAuditEvent(@RequestBody CreateAuditEventRequestDto dto) {
        AuditEventDto created = auditApplicationService.logAuditEvent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
