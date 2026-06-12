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

    @GetMapping
    public ApiResponse<List<Contract>> getAllContracts() {
        return ApiResponse.success(contractService.getAllContracts());
    }

    @PostMapping
    public ApiResponse<Contract> createContract(@Valid @RequestBody CreateContractRequest request) {
        Contract contract = contractService.createContract(request);
        return ApiResponse.success("Contract created successfully", contract);
    }

    @GetMapping("/{id}")
    public ApiResponse<Contract> getContractById(@PathVariable String id) {
        return ApiResponse.success(contractService.getContractById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Contract> updateContract(
            @PathVariable String id,
            @Valid @RequestBody UpdateContractRequest request
    ) {
        Contract contract = contractService.updateContract(id, request);
        return ApiResponse.success("Contract updated successfully", contract);
    }

    @PatchMapping("/{id}/terminate")
    public ApiResponse<Contract> terminateContract(
            @PathVariable String id,
            @RequestBody(required = false) TerminateContractRequest request
    ) {
        Contract contract = contractService.terminateContract(id, request);
        return ApiResponse.success("Contract terminated successfully", contract);
    }

    @PatchMapping("/{id}/renew")
    public ApiResponse<Contract> renewContract(
            @PathVariable String id,
            @Valid @RequestBody RenewContractRequest request
    ) {
        Contract contract = contractService.renewContract(id, request);
        return ApiResponse.success("Contract renewed successfully", contract);
    }
}
