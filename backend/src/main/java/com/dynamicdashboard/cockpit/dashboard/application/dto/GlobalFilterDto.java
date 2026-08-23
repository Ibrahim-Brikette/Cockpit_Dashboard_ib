package com.dynamicdashboard.cockpit.dashboard.application.dto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalFilterDto {
    private UUID id;
    private String name;
    private String label;
    private String input;
    private List<String> options;
    private UUID fieldId;
    private Map<String, String> valueMap;
    private String defaultValue;
    private boolean readerVisible;
}
