package com.dynamicdashboard.cockpit.analytics.application;

import com.dynamicdashboard.cockpit.analytics.application.dto.AnalyticsCountersDto;
import com.dynamicdashboard.cockpit.analytics.application.dto.AnalyticsDailyPointDto;
import com.dynamicdashboard.cockpit.analytics.application.dto.AnalyticsEventDto;
import com.dynamicdashboard.cockpit.analytics.application.dto.AnalyticsSummaryDto;
import com.dynamicdashboard.cockpit.analytics.application.dto.CreateAnalyticsEventRequestDto;
import com.dynamicdashboard.cockpit.analytics.application.mapper.AnalyticsMapper;
import com.dynamicdashboard.cockpit.analytics.domain.AnalyticsEventEntity;
import com.dynamicdashboard.cockpit.analytics.repository.AnalyticsEventRepository;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardRepository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AnalyticsAction;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.AnalyticsTarget;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsApplicationService {

    private final com.dynamicdashboard.cockpit.shared.cache.RedisCacheService redisCacheService;
    private final com.dynamicdashboard.cockpit.shared.sse.SseNotificationService sseNotificationService;


    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<AnalyticsAction> CLICK_ACTIONS = Set.of(
            AnalyticsAction.DASHBOARD_CLICK, AnalyticsAction.WIDGET_INTERACTION,
            AnalyticsAction.RAW_DATA_VIEW, AnalyticsAction.RAW_DATA_EXPORT);
    private static final Set<AnalyticsAction> IMPRESSION_ACTIONS = Set.of(
            AnalyticsAction.DASHBOARD_VIEW, AnalyticsAction.DASHBOARD_IMPRESSION,
            AnalyticsAction.WIDGET_IMPRESSION);

    private final AnalyticsEventRepository analyticsEventRepository;
    private final DashboardRepository dashboardRepository;
    private final CurrentUserService currentUserService;
    private final AnalyticsMapper analyticsMapper;

    @Transactional
    public AnalyticsEventDto recordEvent(CreateAnalyticsEventRequestDto dto) {
        UserAccountEntity actor = currentUserService.getCurrentUser();

        AnalyticsEventEntity event = new AnalyticsEventEntity();
        event.setActorUser(actor);
        event.setAction(parseEnum(AnalyticsAction.class, dto.getAction()));
        event.setTargetType(parseEnum(AnalyticsTarget.class, dto.getTarget()));
        event.setTargetId(dto.getTargetId());
        event.setTargetName(dto.getTargetName());
        event.setDashboardName(dto.getDashboardName());
        event.setOccurredAt(Instant.now());

        if (dto.getDashboardId() != null) {
            DashboardEntity dashboard = dashboardRepository.findById(dto.getDashboardId()).orElse(null);
            event.setDashboard(dashboard);
        }

        AnalyticsEventEntity saved = analyticsEventRepository.save(event);
        redisCacheService.evictByPrefix("analytics");
        sseNotificationService.broadcast("analytics_changed");
        return analyticsMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsEventDto> getRecentEvents(String actionFilter, int limit) {
        String cacheKey = "analytics:recent:" + actionFilter + "_" + limit;
        List<AnalyticsEventDto> cached = redisCacheService.get(cacheKey, new com.fasterxml.jackson.core.type.TypeReference<List<AnalyticsEventDto>>() {});
        if (cached != null) return cached;

        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "occurredAt"));
        List<AnalyticsEventEntity> events;
        if (actionFilter != null && !actionFilter.isBlank() && !"all".equalsIgnoreCase(actionFilter)) {
            AnalyticsAction action = parseEnum(AnalyticsAction.class, actionFilter);
            events = action != null
                    ? analyticsEventRepository.findByActionOrderByOccurredAtDesc(action, page)
                    : List.of();
        } else {
            events = analyticsEventRepository.findAllByOrderByOccurredAtDesc(page);
        }
        List<AnalyticsEventDto> result = events.stream().map(analyticsMapper::toDto).collect(Collectors.toList());
        redisCacheService.put(cacheKey, result);
        return result;
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getSummary(int days) {
        String cacheKey = "analytics:summary:" + days;
        AnalyticsSummaryDto cached = redisCacheService.get(cacheKey, new com.fasterxml.jackson.core.type.TypeReference<AnalyticsSummaryDto>() {});
        if (cached != null) return cached;

        int windowDays = days <= 0 ? 7 : days;

        LocalDate today = LocalDate.now(ZONE);
        Instant lookbackThreshold = today.minusDays(59).atStartOfDay(ZONE).toInstant();
        List<AnalyticsEventEntity> lookbackEvents = analyticsEventRepository
                .findByOccurredAtAfterOrderByOccurredAtDesc(lookbackThreshold);

        List<LocalDate> activeDaysDesc = lookbackEvents.stream()
                .map(event -> event.getOccurredAt().atZone(ZONE).toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .limit(windowDays)
                .collect(Collectors.toList());

        Set<LocalDate> activeDaySet = new HashSet<>(activeDaysDesc);
        List<AnalyticsEventEntity> events = lookbackEvents.stream()
                .filter(event -> activeDaySet.contains(event.getOccurredAt().atZone(ZONE).toLocalDate()))
                .collect(Collectors.toList());

        List<LocalDate> activeDaysAsc = new ArrayList<>(activeDaysDesc);
        Collections.reverse(activeDaysAsc);

        List<AnalyticsEventEntity> recentForFeed = analyticsEventRepository
                .findAllByOrderByOccurredAtDesc(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "occurredAt")));

        AnalyticsSummaryDto result = AnalyticsSummaryDto.builder()
                .counters(buildCounters(events))
                .daily(buildDailySeries(events, activeDaysAsc))
                .events(recentForFeed.stream().map(analyticsMapper::toDto).collect(Collectors.toList()))
                .build();
        redisCacheService.put(cacheKey, result);
        return result;
    }

    private AnalyticsCountersDto buildCounters(List<AnalyticsEventEntity> events) {
        Map<String, Long> dashboardViews = new HashMap<>();
        Map<String, Long> widgetRawDataViews = new HashMap<>();
        Map<String, Long> widgetInteractions = new HashMap<>();
        Map<String, Long> queryExecutions = new HashMap<>();
        long impressions = 0;
        long clicks = 0;

        for (AnalyticsEventEntity event : events) {
            AnalyticsAction action = event.getAction();
            String targetId = event.getTargetId() != null ? event.getTargetId().toString() : null;

            if (action == AnalyticsAction.DASHBOARD_VIEW && targetId != null) {
                dashboardViews.merge(targetId, 1L, Long::sum);
            }
            if (action == AnalyticsAction.RAW_DATA_VIEW && targetId != null) {
                widgetRawDataViews.merge(targetId, 1L, Long::sum);
            }
            if (action == AnalyticsAction.WIDGET_INTERACTION && targetId != null) {
                widgetInteractions.merge(targetId, 1L, Long::sum);
            }
            if (action == AnalyticsAction.QUERY_EXECUTION && targetId != null) {
                queryExecutions.merge(targetId, 1L, Long::sum);
            }
            if (IMPRESSION_ACTIONS.contains(action)) {
                impressions++;
            }
            if (CLICK_ACTIONS.contains(action)) {
                clicks++;
            }
        }

        return AnalyticsCountersDto.builder()
                .dashboardViews(dashboardViews)
                .widgetRawDataViews(widgetRawDataViews)
                .widgetInteractions(widgetInteractions)
                .queryExecutions(queryExecutions)
                .impressions(impressions)
                .clicks(clicks)
                .build();
    }

    private List<AnalyticsDailyPointDto> buildDailySeries(List<AnalyticsEventEntity> events, List<LocalDate> activeDaysAsc) {
        Map<LocalDate, Long> dashboardViewsByDay = new HashMap<>();
        Map<LocalDate, Long> rawDataViewsByDay = new HashMap<>();
        Map<LocalDate, Long> clicksByDay = new HashMap<>();
        Map<LocalDate, Set<UUID>> activeUsersByDay = new HashMap<>();

        for (AnalyticsEventEntity event : events) {
            LocalDate day = event.getOccurredAt().atZone(ZONE).toLocalDate();
            AnalyticsAction action = event.getAction();

            if (action == AnalyticsAction.DASHBOARD_VIEW) {
                dashboardViewsByDay.merge(day, 1L, Long::sum);
            }
            if (action == AnalyticsAction.RAW_DATA_VIEW) {
                rawDataViewsByDay.merge(day, 1L, Long::sum);
            }
            if (CLICK_ACTIONS.contains(action)) {
                clicksByDay.merge(day, 1L, Long::sum);
            }
            if (event.getActorUser() != null) {
                activeUsersByDay
                        .computeIfAbsent(day, key -> new HashSet<>())
                        .add(event.getActorUser().getId());
            }
        }

        List<AnalyticsDailyPointDto> series = new ArrayList<>();
        for (LocalDate day : activeDaysAsc) {
            series.add(AnalyticsDailyPointDto.builder()
                    .date(day.format(DAY_FORMAT))
                    .activeUsers(activeUsersByDay.getOrDefault(day, Set.of()).size())
                    .dashboardViews(dashboardViewsByDay.getOrDefault(day, 0L))
                    .rawDataViews(rawDataViewsByDay.getOrDefault(day, 0L))
                    .clicks(clicksByDay.getOrDefault(day, 0L))
                    .build());
        }
        return series;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}