package com.example.boardinghouse.dto.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * DTO cho yêu cầu cập nhật thông tin tòa nhà/khu trọ.
 */
public class UpdatePropertyRequest {

    @NotBlank(message = "Property name is required")
    private String name;

    @NotBlank(message = "Property address is required")
    private String address;

    private String description;
}
