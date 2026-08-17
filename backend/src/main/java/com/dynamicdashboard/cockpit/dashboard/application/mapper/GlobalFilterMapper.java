package com.dynamicdashboard.cockpit.dashboard.application.mapper;
import com.dynamicdashboard.cockpit.dashboard.application.dto.GlobalFilterDto;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterOptionEntity;
import com.dynamicdashboard.cockpit.dashboard.domain.GlobalFilterValueMapEntity;
import com.dynamicdashboard.cockpit.dashboard.repository.GlobalFilterOptionRepository;
import com.dynamicdashboard.cockpit.dashboard.repository.GlobalFilterValueMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class GlobalFilterMapper {
    private final GlobalFilterOptionRepository globalFilterOptionRepository;
    private final GlobalFilterValueMapRepository globalFilterValueMapRepository;
    public GlobalFilterDto toDto(GlobalFilterEntity entity) {
        if (entity == null) return null;
        List<String> options = globalFilterOptionRepository.findByIdFilterIdOrderByPositionIndexAsc(entity.getId())
                .stream()
                .map(opt -> opt.getId().getOptionValue())
                .collect(Collectors.toList());
        Map<String, String> valueMap = new HashMap<>();
        globalFilterValueMapRepository.findByIdFilterId(entity.getId())
                .forEach(vm -> valueMap.put(vm.getId().getMapKey(), vm.getMapValue()));
        return GlobalFilterDto.builder()
                .id(entity.getId())
                .name(entity.getFilterName())
                .label(entity.getFilterLabel())
                .input(entity.getInputType() != null ? entity.getInputType().name().toLowerCase() : "select")
                .options(options)
                .fieldId(entity.getTargetField() != null ? entity.getTargetField().getId() : null)
                .valueMap(valueMap.isEmpty() ? null : valueMap)
                .defaultValue(entity.getDefaultValue())
                .readerVisible(entity.isReaderVisible())
                .build();
    }
}
