package com.zhai.hw.model.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a GitHub workflow job in a webhook payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowJob {
    
    private long id;
    
    @JsonProperty("run_id")
    private long runId;
    
    private String status;
    
    @NotNull(message = "Labels cannot be null")
    private List<String> labels;
    
    @NotNull(message = "Repository cannot be null")
    @Valid
    private Repository repository;
}
