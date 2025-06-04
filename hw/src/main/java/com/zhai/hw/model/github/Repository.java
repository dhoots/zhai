package com.zhai.hw.model.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a GitHub repository in a webhook payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repository {
    
    @NotBlank(message = "Repository full name cannot be blank")
    @JsonProperty("full_name")
    private String fullName;
}
