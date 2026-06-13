package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.MeterReading;
import com.example.boardinghouse.dto.meterreading.CreateMeterReadingRequest;
import com.example.boardinghouse.dto.meterreading.UpdateMeterReadingRequest;
import com.example.boardinghouse.repository.MeterReadingRepository;
import com.example.boardinghouse.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingServiceTest {

    @Mock
    private MeterReadingRepository meterReadingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MeterReadingService meterReadingService;

    @Test
    void createMeterReadingCreatesReading() {
        CreateMeterReadingRequest request = createRequest();
        when(roomRepository.existsById("room-1")).thenReturn(true);
        when(meterReadingRepository.findByRoomIdAndMonthAndYear("room-1", 6, 2026)).thenReturn(Optional.empty());
        when(meterReadingRepository.save(any(MeterReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeterReading meterReading = meterReadingService.createMeterReading(request);

        assertThat(meterReading.getRoomId()).isEqualTo("room-1");
        assertThat(meterReading.getElectricityNew()).isEqualTo(120L);
        assertThat(meterReading.getWaterNew()).isEqualTo(60L);
    }

    @Test
    void createMeterReadingRejectsInvalidElectricityReading() {
        CreateMeterReadingRequest request = createRequest();
        request.setElectricityNew(90L);

        when(roomRepository.existsById("room-1")).thenReturn(true);

        assertThatThrownBy(() -> meterReadingService.createMeterReading(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("New electricity reading must be greater than or equal to old reading");

        verify(meterReadingRepository, never()).save(any(MeterReading.class));
    }

    @Test
    void createMeterReadingRejectsDuplicateMonth() {
        CreateMeterReadingRequest request = createRequest();
        MeterReading existing = meterReading("meter-1", 6, 2026);

        when(roomRepository.existsById("room-1")).thenReturn(true);
        when(meterReadingRepository.findByRoomIdAndMonthAndYear("room-1", 6, 2026)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> meterReadingService.createMeterReading(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Meter reading already exists for this room and month");
    }

    @Test
    void getLatestMeterReadingReturnsNewestReading() {
        MeterReading latest = meterReading("meter-2", 7, 2026);
        MeterReading older = meterReading("meter-1", 6, 2026);

        when(roomRepository.existsById("room-1")).thenReturn(true);
        when(meterReadingRepository.findByRoomIdOrderByYearDescMonthDesc("room-1"))
                .thenReturn(List.of(latest, older));

        MeterReading result = meterReadingService.getLatestMeterReading("room-1");

        assertThat(result.getId()).isEqualTo("meter-2");
        assertThat(result.getMonth()).isEqualTo(7);
    }

    @Test
    void getLatestMeterReadingRejectsEmptyRoomReadings() {
        when(roomRepository.existsById("room-1")).thenReturn(true);
        when(meterReadingRepository.findByRoomIdOrderByYearDescMonthDesc("room-1")).thenReturn(List.of());

        assertThatThrownBy(() -> meterReadingService.getLatestMeterReading("room-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No meter reading found for room id: room-1");
    }

    @Test
    void updateMeterReadingRejectsPaidInvoice() {
        MeterReading existing = meterReading("meter-1", 6, 2026);
        UpdateMeterReadingRequest request = updateRequest();

        when(meterReadingRepository.findById("meter-1")).thenReturn(Optional.of(existing));
        when(mongoTemplate.count(any(Query.class), eq("invoices"))).thenReturn(1L);

        assertThatThrownBy(() -> meterReadingService.updateMeterReading("meter-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot edit or delete meter reading because invoice is already paid");
    }

    @Test
    void deleteMeterReadingDeletesWhenNoPaidInvoiceExists() {
        MeterReading existing = meterReading("meter-1", 6, 2026);

        when(meterReadingRepository.findById("meter-1")).thenReturn(Optional.of(existing));
        when(mongoTemplate.count(any(Query.class), eq("invoices"))).thenReturn(0L);

        meterReadingService.deleteMeterReading("meter-1");

        verify(meterReadingRepository).delete(existing);
    }

    private CreateMeterReadingRequest createRequest() {
        CreateMeterReadingRequest request = new CreateMeterReadingRequest();
        request.setRoomId("room-1");
        request.setMonth(6);
        request.setYear(2026);
        request.setElectricityOld(100L);
        request.setElectricityNew(120L);
        request.setWaterOld(50L);
        request.setWaterNew(60L);
        return request;
    }

    private UpdateMeterReadingRequest updateRequest() {
        UpdateMeterReadingRequest request = new UpdateMeterReadingRequest();
        request.setMonth(6);
        request.setYear(2026);
        request.setElectricityOld(100L);
        request.setElectricityNew(125L);
        request.setWaterOld(50L);
        request.setWaterNew(65L);
        return request;
    }

    private MeterReading meterReading(String id, Integer month, Integer year) {
        return MeterReading.builder()
                .id(id)
                .roomId("room-1")
                .month(month)
                .year(year)
                .electricityOld(100L)
                .electricityNew(120L)
                .waterOld(50L)
                .waterNew(60L)
                .build();
    }
}
