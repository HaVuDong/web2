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

    /**
     * Lấy danh sách toàn bộ chỉ số điện nước của tất cả các phòng.
     *
     * @return Danh sách chỉ số điện nước
     */
    public List<MeterReading> getAllMeterReadings() {
        return meterReadingRepository.findAll();
    }

    /**
     * Tạo mới bản ghi chỉ số điện nước (chốt điện nước).
     * Kiểm tra phòng, số điện/nước mới phải lớn hơn hoặc bằng số cũ.
     * Đảm bảo không ghi trùng chỉ số cho cùng 1 phòng trong cùng 1 tháng.
     *
     * @param request Dữ liệu chỉ số
     * @return Bản ghi vừa tạo
     */
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

    /**
     * Lấy lịch sử ghi chỉ số điện nước của một phòng cụ thể, sắp xếp giảm dần theo thời gian.
     */
    public List<MeterReading> getMeterReadingsByRoomId(String roomId) {
        ensureRoomExists(roomId);
        return meterReadingRepository.findByRoomIdOrderByYearDescMonthDesc(roomId);
    }

    /**
     * Lấy chỉ số điện nước gần nhất (mới nhất) của một phòng.
     * Dùng để gợi ý số cũ cho lần chốt điện nước tiếp theo.
     */
    public MeterReading getLatestMeterReading(String roomId) {
        List<MeterReading> meterReadings = getMeterReadingsByRoomId(roomId);
        if (meterReadings.isEmpty()) {
            throw new ResourceNotFoundException("No meter reading found for room id: " + roomId);
        }

        return meterReadings.get(0);
    }

    /**
     * Cập nhật thông tin chỉ số điện nước.
     * Không cho phép sửa nếu hóa đơn của tháng đó đã được thanh toán (PAID).
     */
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

    /**
     * Xóa bản ghi chỉ số điện nước. Không cho phép xóa nếu hóa đơn đã thanh toán.
     */
    public void deleteMeterReading(String id) {
        MeterReading meterReading = getMeterReadingById(id);
        ensureNoPaidInvoice(meterReading);
        meterReadingRepository.delete(meterReading);
    }

    /**
     * Lấy thông tin chỉ số điện nước theo ID.
     */
    private MeterReading getMeterReadingById(String id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }

    /**
     * Đảm bảo phòng phải tồn tại trong hệ thống.
     */
    private void ensureRoomExists(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }
    }

    /**
     * Xác thực số mới phải luôn >= số cũ (vì đồng hồ điện nước không chạy lùi).
     */
    private void validateReadingValues(Long electricityOld, Long electricityNew, Long waterOld, Long waterNew) {
        if (electricityNew < electricityOld) {
            throw new BadRequestException("New electricity reading must be greater than or equal to old reading");
        }

        if (waterNew < waterOld) {
            throw new BadRequestException("New water reading must be greater than or equal to old reading");
        }
    }

    /**
     * Đảm bảo không ghi trùng chỉ số 2 lần cho cùng một tháng của một phòng.
     */
    private void ensureNoDuplicate(String roomId, Integer month, Integer year, String currentReadingId) {
        meterReadingRepository.findByRoomIdAndMonthAndYear(roomId, month, year)
                .filter(existing -> currentReadingId == null || !existing.getId().equals(currentReadingId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Meter reading already exists for this room and month");
                });
    }

    /**
     * Đảm bảo hóa đơn của phòng và tháng tương ứng chưa được thanh toán (trạng thái PAID).
     * Nếu đã thanh toán, không được phép sửa hay xóa số điện nước.
     */
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
