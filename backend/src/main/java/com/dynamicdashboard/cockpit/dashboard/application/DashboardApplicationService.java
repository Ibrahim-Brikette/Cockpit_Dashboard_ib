package com.dynamicdashboard.cockpit.dashboard.application;
import com.dynamicdashboard.cockpit.catalog.repository.DataFieldRepository;
import com.dynamicdashboard.cockpit.dashboard.application.dto.DashboardRequestDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.DashboardResponseDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.GlobalFilterDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.WidgetDto;
import com.dynamicdashboard.cockpit.dashboard.application.dto.WidgetFilterDto;
import com.dynamicdashboard.cockpit.dashboard.application.mapper.DashboardMapper;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardTagEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.DashboardTagEntity.DashboardTagId;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterOptionEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterValueMapEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetDatagridConfigEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetDatagridVisibleColumnEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.WidgetFilterEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.DashboardTagRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.GlobalFilterOptionRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.GlobalFilterRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.GlobalFilterValueMapRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetDatagridConfigRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetDatagridVisibleColumnRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetFilterRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.WidgetRepository;
import com.dynamicdashboard.cockpit.identity.domain.UserAccountEntity;
import com.dynamicdashboard.cockpit.query.repository.DataQueryRepository;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.DashboardDensity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.DashboardStatus;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.DataGridDensity;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.FilterOperator;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.GlobalFilterInput;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.KpiFormat;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.RefreshInterval;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.ShareLevel;
import com.dynamicdashboard.cockpit.shared.domain.DomainEnums.WidgetType;
import com.dynamicdashboard.cockpit.shared.security.CurrentUserService;
import com.dynamicdashboard.cockpit.shared.utils.ParsingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class DashboardApplicationService {
    private final DashboardRepository dashboardRepository;
    private final WidgetRepository widgetRepository;
    private final GlobalFilterRepository globalFilterRepository;
    private final DashboardTagRepository dashboardTagRepository;
    private final WidgetDatagridConfigRepository widgetDatagridConfigRepository;
    private final WidgetDatagridVisibleColumnRepository widgetDatagridVisibleColumnRepository;
    private final WidgetFilterRepository widgetFilterRepository;
    private final GlobalFilterOptionRepository globalFilterOptionRepository;
    private final GlobalFilterValueMapRepository globalFilterValueMapRepository;
    private final DataQueryRepository dataQueryRepository;
    private final DataFieldRepository dataFieldRepository;
    private final CurrentUserService currentUserService;
    private final DashboardMapper dashboardMapper;
    private final com.dynamicdashboard.cockpit.audit.application.AuditApplicationService auditApplicationService;
    @Transactional(readOnly = true)
    public List<DashboardResponseDto> getAllDashboards() {
        return dashboardRepository.findAll().stream()
                .map(dashboardMapper::toDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Optional<DashboardResponseDto> getDashboardById(UUID id) {
        return dashboardRepository.findById(id).map(dashboardMapper::toDto);
    }
    @Transactional
    public DashboardResponseDto createDashboard(DashboardRequestDto dto) {
        UserAccountEntity owner = currentUserService.getCurrentUser();
        DashboardEntity dashboard = new DashboardEntity();
        dashboard.setOwner(owner);
        applyDtoToDashboardEntity(dto, dashboard);
        DashboardEntity savedDashboard = dashboardRepository.save(dashboard);
        saveChildEntities(savedDashboard, dto);
        auditApplicationService.logEvent("Création de tableau de bord", "DASHBOARD", savedDashboard.getId(), savedDashboard.getDashboardName(), null);
        return dashboardMapper.toDto(savedDashboard);
    }
    @Transactional
    public Optional<DashboardResponseDto> updateDashboard(UUID id, DashboardRequestDto dto) {
        return dashboardRepository.findById(id).map(dashboard -> {
            deleteChildEntities(dashboard.getId());
            applyDtoToDashboardEntity(dto, dashboard);
            dashboard.setUpdatedAt(java.time.Instant.now());
            DashboardEntity updatedDashboard = dashboardRepository.saveAndFlush(dashboard);
            saveChildEntities(updatedDashboard, dto);
            auditApplicationService.logEvent("Modification de tableau de bord", "DASHBOARD", updatedDashboard.getId(), updatedDashboard.getDashboardName(), null);
            return dashboardMapper.toDto(updatedDashboard);
        });
    }
    @Transactional
    public boolean deleteDashboard(UUID id) {
        return dashboardRepository.findById(id).map(dashboard -> {
            auditApplicationService.logEvent("Suppression de tableau de bord", "DASHBOARD", dashboard.getId(), dashboard.getDashboardName(), null);
            dashboardRepository.deleteById(id);
            return true;
        }).orElse(false);
    }
    @Transactional
    public Optional<DashboardResponseDto> duplicateDashboard(UUID id) {
        return dashboardRepository.findById(id).map(source -> {
            DashboardResponseDto sourceDto = dashboardMapper.toDto(source);
            DashboardRequestDto copyDto = DashboardRequestDto.builder()
                    .name(sourceDto.getName() + " (copie)")
                    .description(sourceDto.getDescription())
                    .color(sourceDto.getColor())
                    .status("DRAFT")
                    .owner(sourceDto.getOwner())
                    .shareLevel("PRIVATE")
                    .columns(sourceDto.getColumns())
                    .density(sourceDto.getDensity())
                    .refreshInterval(sourceDto.getRefreshInterval())
                    .widgets(sourceDto.getWidgets() != null ? new ArrayList<>(sourceDto.getWidgets()) : new ArrayList<>())
                    .globalFilters(sourceDto.getGlobalFilters() != null ? new ArrayList<>(sourceDto.getGlobalFilters()) : new ArrayList<>())
                    .tags(sourceDto.getTags() != null ? new ArrayList<>(sourceDto.getTags()) : new ArrayList<>())
                    .favorite(false)
                    .archived(false)
                    .build();
            if (copyDto.getWidgets() != null) {
                copyDto.getWidgets().forEach(w -> {
                    w.setId(null);
                    if (w.getFilters() != null) {
                        w.getFilters().forEach(f -> f.setId(null));
                    }
                });
            }
            if (copyDto.getGlobalFilters() != null) {
                copyDto.getGlobalFilters().forEach(f -> f.setId(null));
            }
            DashboardResponseDto created = createDashboard(copyDto);
            auditApplicationService.logEvent("Duplication de tableau de bord", "DASHBOARD", created.getId(), created.getName(), null);
            return created;
        });
    }
    @Transactional
    public Optional<DashboardResponseDto> toggleFavorite(UUID id) {
        return dashboardRepository.findById(id).map(dashboard -> {
            dashboard.setFavorite(!dashboard.isFavorite());
            DashboardEntity saved = dashboardRepository.save(dashboard);
            auditApplicationService.logEvent(saved.isFavorite() ? "Mise en favori" : "Retrait des favoris", "DASHBOARD", saved.getId(), saved.getDashboardName(), null);
            return dashboardMapper.toDto(saved);
        });
    }
    @Transactional
    public Optional<DashboardResponseDto> toggleArchive(UUID id) {
        return dashboardRepository.findById(id).map(dashboard -> {
            dashboard.setArchived(!dashboard.isArchived());
            DashboardEntity saved = dashboardRepository.save(dashboard);
            auditApplicationService.logEvent(saved.isArchived() ? "Archivage de tableau de bord" : "Désarchivage de tableau de bord", "DASHBOARD", saved.getId(), saved.getDashboardName(), null);
            return dashboardMapper.toDto(saved);
        });
    }
    private void applyDtoToDashboardEntity(DashboardRequestDto dto, DashboardEntity dashboard) {
        dashboard.setDashboardName(dto.getName() != null ? dto.getName() : "Nouveau tableau de bord");
        dashboard.setDashboardDescription(dto.getDescription());
        dashboard.setColorHex(dto.getColor() != null ? dto.getColor() : "#2563eb");
        dashboard.setStatus(ParsingUtils.parseEnum(DashboardStatus.class, dto.getStatus(), DashboardStatus.DRAFT));
        dashboard.setShareLevel(ParsingUtils.parseEnum(ShareLevel.class, dto.getShareLevel(), ShareLevel.PRIVATE));
        dashboard.setColumnsCount(dto.getColumns() != null ? dto.getColumns() : 12);
        dashboard.setDensity(ParsingUtils.parseEnum(DashboardDensity.class, dto.getDensity(), DashboardDensity.COMPACT));
        dashboard.setRefreshInterval(ParsingUtils.parseEnum(RefreshInterval.class, dto.getRefreshInterval(), RefreshInterval.OFF));
        dashboard.setFavorite(Boolean.TRUE.equals(dto.getFavorite()));
        dashboard.setArchived(Boolean.TRUE.equals(dto.getArchived()));
    }
    private void saveChildEntities(DashboardEntity dashboard, DashboardRequestDto dto) {
        if (dto.getTags() != null) {
            for (String tagValue : dto.getTags()) {
                DashboardTagEntity tag = new DashboardTagEntity();
                DashboardTagId tagId = new DashboardTagId();
                tagId.setDashboardId(dashboard.getId());
                tagId.setTagValue(tagValue);
                tag.setId(tagId);
                tag.setDashboard(dashboard);
                dashboardTagRepository.save(tag);
            }
        }
        if (dto.getGlobalFilters() != null) {
            for (GlobalFilterDto gfDto : dto.getGlobalFilters()) {
                GlobalFilterEntity gf = new GlobalFilterEntity();
                gf.setDashboard(dashboard);
                gf.setFilterName(gfDto.getName() != null ? gfDto.getName() : "filter");
                gf.setFilterLabel(gfDto.getLabel() != null ? gfDto.getLabel() : "Filtre");
                gf.setInputType(ParsingUtils.parseEnum(GlobalFilterInput.class, gfDto.getInput(), GlobalFilterInput.SELECT));
                if (gfDto.getFieldId() != null) {
                    dataFieldRepository.findById(gfDto.getFieldId()).ifPresent(gf::setTargetField);
                }
                gf.setDefaultValue(gfDto.getDefaultValue() != null ? gfDto.getDefaultValue() : "");
                gf.setReaderVisible(gfDto.isReaderVisible());
                GlobalFilterEntity savedGf = globalFilterRepository.save(gf);
                if (gfDto.getOptions() != null) {
                    for (int i = 0; i < gfDto.getOptions().size(); i++) {
                        GlobalFilterOptionEntity option = new GlobalFilterOptionEntity();
                        GlobalFilterOptionEntity.GlobalFilterOptionId optId = new GlobalFilterOptionEntity.GlobalFilterOptionId();
                        optId.setFilterId(savedGf.getId());
                        optId.setOptionValue(gfDto.getOptions().get(i));
                        option.setId(optId);
                        option.setFilter(savedGf);
                        option.setPositionIndex(i);
                        globalFilterOptionRepository.save(option);
                    }
                }
                if (gfDto.getValueMap() != null) {
                    gfDto.getValueMap().forEach((key, val) -> {
                        GlobalFilterValueMapEntity vm = new GlobalFilterValueMapEntity();
                        GlobalFilterValueMapEntity.GlobalFilterValueMapId vmId = new GlobalFilterValueMapEntity.GlobalFilterValueMapId();
                        vmId.setFilterId(savedGf.getId());
                        vmId.setMapKey(key);
                        vm.setId(vmId);
                        vm.setFilter(savedGf);
                        vm.setMapValue(val);
                        globalFilterValueMapRepository.save(vm);
                    });
                }
            }
        }
        if (dto.getWidgets() != null) {
            for (WidgetDto wDto : dto.getWidgets()) {
                WidgetEntity w = new WidgetEntity();
                w.setDashboard(dashboard);
                w.setWidgetType(ParsingUtils.parseEnum(WidgetType.class, wDto.getType(), WidgetType.KPI));
                w.setWidgetTitle(wDto.getTitle() != null ? wDto.getTitle() : "Widget");
                w.setShowTitle(wDto.isShowTitle());
                w.setWidgetDescription(wDto.getDescription());
                if (wDto.getQueryId() != null) {
                    UUID qId = ParsingUtils.parseUuid(wDto.getQueryId());
                    if (qId != null) {
                        dataQueryRepository.findById(qId).ifPresent(w::setQuery);
                    }
                }
                if (wDto.getNavigateToDashboardId() != null) {
                    UUID navId = ParsingUtils.parseUuid(wDto.getNavigateToDashboardId());
                    if (navId != null) {
                        dashboardRepository.findById(navId).ifPresent(w::setNavigateToDashboard);
                    }
                }
                if (wDto.getLayout() != null) {
                    w.setGridX(wDto.getLayout().getX());
                    w.setGridY(wDto.getLayout().getY());
                    w.setGridW(wDto.getLayout().getW());
                    w.setGridH(wDto.getLayout().getH());
                } else {
                    w.setGridX(0);
                    w.setGridY(0);
                    w.setGridW(4);
                    w.setGridH(3);
                }
                w.setRefreshInterval(ParsingUtils.parseEnum(RefreshInterval.class, wDto.getRefreshInterval(), RefreshInterval.INHERIT));
                if (wDto.getKpiFormat() != null) {
                    w.setKpiFormat(ParsingUtils.parseEnum(KpiFormat.class, wDto.getKpiFormat(), null));
                }
                w.setTextContent(wDto.getText());
                WidgetEntity savedWidget = widgetRepository.save(w);
                if (wDto.getFilters() != null) {
                    for (int i = 0; i < wDto.getFilters().size(); i++) {
                        WidgetFilterDto filterDto = wDto.getFilters().get(i);
                        WidgetFilterEntity filter = new WidgetFilterEntity();
                        filter.setWidget(savedWidget);
                        filter.setFilterLabel(filterDto.getLabel());
                        filter.setOperator(ParsingUtils.parseEnum(FilterOperator.class, filterDto.getOperator(), FilterOperator.EQ));
                        filter.setFilterValue(filterDto.getValue() != null ? filterDto.getValue() : "");
                        filter.setPositionIndex(i);
                        if (filterDto.getFieldId() != null && !filterDto.getFieldId().isBlank()) {
                            dataFieldRepository.findByFieldKey(filterDto.getFieldId()).ifPresent(filter::setTargetField);
                        }
                        widgetFilterRepository.save(filter);
                    }
                }
                if (wDto.getDatagrid() != null) {
                    WidgetDatagridConfigEntity dgConfig = new WidgetDatagridConfigEntity();
                    dgConfig.setWidgetId(savedWidget.getId());
                    dgConfig.setWidget(savedWidget);
                    dgConfig.setRowsPerPage(wDto.getDatagrid().getRowsPerPage());
                    dgConfig.setDensity(ParsingUtils.parseEnum(DataGridDensity.class, wDto.getDatagrid().getDensity(), DataGridDensity.NORMAL));
                    dgConfig.setShowToolbar(wDto.getDatagrid().getShowToolbar());
                    dgConfig.setShowSearch(wDto.getDatagrid().getShowSearch());
                    dgConfig.setShowPagination(wDto.getDatagrid().getShowPagination());
                    dgConfig.setShowTotals(wDto.getDatagrid().getShowTotals());
                    dgConfig.setSortable(wDto.getDatagrid().getSortable());
                    dgConfig.setFilterable(wDto.getDatagrid().getFilterable());
                    widgetDatagridConfigRepository.save(dgConfig);
                    if (wDto.getDatagrid().getVisibleColumns() != null) {
                        for (int i = 0; i < wDto.getDatagrid().getVisibleColumns().size(); i++) {
                            WidgetDatagridVisibleColumnEntity col = new WidgetDatagridVisibleColumnEntity();
                            WidgetDatagridVisibleColumnEntity.WidgetDatagridVisibleColumnId colId = new WidgetDatagridVisibleColumnEntity.WidgetDatagridVisibleColumnId();
                            colId.setWidgetId(savedWidget.getId());
                            colId.setColumnName(wDto.getDatagrid().getVisibleColumns().get(i));
                            col.setId(colId);
                            col.setWidgetDatagridConfig(dgConfig);
                            col.setPositionIndex(i);
                            widgetDatagridVisibleColumnRepository.save(col);
                        }
                    }
                }
            }
        }
    }
    private void deleteChildEntities(UUID dashboardId) {
        dashboardTagRepository.deleteAll(dashboardTagRepository.findByIdDashboardId(dashboardId));
        globalFilterRepository.deleteAll(globalFilterRepository.findByDashboardId(dashboardId));
        widgetRepository.deleteAll(widgetRepository.findByDashboardId(dashboardId));
    }
}
