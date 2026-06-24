package com.example.boardinghouse.dto.tenant;

import com.example.boardinghouse.domain.enums.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
/**
 * DTO cho yêu cầu thêm mới khách thuê.
 */
public class CreateTenantRequest {

    @NotBlank(message = "Tenant full name is required")
    private String fullName;

    @NotBlank(message = "Tenant phone is required")
    private String phone;

    @Email(message = "Tenant email must be valid")
    private String email;

    private String identityNumber;

    private LocalDate dateOfBirth;

    private String permanentAddress;

    private String currentRoomId;

    private TenantStatus status;

    private String note;
}
