package com.spawnbase.metadata.dto;

import com.spawnbase.provisioning.model.DatabaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


@Data
public class CreateInstanceRequest {


    @NotBlank(message = "Instance name is required")
    @Size(min = 3, max = 100,
            message = "Name must be between 3 and 100 characters")
    @Pattern(
            regexp = "^[a-z0-9-]+$",
            message = "Name can only contain lowercase letters, numbers, and hyphens"
    )
    private String name;


    @NotNull(message = "Database type is required")
    private DatabaseType dbType;


    @NotBlank(message = "Owner ID is required")
    private String ownerId;
}