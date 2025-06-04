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

public class VmPoolParametersTest {

    private static Validator validator;

    @BeforeAll
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testSuccessfulInstantiationAndGetters() {
        Duration idleTimeout = Duration.ofHours(3);
        VmPoolParameters params = new VmPoolParameters(3, 12, 3, idleTimeout);

        assertEquals(3, params.getMinimumWarmVms());
        assertEquals(12, params.getMaximumPoolSize());
        assertEquals(3, params.getScaleUpTriggerThreshold());
        assertEquals(idleTimeout, params.getIdleTimeout());
    }

    @Test
    void testEqualsAndHashCode() {
        Duration idleTimeout1 = Duration.ofHours(3);
        VmPoolParameters params1 = new VmPoolParameters(3, 12, 3, idleTimeout1);

        Duration idleTimeout2 = Duration.ofHours(3);
        VmPoolParameters params2 = new VmPoolParameters(3, 12, 3, idleTimeout2);

        Duration idleTimeout3 = Duration.ofHours(1);
        VmPoolParameters params3 = new VmPoolParameters(1, 10, 1, idleTimeout3);

        assertEquals(params1, params2);
        assertEquals(params1.hashCode(), params2.hashCode());

        assertNotEquals(params1, params3);
        assertNotEquals(params1.hashCode(), params3.hashCode());

        assertNotEquals(params1, null);
        assertNotEquals(params1, new Object());
    }

    @Test
    void testValidation_validParameters() {
        VmPoolParameters params = new VmPoolParameters(3, 12, 3, Duration.ofHours(1));
        Set<ConstraintViolation<VmPoolParameters>> violations = validator.validate(params);
        assertTrue(violations.isEmpty(), "Should be no validation errors for valid parameters");
    }

    @Test
    void testValidation_invalidMinimumWarmVms() {
        VmPoolParameters params = new VmPoolParameters(-1, 12, 3, Duration.ofHours(1));
        Set<ConstraintViolation<VmPoolParameters>> violations = validator.validate(params);
        assertEquals(1, violations.size());
        ConstraintViolation<VmPoolParameters> violation = violations.iterator().next();
        assertEquals("Minimum warm VMs cannot be negative", violation.getMessage());
        assertEquals("minimumWarmVms", violation.getPropertyPath().toString());
    }

    @Test
    void testValidation_invalidMaximumPoolSize() {
        VmPoolParameters params = new VmPoolParameters(3, -1, 3, Duration.ofHours(1));
        Set<ConstraintViolation<VmPoolParameters>> violations = validator.validate(params);
        assertEquals(1, violations.size());
        ConstraintViolation<VmPoolParameters> violation = violations.iterator().next();
        assertEquals("Maximum pool size cannot be negative", violation.getMessage());
        assertEquals("maximumPoolSize", violation.getPropertyPath().toString());
    }

    @Test
    void testValidation_invalidScaleUpTriggerThreshold() {
        VmPoolParameters params = new VmPoolParameters(3, 12, -1, Duration.ofHours(1));
        Set<ConstraintViolation<VmPoolParameters>> violations = validator.validate(params);
        assertEquals(1, violations.size());
        ConstraintViolation<VmPoolParameters> violation = violations.iterator().next();
        assertEquals("Scale-up trigger threshold cannot be negative", violation.getMessage());
        assertEquals("scaleUpTriggerThreshold", violation.getPropertyPath().toString());
    }

    @Test
    void testValidation_nullIdleTimeout() {
        VmPoolParameters params = new VmPoolParameters(3, 12, 3, null);
        Set<ConstraintViolation<VmPoolParameters>> violations = validator.validate(params);
        assertEquals(1, violations.size());
        ConstraintViolation<VmPoolParameters> violation = violations.iterator().next();
        assertEquals("Idle timeout must be specified", violation.getMessage());
        assertEquals("idleTimeout", violation.getPropertyPath().toString());
    }
}
