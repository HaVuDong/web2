package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.common.util.SoftDeleteHelper;
import com.example.boardinghouse.domain.entity.MeterReading;
import com.example.boardinghouse.dto.meterreading.CreateMeterReadingRequest;
import com.example.boardinghouse.dto.meterreading.UpdateMeterReadingRequest;
import com.example.boardinghouse.repository.MeterReadingRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.security.CurrentUserService;
import com.example.boardinghouse.realtime.RealtimeEventPublisher;
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
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    /**
     * Lấy danh sách toàn bộ chỉ số điện nước của tất cả các phòng.
     *
     * @return Danh sách chỉ số điện nước
     */
    public List<MeterReading> getAllMeterReadings() {
        return meterReadingRepository.findByOwnerId(currentUserService.getOwnerId());
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
        String ownerId = currentUserService.getOwnerId();
        ensureRoomExists(request.getRoomId(), ownerId);
        validateReadingValues(
                request.getElectricityOld(),
                request.getElectricityNew(),
                request.getWaterOld(),
                request.getWaterNew()
        );
        ensureNoDuplicate(request.getRoomId(), ownerId, request.getMonth(), request.getYear(), null);

        MeterReading meterReading = MeterReading.builder()
                .ownerId(ownerId)
                .roomId(request.getRoomId())
                .month(request.getMonth())
                .year(request.getYear())
                .electricityOld(request.getElectricityOld())
                .electricityNew(request.getElectricityNew())
                .waterOld(request.getWaterOld())
                .waterNew(request.getWaterNew())
                .note(request.getNote())
                .build();

        MeterReading saved = meterReadingRepository.save(meterReading);
        auditService.log("CREATE", "METER_READING", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Lấy lịch sử ghi chỉ số điện nước của một phòng cụ thể, sắp xếp giảm dần theo thời gian.
     */
    public List<MeterReading> getMeterReadingsByRoomId(String roomId) {
        String ownerId = currentUserService.getOwnerId();
        ensureRoomExists(roomId, ownerId);
        return meterReadingRepository.findByRoomIdAndOwnerIdOrderByYearDescMonthDesc(roomId, ownerId);
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
        ensureNoInvoiceExists(meterReading);
        validateReadingValues(
                request.getElectricityOld(),
                request.getElectricityNew(),
                request.getWaterOld(),
                request.getWaterNew()
        );
        ensureNoDuplicate(meterReading.getRoomId(), meterReading.getOwnerId(), request.getMonth(), request.getYear(), id);

        meterReading.setMonth(request.getMonth());
        meterReading.setYear(request.getYear());
        meterReading.setElectricityOld(request.getElectricityOld());
        meterReading.setElectricityNew(request.getElectricityNew());
        meterReading.setWaterOld(request.getWaterOld());
        meterReading.setWaterNew(request.getWaterNew());
        meterReading.setNote(request.getNote());

        MeterReading saved = meterReadingRepository.save(meterReading);
        auditService.log("UPDATE", "METER_READING", saved.getId(), null, saved);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Xóa bản ghi chỉ số điện nước. Không cho phép xóa nếu hóa đơn đã thanh toán.
     */
    public void deleteMeterReading(String id) {
        MeterReading meterReading = getMeterReadingById(id);
        ensureNoInvoiceExists(meterReading);
        SoftDeleteHelper.markDeleted(meterReading, currentUserService.getOwnerId());
        meterReadingRepository.save(meterReading);
        auditService.log("SOFT_DELETE", "METER_READING", meterReading.getId(), null, meterReading);
        realtimeEventPublisher.publishGlobalUpdate();
    }

    /**
     * Lấy thông tin chỉ số điện nước theo ID.
     */
    private MeterReading getMeterReadingById(String id) {
        return meterReadingRepository.findByIdAndOwnerId(id, currentUserService.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found with id: " + id));
    }

    /**
     * Đảm bảo phòng phải tồn tại trong hệ thống.
     */
    private void ensureRoomExists(String roomId, String ownerId) {
        if (roomRepository.findByIdAndOwnerId(roomId, ownerId).isEmpty()) {
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
    private void ensureNoDuplicate(String roomId, String ownerId, Integer month, Integer year, String currentReadingId) {
        meterReadingRepository.findByRoomIdAndOwnerIdAndMonthAndYear(roomId, ownerId, month, year)
                .filter(existing -> currentReadingId == null || !existing.getId().equals(currentReadingId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Meter reading already exists for this room and month");
                });
    }

    /**
     * Đảm bảo KHÔNG có bất kỳ hóa đơn nào (dù đã thanh toán hay chưa) tồn tại cho tháng này của phòng.
     * Nếu đã xuất hóa đơn, không được phép sửa hay xóa số điện nước,
     * vì điều này sẽ làm sai lệch dữ liệu (tiền điện nước trên hóa đơn đã được tính trước đó).
     */
    private void ensureNoInvoiceExists(MeterReading meterReading) {
        long invoiceCount = mongoTemplate.count(
                Query.query(Criteria.where("roomId").is(meterReading.getRoomId())
                        .and("ownerId").is(meterReading.getOwnerId())
                        .and("month").is(meterReading.getMonth())
                        .and("year").is(meterReading.getYear())
                        .and("deleted").ne(true)),
                "invoices"
        );

        if (invoiceCount > 0) {
            throw new BadRequestException("Cannot edit or delete meter reading because an invoice already exists for this month. Please delete the invoice first.");
        }
    }
}
