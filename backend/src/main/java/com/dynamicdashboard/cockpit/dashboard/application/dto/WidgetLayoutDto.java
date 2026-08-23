package com.dynamicdashboard.cockpit.dashboard.application.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WidgetLayoutDto {
    private int x;
    private int y;
    private int w;
    private int h;
}
