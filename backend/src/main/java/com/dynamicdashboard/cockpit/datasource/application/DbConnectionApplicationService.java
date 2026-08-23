package com.dynamicdashboard.cockpit.datasource.application;
import com.dynamicdashboard.cockpit.catalog.repository.DataSourceRepository;
import com.dynamicdashboard.cockpit.datasource.application.dto.DataSourceConnectionRequestDto;
import com.dynamicdashboard.cockpit.datasource.application.dto.DbConnectionResponseDto;
import com.dynamicdashboard.cockpit.datasource.domain.DbConnectionEntity;
import com.dynamicdashboard.cockpit.datasource.repository.DbConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class DbConnectionApplicationService {
    private final DbConnectionRepository dbConnectionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final VaultSecretService vaultSecretService;
    private final DatabaseSchemaExtractor databaseSchemaExtractor;
    @Transactional(readOnly = true)
    public Map<String, Object> testConnection(DataSourceConnectionRequestDto request) {
        DbConnectionEntity tempEntity = new DbConnectionEntity();
        tempEntity.setDbType(request.getDbType());
        tempEntity.setDbHost(request.getDbHost());
        tempEntity.setDbPort(request.getDbPort());
        tempEntity.setDbName(request.getDbName());
        tempEntity.setDbUsername(request.getDbUsername());
        tempEntity.setUseSsl(request.isUseSsl());
        try {
            var schemaPreview = databaseSchemaExtractor.previewSchema(tempEntity, request.getDbPassword());
            return Map.of(
                "success", true,
                "message", "Connexion à la base de données établie avec succès !",
                "detectedSchema", schemaPreview
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "Erreur de connexion : " + e.getMessage());
        }
    }
    @Transactional
    public String createConnection(DataSourceConnectionRequestDto request) {
        String vaultSecretKey = vaultSecretService.storePassword(request.getDbPassword());
        DbConnectionEntity entity = new DbConnectionEntity();
        entity.setCreatedBy("admin");
        entity.setUpdatedBy("admin");
        entity.setConnectionName(request.getConnectionName());
        entity.setDbType(request.getDbType());
        entity.setDbHost(request.getDbHost());
        entity.setDbPort(request.getDbPort());
        entity.setDbName(request.getDbName());
        entity.setDbUsername(request.getDbUsername());
        entity.setUseSsl(request.isUseSsl());
        entity.setVaultSecretKey(vaultSecretKey);
        dbConnectionRepository.save(entity);
        databaseSchemaExtractor.extractAndSaveSchema(entity, request.getDbPassword());
        return entity.getId().toString();
    }
    @Transactional
    public void updateConnection(UUID id, DataSourceConnectionRequestDto request) {
        DbConnectionEntity entity = dbConnectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found with id: " + id));
        entity.setConnectionName(request.getConnectionName());
        entity.setDbType(request.getDbType());
        entity.setDbHost(request.getDbHost());
        entity.setDbPort(request.getDbPort());
        entity.setDbName(request.getDbName());
        entity.setDbUsername(request.getDbUsername());
        entity.setUseSsl(request.isUseSsl());
        String rawPassword = request.getDbPassword();
        if (rawPassword != null && !rawPassword.isBlank()) {
            String vaultSecretKey = vaultSecretService.storePassword(rawPassword);
            entity.setVaultSecretKey(vaultSecretKey);
        } else if (entity.getVaultSecretKey() != null) {
            rawPassword = vaultSecretService.retrievePassword(entity.getVaultSecretKey());
        }
        entity = dbConnectionRepository.save(entity);
        if (rawPassword != null && !rawPassword.isBlank()) {
            databaseSchemaExtractor.extractAndSaveSchema(entity, rawPassword);
        }
    }
    @Transactional
    public void deleteConnection(UUID id) {
        DbConnectionEntity entity = dbConnectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Connection not found with id: " + id));
        var dataSources = dataSourceRepository.findByDbConnectionId(id);
        dataSourceRepository.deleteAll(dataSources);
        dbConnectionRepository.delete(entity);
    }
    @Transactional(readOnly = true)
    public List<DbConnectionResponseDto> getAllConnections() {
        return dbConnectionRepository.findAll().stream().map(entity -> {
            DbConnectionResponseDto dto = new DbConnectionResponseDto();
            dto.setId(entity.getId());
            dto.setConnectionName(entity.getConnectionName());
            dto.setDbType(entity.getDbType());
            dto.setDbHost(entity.getDbHost());
            dto.setDbPort(entity.getDbPort());
            dto.setDbName(entity.getDbName());
            dto.setDbUsername(entity.getDbUsername());
            dto.setUseSsl(entity.isUseSsl());
            dto.setTableCount(dataSourceRepository.countByDbConnectionId(entity.getId()));
            return dto;
        }).collect(Collectors.toList());
    }
}
