package com.dynamicdashboard.cockpit.catalog.application;
import com.dynamicdashboard.cockpit.catalog.application.dto.DataFieldDto;
import com.dynamicdashboard.cockpit.catalog.application.dto.DataSourceDto;
import com.dynamicdashboard.cockpit.catalog.application.mapper.CatalogMapper;
import com.dynamicdashboard.cockpit.catalog.repository.DataFieldRepository;
import com.dynamicdashboard.cockpit.catalog.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class CatalogApplicationService {
    private final DataSourceRepository dataSourceRepository;
    private final DataFieldRepository dataFieldRepository;
    private final CatalogMapper catalogMapper;
    @Transactional(readOnly = true)
    public List<DataSourceDto> getAllDataSources() {
        return dataSourceRepository.findAll().stream()
                .map(catalogMapper::toDataSourceDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Optional<DataSourceDto> getDataSourceById(UUID id) {
        return dataSourceRepository.findById(id).map(catalogMapper::toDataSourceDto);
    }
    @Transactional(readOnly = true)
    public List<DataFieldDto> getFieldsByDataSourceId(UUID dataSourceId) {
        return dataFieldRepository.findByDataSourceId(dataSourceId).stream()
                .map(catalogMapper::toDataFieldDto)
                .collect(Collectors.toList());
    }
}
