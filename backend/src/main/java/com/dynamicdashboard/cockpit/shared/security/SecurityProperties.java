package com.dynamicdashboard.cockpit.shared.security;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "cockpit.security")
public class SecurityProperties {
    @NotEmpty
    private List<@NotBlank String> allowedOrigins = new ArrayList<>();
    @NotBlank
    private String requiredAudience;
}
