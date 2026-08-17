package com.dynamicdashboard.cockpit.query.application.dto;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryJoinDto {
    private String id;
    private String leftSourceId;
    private String leftFieldId;
    private String type;
    private String rightSourceId;
    private String rightFieldId;
}
