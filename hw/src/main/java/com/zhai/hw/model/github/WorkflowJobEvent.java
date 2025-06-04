package com.zhai.hw.model.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a GitHub workflow_job webhook event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowJobEvent {
    
    @NotBlank(message = "Action cannot be blank")
    private String action;
    
    @NotNull(message = "Workflow job cannot be null")
    @Valid
    @JsonProperty("workflow_job")
    private WorkflowJob workflowJob;
}
