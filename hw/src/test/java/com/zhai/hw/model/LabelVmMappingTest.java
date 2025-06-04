package com.zhai.hw.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class LabelVmMappingTest {

    private static Validator validator;

    @BeforeAll
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private VmPoolParameters createValidPoolParameters() {
        return new VmPoolParameters(3, 10, 3, Duration.ofHours(1));
    }

    @Test
    void testSuccessfulInstantiationAndGetters() {
        VmPoolParameters poolParams = createValidPoolParameters();
        LabelVmMapping mapping = new LabelVmMapping(
                "android-small", "Standard_D8s_v6", "Ubuntu 24.04",
                "US WEST", "hw-vnet", "hw-subnet", "hw-nsg",
                "Premium SSD V2 1024GB", 1, poolParams
        );

        assertEquals("android-small", mapping.getLabel());
        assertEquals("Standard_D8s_v6", mapping.getVmSeriesSize());
        assertEquals("Ubuntu 24.04", mapping.getOsImage());
        assertEquals("US WEST", mapping.getRegion());
        assertEquals("hw-vnet", mapping.getVNet());
        assertEquals("hw-subnet", mapping.getSubnet());
        assertEquals("hw-nsg", mapping.getNetworkSecurityGroup());
        assertEquals("Premium SSD V2 1024GB", mapping.getDiskTypeSize());
        assertEquals(1, mapping.getRunnersPerVm());
        assertEquals(poolParams, mapping.getPoolParameters());
    }

    @Test
    void testEqualsAndHashCode() {
        VmPoolParameters poolParams1 = createValidPoolParameters();
        LabelVmMapping mapping1 = new LabelVmMapping("label1", "size1", "os1", "region1", "vnet1", "subnet1", "nsg1", "disk1", 1, poolParams1);

        VmPoolParameters poolParams2 = createValidPoolParameters(); // Same content as poolParams1
        LabelVmMapping mapping2 = new LabelVmMapping("label1", "size1", "os1", "region1", "vnet1", "subnet1", "nsg1", "disk1", 1, poolParams2);

        VmPoolParameters poolParams3 = new VmPoolParameters(1, 5, 1, Duration.ofHours(2));
        LabelVmMapping mapping3 = new LabelVmMapping("label2", "size2", "os2", "region2", "vnet2", "subnet2", "nsg2", "disk2", 2, poolParams3);

        assertEquals(mapping1, mapping2);
        assertEquals(mapping1.hashCode(), mapping2.hashCode());

        assertNotEquals(mapping1, mapping3);
        assertNotEquals(mapping1.hashCode(), mapping3.hashCode());

        assertNotEquals(mapping1, null);
        assertNotEquals(mapping1, new Object());
    }

    @Test
    void testValidation_validMapping() {
        LabelVmMapping mapping = new LabelVmMapping(
                "valid-label", "valid-size", "valid-os", "valid-region",
                "valid-vnet", "valid-subnet", "valid-nsg", "valid-disk",
                1, createValidPoolParameters()
        );
        Set<ConstraintViolation<LabelVmMapping>> violations = validator.validate(mapping);
        assertTrue(violations.isEmpty(), "Should be no validation errors for a valid mapping");
    }

    @Test
    void testValidation_blankLabel() {
        LabelVmMapping mapping = new LabelVmMapping("", "size", "os", "region", "vnet", "subnet", "nsg", "disk", 1, createValidPoolParameters());
        Set<ConstraintViolation<LabelVmMapping>> violations = validator.validate(mapping);
        assertEquals(1, violations.size());
        assertEquals("Label cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testValidation_nullPoolParameters() {
        LabelVmMapping mapping = new LabelVmMapping("label", "size", "os", "region", "vnet", "subnet", "nsg", "disk", 1, null);
        Set<ConstraintViolation<LabelVmMapping>> violations = validator.validate(mapping);
        assertEquals(1, violations.size());
        assertEquals("Pool parameters must be specified", violations.iterator().next().getMessage());
    }

    @Test
    void testValidation_invalidRunnersPerVm() {
        LabelVmMapping mapping = new LabelVmMapping("label", "size", "os", "region", "vnet", "subnet", "nsg", "disk", 0, createValidPoolParameters());
        Set<ConstraintViolation<LabelVmMapping>> violations = validator.validate(mapping);
        assertEquals(1, violations.size());
        assertEquals("Runners per VM must be at least 1", violations.iterator().next().getMessage());
    }
    
    @Test
    void testValidation_invalidNestedPoolParameters() {
        VmPoolParameters invalidPoolParams = new VmPoolParameters(-1, 10, 3, Duration.ofHours(1)); // Invalid minimumWarmVms
        LabelVmMapping mapping = new LabelVmMapping(
                "valid-label", "valid-size", "valid-os", "valid-region",
                "valid-vnet", "valid-subnet", "valid-nsg", "valid-disk",
                1, invalidPoolParams
        );
        Set<ConstraintViolation<LabelVmMapping>> violations = validator.validate(mapping);
        assertEquals(1, violations.size());
        ConstraintViolation<LabelVmMapping> violation = violations.iterator().next();
        assertEquals("Minimum warm VMs cannot be negative", violation.getMessage());
        assertEquals("poolParameters.minimumWarmVms", violation.getPropertyPath().toString());
    }
}
