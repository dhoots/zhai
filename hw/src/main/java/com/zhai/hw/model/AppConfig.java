package com.zhai.hw.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {

    @NotBlank(message = "Application name cannot be blank")
    private String appName;

    @NotBlank(message = "Version cannot be blank")
    private String version;

    @NotNull(message = "Server configuration cannot be null")
    @Valid
    private ServerConfig server;

    @NotNull(message = "Feature flags cannot be null")
    private Map<String, Boolean> featureFlags;

    @NotNull(message = "VM pool configuration cannot be null")
    @Valid
    private VmPoolConfig vmPool;

    @JsonProperty("labelVmMappings")
    @NotEmpty(message = "Label VM mappings cannot be empty")
    @Valid // Ensures nested validation of LabelVmMapping objects in the list
    private List<LabelVmMapping> labelVmMappings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerConfig {
        @NotBlank(message = "Host cannot be blank")
        private String host;

        @NotNull(message = "Port cannot be null")
        private Integer port;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VmPoolConfig {
        @NotNull(message = "Max size cannot be null")
        private Integer maxSize;

        @NotNull(message = "Idle timeout cannot be null")
        private Duration idleTimeout;

        @NotBlank(message = "Default OS cannot be blank")
        private String defaultOs;
    }
}
