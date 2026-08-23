package com.dynamicdashboard.cockpit.catalog.application.mapper;
import com.dynamicdashboard.cockpit.catalog.application.dto.DataFieldDto;
import com.dynamicdashboard.cockpit.catalog.application.dto.DataSourceDto;
import com.dynamicdashboard.cockpit.catalog.domain.DataFieldEntity;
import com.dynamicdashboard.cockpit.catalog.domain.DataSourceEntity;
import com.dynamicdashboard.cockpit.catalog.repository.DataFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class CatalogMapper {
    private final DataFieldRepository dataFieldRepository;
    public DataSourceDto toDataSourceDto(DataSourceEntity entity) {
        if (entity == null) return null;
        List<DataFieldDto> fields = dataFieldRepository.findByDataSourceId(entity.getId()).stream()
                .map(this::toDataFieldDto)
                .collect(Collectors.toList());
        String appLabel = entity.getHostApplication() != null ? entity.getHostApplication() : "ERP";
        return DataSourceDto.builder()
                .id(entity.getId())
                .key(entity.getSourceKey())
                .label(entity.getSourceLabel())
                .description(entity.getSourceDescription())
                .app(appLabel)
                .active(entity.isActive())
                .fields(fields)
                .build();
    }
    public DataFieldDto toDataFieldDto(DataFieldEntity entity) {
        if (entity == null) return null;
        return DataFieldDto.builder()
                .id(entity.getId())
                .key(entity.getFieldKey())
                .label(entity.getFieldLabel())
                .type(entity.getFieldType() != null ? entity.getFieldType().name().toLowerCase() : "text")
                .description(entity.getFieldDescription())
                .nullable(entity.isNullable())
                .build();
    }
}
