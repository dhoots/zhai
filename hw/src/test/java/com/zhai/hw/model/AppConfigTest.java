package com.zhai.hw.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AppConfigTest {

    private static Validator validator;

    @BeforeAll
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private LabelVmMapping createValidLabelVmMapping(String label) {
        VmPoolParameters poolParams = new VmPoolParameters(1, 5, 1, Duration.ofHours(1));
        return new LabelVmMapping(
                label, "Standard_D2s_v3", "Ubuntu 20.04", "East US",
                "vnet-test", "subnet-test", "nsg-test", "Premium_LRS 128GB",
                1, poolParams
        );
    }
    
    private AppConfig createValidAppConfig(List<LabelVmMapping> labelVmMappings) {
        String appName = "Test App";
        String version = "1.0.0";
        AppConfig.ServerConfig serverConfig = new AppConfig.ServerConfig("localhost", 8080);
        Map<String, Boolean> featureFlags = new HashMap<>();
        featureFlags.put("newDashboard", true);
        featureFlags.put("dataExport", false);
        AppConfig.VmPoolConfig vmPoolConfig = new AppConfig.VmPoolConfig(100, Duration.ofMinutes(30), "ubuntu-latest");
        
        return new AppConfig(appName, version, serverConfig, featureFlags, vmPoolConfig, labelVmMappings);
    }

    @Test
    void testSuccessfulInstantiationAndGetters() {
        LabelVmMapping mapping1 = createValidLabelVmMapping("label1");
        LabelVmMapping mapping2 = createValidLabelVmMapping("label2");
        List<LabelVmMapping> mappings = List.of(mapping1, mapping2);

        AppConfig appConfig = createValidAppConfig(mappings);

        assertEquals(2, appConfig.getLabelVmMappings().size());
        assertTrue(appConfig.getLabelVmMappings().contains(mapping1));
        assertTrue(appConfig.getLabelVmMappings().contains(mapping2));
        assertEquals("Test App", appConfig.getAppName());
        assertEquals("1.0.0", appConfig.getVersion());
        assertEquals("localhost", appConfig.getServer().getHost());
        assertEquals(8080, appConfig.getServer().getPort());
        assertTrue(appConfig.getFeatureFlags().get("newDashboard"));
        assertFalse(appConfig.getFeatureFlags().get("dataExport"));
        assertEquals(100, appConfig.getVmPool().getMaxSize());
        assertEquals(Duration.ofMinutes(30), appConfig.getVmPool().getIdleTimeout());
        assertEquals("ubuntu-latest", appConfig.getVmPool().getDefaultOs());
    }

    @Test
    void testEqualsAndHashCode() {
        LabelVmMapping mapping1a = createValidLabelVmMapping("label1");
        LabelVmMapping mapping2a = createValidLabelVmMapping("label2");
        AppConfig config1 = createValidAppConfig(List.of(mapping1a, mapping2a));

        LabelVmMapping mapping1b = createValidLabelVmMapping("label1"); // Same content
        LabelVmMapping mapping2b = createValidLabelVmMapping("label2"); // Same content
        AppConfig config2 = createValidAppConfig(List.of(mapping1b, mapping2b));

        LabelVmMapping mapping3 = createValidLabelVmMapping("label3");
        AppConfig config3 = createValidAppConfig(List.of(mapping3));

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());

        assertNotEquals(config1, config3);
        assertNotEquals(config1.hashCode(), config3.hashCode());
        
        assertNotEquals(config1, null);
        assertNotEquals(config1, new Object());
    }

    @Test
    void testValidation_validAppConfig() {
        AppConfig appConfig = createValidAppConfig(List.of(createValidLabelVmMapping("label1")));
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
        assertTrue(violations.isEmpty(), "Should be no validation errors for a valid AppConfig");
    }

    @Test
    void testValidation_emptyLabelVmMappings() {
        AppConfig appConfig = createValidAppConfig(Collections.emptyList());
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
        assertEquals(1, violations.size());
        assertEquals("Label VM mappings cannot be empty", violations.iterator().next().getMessage());
    }

    @Test
    void testValidation_nullLabelVmMappings() {
        // Create a valid AppConfig but with null labelVmMappings
        String appName = "Test App";
        String version = "1.0.0";
        AppConfig.ServerConfig serverConfig = new AppConfig.ServerConfig("localhost", 8080);
        Map<String, Boolean> featureFlags = new HashMap<>();
        featureFlags.put("newDashboard", true);
        AppConfig.VmPoolConfig vmPoolConfig = new AppConfig.VmPoolConfig(100, Duration.ofMinutes(30), "ubuntu-latest");
        
        AppConfig appConfig = new AppConfig(appName, version, serverConfig, featureFlags, vmPoolConfig, null);
        
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
        // Depending on how @NotEmpty is implemented by the provider, null might also trigger it.
        // Or it could be a different violation if @NotNull was also present (it's not here, relying on @NotEmpty).
        // For Hibernate Validator, @NotEmpty implies @NotNull.
        assertEquals(1, violations.size());
        ConstraintViolation<AppConfig> violation = violations.iterator().next();
        // Message could be "must not be empty" or "must not be null" depending on validator
        assertTrue(violation.getMessage().contains("Label VM mappings cannot be empty") || violation.getMessage().contains("must not be null"));
        assertEquals("labelVmMappings", violation.getPropertyPath().toString());
    }

    @Test
    void testValidation_invalidNestedLabelVmMapping() {
        LabelVmMapping invalidMapping = new LabelVmMapping(
                "", "size", "os", "region", "vnet", "subnet", "nsg", "disk",
                1, new VmPoolParameters(1,2,1,Duration.ofHours(1)) // Blank label
        );
        AppConfig appConfig = createValidAppConfig(List.of(invalidMapping));
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
        
        assertEquals(1, violations.size());
        ConstraintViolation<AppConfig> violation = violations.iterator().next();
        assertEquals("Label cannot be blank", violation.getMessage());
        assertEquals("labelVmMappings[0].label", violation.getPropertyPath().toString());
    }

    @Test
    void testValidation_invalidNestedPoolParametersInList() {
        VmPoolParameters invalidPoolParams = new VmPoolParameters(-1, 5, 1, Duration.ofHours(1)); // Invalid minWarmVms
        LabelVmMapping mappingWithInvalidPool = new LabelVmMapping(
                "label1", "size", "os", "region", "vnet", "subnet", "nsg", "disk",
                1, invalidPoolParams
        );
        AppConfig appConfig = createValidAppConfig(List.of(mappingWithInvalidPool));
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);

        assertEquals(1, violations.size());
        ConstraintViolation<AppConfig> violation = violations.iterator().next();
        assertEquals("Minimum warm VMs cannot be negative", violation.getMessage());
        assertEquals("labelVmMappings[0].poolParameters.minimumWarmVms", violation.getPropertyPath().toString());
    }
    
    @Test
    void testValidation_missingAppName() {
        // Create AppConfig with null appName
        AppConfig appConfig = new AppConfig(
            null, // Missing appName
            "1.0.0",
            new AppConfig.ServerConfig("localhost", 8080),
            Map.of("feature", true),
            new AppConfig.VmPoolConfig(100, Duration.ofMinutes(30), "ubuntu-latest"),
            List.of(createValidLabelVmMapping("label1"))
        );
        
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
        assertEquals(1, violations.size());
        ConstraintViolation<AppConfig> violation = violations.iterator().next();
        assertEquals("Application name cannot be blank", violation.getMessage());
    }
    
    @Test
    void testValidation_missingServerHost() {
        // Create AppConfig with null server host
        AppConfig appConfig = new AppConfig(
            "Test App",
            "1.0.0",
            new AppConfig.ServerConfig(null, 8080), // Missing host
            Map.of("feature", true),
            new AppConfig.VmPoolConfig(100, Duration.ofMinutes(30), "ubuntu-latest"),
            List.of(createValidLabelVmMapping("label1"))
        );
        
        Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
        assertEquals(1, violations.size());
        ConstraintViolation<AppConfig> violation = violations.iterator().next();
        assertEquals("Host cannot be blank", violation.getMessage());
    }
}
