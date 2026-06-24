package com.example.boardinghouse.dto.contract;

import com.example.boardinghouse.domain.enums.RoomStatus;
import lombok.Data;

@Data
/**
 * DTO cho yêu cầu chấm dứt hợp đồng trước thời hạn.
 */
public class TerminateContractRequest {

    private RoomStatus roomStatus;

    private String note;
}
