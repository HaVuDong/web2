package com.example.boardinghouse.dto.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePropertyRequest {

    @NotBlank(message = "Property name is required")
    private String name;

    @NotBlank(message = "Property address is required")
    private String address;

    private String description;
}
