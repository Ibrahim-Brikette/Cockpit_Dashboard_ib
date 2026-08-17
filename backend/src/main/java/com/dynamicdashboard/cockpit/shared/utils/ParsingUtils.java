package com.dynamicdashboard.cockpit.shared.utils;
import java.util.UUID;
public class ParsingUtils {
    public static UUID parseUuid(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return UUID.fromString(val);
        } catch (Exception e) {
            return null;
        }
    }
    public static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String formatted = value.toUpperCase().replace("-", "_").trim();
        if (formatted.equals("5S")) formatted = "S5";
        if (formatted.equals("10S")) formatted = "S10";
        if (formatted.equals("30S")) formatted = "S30";
        if (formatted.equals("1MIN")) formatted = "M1";
        if (formatted.equals("5MIN")) formatted = "M5";
        if (formatted.equals("15MIN")) formatted = "M15";
        if (formatted.equals("30MIN")) formatted = "M30";
        if (formatted.equals("1H")) formatted = "H1";
        try {
            return Enum.valueOf(enumClass, formatted);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
