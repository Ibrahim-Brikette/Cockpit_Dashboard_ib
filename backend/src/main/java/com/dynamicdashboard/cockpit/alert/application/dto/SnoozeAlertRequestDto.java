package com.dynamicdashboard.cockpit.alert.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnoozeAlertRequestDto {
    private int minutes;
}