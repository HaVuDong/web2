package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.MeterReading;
import com.example.boardinghouse.dto.meterreading.CreateMeterReadingRequest;
import com.example.boardinghouse.dto.meterreading.UpdateMeterReadingRequest;
import com.example.boardinghouse.repository.MeterReadingRepository;
import com.example.boardinghouse.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final RoomRepository roomRepository;
    private final MongoTemplate mongoTemplate;

    public List<MeterReading> getAllMeterReadings() {
        return meterReadingRepository.findAll();
    }

    public MeterReading createMeterReading(CreateMeterReadingRequest request) {
        ensureRoomExists(request.getRoomId());
        validateReadingValues(
                request.getElectricityOld(),
                request.getElectricityNew(),
                request.getWaterOld(),
                request.getWaterNew()
        );
        ensureNoDuplicate(request.getRoomId(), request.getMonth(), request.getYear(), null);

        MeterReading meterReading = MeterReading.builder()
                .roomId(request.getRoomId())
                .month(request.getMonth())
                .year(request.getYear())
                .electricityOld(request.getElectricityOld())
                .electricityNew(request.getElectricityNew())
                .waterOld(request.getWaterOld())
                .waterNew(request.getWaterNew())
                .note(request.getNote())
                .build();

        return meterReadingRepository.save(meterReading);
    }

    public List<MeterReading> getMeterReadingsByRoomId(String roomId) {
        ensureRoomExists(roomId);
        return meterReadingRepository.findByRoomIdOrderByYearDescMonthDesc(roomId);
    }

    public MeterReading getLatestMeterReading(String roomId) {
        List<MeterReading> meterReadings = getMeterReadingsByRoomId(roomId);
        if (meterReadings.isEmpty()) {
            throw new ResourceNotFoundException("No meter reading found for room id: " + roomId);
        }

        return meterReadings.get(0);
    }

    public MeterReading updateMeterReading(String id, UpdateMeterReadingRequest request) {
        MeterReading meterReading = getMeterReadingById(id);
        ensureNoPaidInvoice(meterReading);
        validateReadingValues(
                request.getElectricityOld(),
                request.getElectricityNew(),
                request.getWaterOld(),
                request.getWaterNew()
        );
        ensureNoDuplicate(meterReading.getRoomId(), request.getMonth(), request.getYear(), id);

        meterReading.setMonth(request.getMonth());
        meterReading.setYear(request.getYear());
        meterReading.setElectricityOld(request.getElectricityOld());
        meterReading.setElectricityNew(request.getElectricityNew());
        meterReading.setWaterOld(request.getWaterOld());
        meterReading.setWaterNew(request.getWaterNew());
        meterReading.setNote(request.getNote());

        return meterReadingRepository.save(meterReading);
    }

    public void deleteMeterReading(String id) {
        MeterReading meterReading = getMeterReadingById(id);
        ensureNoPaidInvoice(meterReading);
        meterReadingRepository.delete(meterReading);
    }

    private MeterReading getMeterReadingById(String id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }

    private void ensureRoomExists(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }
    }

    private void validateReadingValues(Long electricityOld, Long electricityNew, Long waterOld, Long waterNew) {
        if (electricityNew < electricityOld) {
            throw new BadRequestException("New electricity reading must be greater than or equal to old reading");
        }

        if (waterNew < waterOld) {
            throw new BadRequestException("New water reading must be greater than or equal to old reading");
        }
    }

    private void ensureNoDuplicate(String roomId, Integer month, Integer year, String currentReadingId) {
        meterReadingRepository.findByRoomIdAndMonthAndYear(roomId, month, year)
                .filter(existing -> currentReadingId == null || !existing.getId().equals(currentReadingId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Meter reading already exists for this room and month");
                });
    }

    private void ensureNoPaidInvoice(MeterReading meterReading) {
        long paidInvoiceCount = mongoTemplate.count(
                Query.query(Criteria.where("roomId").is(meterReading.getRoomId())
                        .and("month").is(meterReading.getMonth())
                        .and("year").is(meterReading.getYear())
                        .and("status").is("PAID")),
                "invoices"
        );

        if (paidInvoiceCount > 0) {
            throw new BadRequestException("Cannot edit or delete meter reading because invoice is already paid");
        }
    }
}
