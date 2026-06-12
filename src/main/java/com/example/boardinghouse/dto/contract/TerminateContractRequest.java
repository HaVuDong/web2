package com.example.boardinghouse.dto.contract;

import com.example.boardinghouse.domain.enums.RoomStatus;
import lombok.Data;

@Data
public class TerminateContractRequest {

    private RoomStatus roomStatus;

    private String note;
}
