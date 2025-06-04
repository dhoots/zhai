package com.zhai.hw.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VmPoolParameters {

    @Min(value = 0, message = "Minimum warm VMs cannot be negative")
    private int minimumWarmVms;

    @Min(value = 0, message = "Maximum pool size cannot be negative")
    private int maximumPoolSize;

    @Min(value = 0, message = "Scale-up trigger threshold cannot be negative")
    private int scaleUpTriggerThreshold;

    @NotNull(message = "Idle timeout must be specified")
    private Duration idleTimeout;
}
