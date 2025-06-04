package com.zhai.hw.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelVmMapping {

    @NotBlank(message = "Label cannot be blank")
    private String label;

    @NotBlank(message = "VM series/size cannot be blank")
    private String vmSeriesSize;

    @NotBlank(message = "OS image cannot be blank")
    private String osImage;

    @NotBlank(message = "Region cannot be blank")
    private String region;

    @NotBlank(message = "VNet cannot be blank")
    private String vNet;

    @NotBlank(message = "Subnet cannot be blank")
    private String subnet;

    @NotBlank(message = "Network security group cannot be blank")
    private String networkSecurityGroup;

    @NotBlank(message = "Disk type/size cannot be blank")
    private String diskTypeSize;

    @Min(value = 1, message = "Runners per VM must be at least 1")
    private int runnersPerVm;

    @NotNull(message = "Pool parameters must be specified")
    @Valid // Ensures nested validation of VmPoolParameters
    private VmPoolParameters poolParameters;
}
