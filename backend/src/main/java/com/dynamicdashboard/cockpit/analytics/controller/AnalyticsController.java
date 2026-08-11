package com.dynamicdashboard.cockpit.analytics.controller;

import com.dynamicdashboard.cockpit.analytics.application.AnalyticsApplicationService;
import com.dynamicdashboard.cockpit.analytics.application.dto.AnalyticsEventDto;
import com.dynamicdashboard.cockpit.analytics.application.dto.AnalyticsSummaryDto;
import com.dynamicdashboard.cockpit.analytics.application.dto.CreateAnalyticsEventRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsApplicationService analyticsApplicationService;

    @GetMapping({"/summary", "/overview"})
    public ResponseEntity<AnalyticsSummaryDto> getSummary(
            @RequestParam(name = "days", defaultValue = "7") int days) {
        return ResponseEntity.ok(analyticsApplicationService.getSummary(days));
    }

    @GetMapping("/events")
    public ResponseEntity<List<AnalyticsEventDto>> getEvents(
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(analyticsApplicationService.getRecentEvents(action, limit));
    }

    @PostMapping("/events")
    public ResponseEntity<AnalyticsEventDto> recordEvent(@RequestBody CreateAnalyticsEventRequestDto dto) {
        AnalyticsEventDto created = analyticsApplicationService.recordEvent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}