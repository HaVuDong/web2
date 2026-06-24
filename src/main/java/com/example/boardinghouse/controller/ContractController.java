package com.example.boardinghouse.controller;

import com.example.boardinghouse.common.dto.ApiResponse;
import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.dto.contract.CreateContractRequest;
import com.example.boardinghouse.dto.contract.RenewContractRequest;
import com.example.boardinghouse.dto.contract.TerminateContractRequest;
import com.example.boardinghouse.dto.contract.UpdateContractRequest;
import com.example.boardinghouse.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    /**
     * API: Lấy danh sách toàn bộ hợp đồng thuê phòng.
     * Endpoint: GET /api/contracts
     */
    @GetMapping
    public ApiResponse<List<Contract>> getAllContracts() {
        return ApiResponse.success(contractService.getAllContracts());
    }

    /**
     * API: Tạo mới một hợp đồng thuê phòng.
     * Endpoint: POST /api/contracts
     * 
     * @param request Dữ liệu tạo hợp đồng (phòng, tiền cọc, tiền thuê, ngày bắt đầu, ngày kết thúc...)
     */
    @PostMapping
    public ApiResponse<Contract> createContract(@Valid @RequestBody CreateContractRequest request) {
        Contract contract = contractService.createContract(request);
        return ApiResponse.success("Contract created successfully", contract);
    }

    /**
     * API: Lấy thông tin chi tiết của một hợp đồng theo ID.
     * Endpoint: GET /api/contracts/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Contract> getContractById(@PathVariable String id) {
        return ApiResponse.success(contractService.getContractById(id));
    }

    /**
     * API: Cập nhật thông tin hợp đồng.
     * Endpoint: PUT /api/contracts/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<Contract> updateContract(
            @PathVariable String id,
            @Valid @RequestBody UpdateContractRequest request
    ) {
        Contract contract = contractService.updateContract(id, request);
        return ApiResponse.success("Contract updated successfully", contract);
    }

    /**
     * API: Chấm dứt hợp đồng trước hạn hoặc đúng hạn.
     * Endpoint: PATCH /api/contracts/{id}/terminate
     */
    @PatchMapping("/{id}/terminate")
    public ApiResponse<Contract> terminateContract(
            @PathVariable String id,
            @RequestBody(required = false) TerminateContractRequest request
    ) {
        Contract contract = contractService.terminateContract(id, request);
        return ApiResponse.success("Contract terminated successfully", contract);
    }

    /**
     * API: Gia hạn hợp đồng hiện tại.
     * Endpoint: PATCH /api/contracts/{id}/renew
     */
    @PatchMapping("/{id}/renew")
    public ApiResponse<Contract> renewContract(
            @PathVariable String id,
            @Valid @RequestBody RenewContractRequest request
    ) {
        Contract contract = contractService.renewContract(id, request);
        return ApiResponse.success("Contract renewed successfully", contract);
    }
}
