package com.zhai.hw.service;

import com.zhai.hw.config.AppProperties;
import com.zhai.hw.exception.InvalidConfigurationException;
import com.zhai.hw.model.AppConfig;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfigurationLoaderServiceTest {

    @Mock
    private AppProperties appProperties;

    private ConfigurationLoaderService configurationLoaderService;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        configurationLoaderService = new ConfigurationLoaderService(appProperties, validator);
    }

    @Test
    void testLoadConfiguration_Success() {
        // Given
        when(appProperties.getConfigPath()).thenReturn("classpath:test-config.yml");

        // When
        AppConfig appConfig = configurationLoaderService.loadConfiguration();

        // Then
        assertNotNull(appConfig);
        assertEquals("Test App", appConfig.getAppName());
        assertEquals("1.0-test", appConfig.getVersion());
        assertEquals("testhost", appConfig.getServer().getHost());
        assertEquals(9090, appConfig.getServer().getPort());
        assertTrue(appConfig.getFeatureFlags().containsKey("newDashboard"));
        assertFalse(appConfig.getFeatureFlags().get("newDashboard"));
        assertTrue(appConfig.getFeatureFlags().containsKey("dataExport"));
        assertTrue(appConfig.getFeatureFlags().get("dataExport"));
        assertEquals(50, appConfig.getVmPool().getMaxSize());
        assertEquals(Duration.ofMinutes(15), appConfig.getVmPool().getIdleTimeout());
        assertEquals("test-os", appConfig.getVmPool().getDefaultOs());
        assertEquals(2, appConfig.getLabelVmMappings().size());
        assertEquals("test-gpu", appConfig.getLabelVmMappings().get(0).getLabel());
        assertEquals("test-gpu-vm", appConfig.getLabelVmMappings().get(0).getVmSeriesSize());
        assertEquals(1, appConfig.getLabelVmMappings().get(0).getRunnersPerVm());
        assertEquals("test-mem", appConfig.getLabelVmMappings().get(1).getLabel());
        assertEquals("test-mem-vm", appConfig.getLabelVmMappings().get(1).getVmSeriesSize());
        assertEquals(3, appConfig.getLabelVmMappings().get(1).getRunnersPerVm());
    }

    @Test
    void testLoadConfiguration_WithFilePath_Success() {
        // When
        AppConfig appConfig = configurationLoaderService.loadConfiguration("classpath:test-config.yml");

        // Then
        assertNotNull(appConfig);
        assertEquals("Test App", appConfig.getAppName());
        assertEquals("1.0-test", appConfig.getVersion());
    }

    @Test
    void testLoadConfiguration_NullPath() {
        // Given
        when(appProperties.getConfigPath()).thenReturn(null);

        // When/Then
        InvalidConfigurationException exception = assertThrows(
                InvalidConfigurationException.class,
                () -> configurationLoaderService.loadConfiguration()
        );
        assertTrue(exception.getMessage().contains("Configuration file path is not defined"));
    }

    @Test
    void testLoadConfiguration_EmptyPath() {
        // Given
        when(appProperties.getConfigPath()).thenReturn("");

        // When/Then
        InvalidConfigurationException exception = assertThrows(
                InvalidConfigurationException.class,
                () -> configurationLoaderService.loadConfiguration()
        );
        assertTrue(exception.getMessage().contains("Configuration file path is not defined"));
    }

    @Test
    void testLoadConfiguration_FileNotFound() {
        // Given
        String nonExistentPath = "classpath:non-existent-config.yml";

        // When/Then
        InvalidConfigurationException exception = assertThrows(
                InvalidConfigurationException.class,
                () -> configurationLoaderService.loadConfiguration(nonExistentPath)
        );
        assertTrue(exception.getMessage().contains("Error reading configuration file"));
    }

    @Test
    void testLoadConfiguration_MalformedYaml() {
        // When/Then
        InvalidConfigurationException exception = assertThrows(
                InvalidConfigurationException.class,
                () -> configurationLoaderService.loadConfiguration("classpath:malformed-config.yml")
        );
        assertTrue(exception.getMessage().contains("Error parsing YAML configuration file"));
    }

    @Test
    void testLoadConfiguration_ValidationFailure() throws IOException {
        // Create a test file with invalid configuration (missing required fields)
        // For this test, we'll use a real file that's missing required fields
        
        // This test assumes there's a YAML file in the test resources that's valid YAML but missing required fields
        // We could create this file dynamically, but for simplicity, we'll assume it exists
        
        // When/Then
        // The exact validation error will depend on which fields are missing in the test file
        InvalidConfigurationException exception = assertThrows(
                InvalidConfigurationException.class,
                () -> configurationLoaderService.loadConfiguration("classpath:invalid-config.yml")
        );
        assertTrue(exception.getMessage().contains("Invalid configuration"));
    }

    @Test
    void testLoadConfiguration_DurationParsing() {
        // Given
        when(appProperties.getConfigPath()).thenReturn("classpath:test-config.yml");

        // When
        AppConfig appConfig = configurationLoaderService.loadConfiguration();

        // Then
        assertNotNull(appConfig);
        assertEquals(Duration.ofMinutes(15), appConfig.getVmPool().getIdleTimeout());
    }
}
