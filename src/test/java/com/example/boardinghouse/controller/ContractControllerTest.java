package com.example.boardinghouse.controller;

import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.enums.ContractStatus;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.ContractService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContractController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContractService contractService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createContractReturnsCreatedContract() throws Exception {
        Contract contract = activeContract();
        when(contractService.createContract(any())).thenReturn(contract);

        mockMvc.perform(post("/api/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "tenantIds": ["tenant-1"],
                                  "startDate": "2026-06-01",
                                  "endDate": "2027-06-01",
                                  "monthlyRent": 2500000,
                                  "deposit": 2500000,
                                  "paymentDueDay": 5,
                                  "note": "Hop dong 12 thang"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contract created successfully"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void createContractRejectsInvalidPaymentDueDay() throws Exception {
        mockMvc.perform(post("/api/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": "room-1",
                                  "tenantIds": ["tenant-1"],
                                  "startDate": "2026-06-01",
                                  "endDate": "2027-06-01",
                                  "monthlyRent": 2500000,
                                  "deposit": 2500000,
                                  "paymentDueDay": 32
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void terminateContractReturnsTerminatedContract() throws Exception {
        Contract contract = activeContract();
        contract.setStatus(ContractStatus.TERMINATED);

        when(contractService.terminateContract(eq("contract-1"), any())).thenReturn(contract);

        mockMvc.perform(patch("/api/contracts/contract-1/terminate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomStatus": "AVAILABLE",
                                  "note": "Ket thuc hop dong"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contract terminated successfully"))
                .andExpect(jsonPath("$.data.status").value("TERMINATED"));
    }

    @Test
    void renewContractReturnsRenewedContract() throws Exception {
        Contract contract = activeContract();
        contract.setEndDate(LocalDate.of(2028, 6, 1));
        when(contractService.renewContract(eq("contract-1"), any())).thenReturn(contract);

        mockMvc.perform(patch("/api/contracts/contract-1/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newEndDate": "2028-06-01",
                                  "monthlyRent": 3000000,
                                  "paymentDueDay": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Contract renewed successfully"))
                .andExpect(jsonPath("$.data.endDate").value("2028-06-01"));
    }

    private Contract activeContract() {
        return Contract.builder()
                .id("contract-1")
                .roomId("room-1")
                .tenantIds(List.of("tenant-1"))
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2027, 6, 1))
                .monthlyRent(2_500_000L)
                .deposit(2_500_000L)
                .paymentDueDay(5)
                .status(ContractStatus.ACTIVE)
                .build();
    }
}
