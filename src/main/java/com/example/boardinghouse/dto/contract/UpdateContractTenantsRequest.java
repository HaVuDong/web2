package com.example.boardinghouse.dto.contract;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
/**
 * DTO cho yêu cầu cập nhật danh sách khách thuê của hợp đồng đang hoạt động.
 */
public class UpdateContractTenantsRequest {

    @NotEmpty(message = "Danh sách khách thuê không được để trống")
    private List<String> tenantIds;
}
