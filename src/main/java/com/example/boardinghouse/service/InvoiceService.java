package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.entity.MeterReading;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.entity.ServicePrice;
import com.example.boardinghouse.domain.enums.ContractStatus;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.dto.invoice.GenerateInvoiceRequest;
import com.example.boardinghouse.dto.invoice.GenerateMonthlyInvoiceRequest;
import com.example.boardinghouse.dto.invoice.MonthlyInvoiceGenerationResponse;
import com.example.boardinghouse.dto.invoice.UpdateInvoiceRequest;
import com.example.boardinghouse.repository.ContractRepository;
import com.example.boardinghouse.repository.InvoiceRepository;
import com.example.boardinghouse.repository.MeterReadingRepository;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.repository.ServicePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final ServicePriceRepository servicePriceRepository;
    private final PropertyRepository propertyRepository;

    /**
     * Lấy danh sách toàn bộ hóa đơn.
     *
     * @return Danh sách hóa đơn
     */
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    /**
     * Tạo một hóa đơn lẻ cho một phòng cụ thể.
     *
     * @param request Dữ liệu đầu vào để tạo hóa đơn
     * @return Hóa đơn vừa tạo
     */
    public Invoice generateInvoice(GenerateInvoiceRequest request) {
        return createInvoice(
                request.getRoomId(),
                request.getMonth(),
                request.getYear(),
                valueOrZero(request.getOtherFees()),
                valueOrZero(request.getDiscountAmount()),
                request.getNote()
        );
    }

    /**
     * Tạo hóa đơn hàng tháng tự động cho tất cả các phòng đang có người ở trong một tòa nhà.
     * Nếu phòng nào thiếu dữ liệu (ví dụ: chưa chốt điện nước), sẽ được bỏ qua và ghi nhận lỗi.
     *
     * @param request Yêu cầu tạo hóa đơn hàng loạt
     * @return Kết quả tổng hợp các hóa đơn tạo thành công và danh sách lỗi
     */
    public MonthlyInvoiceGenerationResponse generateMonthlyInvoices(GenerateMonthlyInvoiceRequest request) {
        if (!propertyRepository.existsById(request.getPropertyId())) {
            throw new ResourceNotFoundException("Property not found with id: " + request.getPropertyId());
        }

        List<Invoice> createdInvoices = new ArrayList<>();
        List<String> skippedRooms = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        roomRepository.findByPropertyId(request.getPropertyId()).stream()
                .filter(room -> room.getStatus() == RoomStatus.OCCUPIED)
                .forEach(room -> {
                    try {
                        Invoice invoice = createInvoice(room.getId(), request.getMonth(), request.getYear(), 0L, 0L, null);
                        createdInvoices.add(invoice);
                    } catch (BadRequestException | ResourceNotFoundException ex) {
                        skippedRooms.add(room.getId());
                        errors.add(room.getId() + ": " + ex.getMessage());
                    }
                });

        return MonthlyInvoiceGenerationResponse.builder()
                .createdInvoices(createdInvoices)
                .skippedRooms(skippedRooms)
                .errors(errors)
                .build();
    }

    /**
     * Lấy thông tin hóa đơn theo ID. Ném ngoại lệ nếu không tìm thấy.
     */
    public Invoice getInvoiceById(String id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    /**
     * Cập nhật thông tin hóa đơn (phí khác, giảm giá, hạn chót, ghi chú).
     * Không cho phép cập nhật nếu hóa đơn đã thanh toán hoặc đã bị hủy.
     * Sau khi cập nhật, tự động tính toán lại tổng tiền.
     *
     * @param id ID hóa đơn
     * @param request Dữ liệu cập nhật
     * @return Hóa đơn sau khi cập nhật
     */
    public Invoice updateInvoice(String id, UpdateInvoiceRequest request) {
        Invoice invoice = getInvoiceById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Paid invoice cannot be edited");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cancelled invoice cannot be edited");
        }

        invoice.setOtherFees(request.getOtherFees() == null ? invoice.getOtherFees() : request.getOtherFees());
        invoice.setDiscountAmount(request.getDiscountAmount() == null ? invoice.getDiscountAmount() : request.getDiscountAmount());
        invoice.setDueDate(request.getDueDate() == null ? invoice.getDueDate() : request.getDueDate());
        invoice.setNote(request.getNote() == null ? invoice.getNote() : request.getNote());
        invoice.setTotalAmount(calculateTotalAmount(invoice));

        return invoiceRepository.save(invoice);
    }

    /**
     * Xóa một hóa đơn. Không cho phép xóa hóa đơn đã thanh toán.
     */
    public void deleteInvoice(String id) {
        Invoice invoice = getInvoiceById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Paid invoice cannot be deleted");
        }

        invoiceRepository.delete(invoice);
    }

    /**
     * Đánh dấu hóa đơn đã được thanh toán (PAID).
     * Cập nhật thời gian thanh toán là thời điểm hiện tại.
     */
    public Invoice markPaid(String id) {
        Invoice invoice = getInvoiceById(id);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BadRequestException("Cancelled invoice cannot be marked as paid");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    /**
     * Hủy bỏ một hóa đơn (CANCELLED). Không thể hủy hóa đơn đã thanh toán.
     */
    public Invoice cancelInvoice(String id) {
        Invoice invoice = getInvoiceById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BadRequestException("Paid invoice cannot be cancelled");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoiceRepository.save(invoice);
    }

    /**
     * Lấy danh sách hóa đơn của một phòng cụ thể.
     */
    public List<Invoice> getInvoicesByRoomId(String roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }

        return invoiceRepository.findByRoomId(roomId);
    }

    /**
     * Logic lõi để tạo hóa đơn. 
     * Lấy thông tin hợp đồng, bảng giá dịch vụ, và số điện nước đã chốt để tính toán thành tiền.
     * Đảm bảo không tạo trùng hóa đơn trong cùng một tháng.
     */
    private Invoice createInvoice(String roomId, Integer month, Integer year, Long otherFees, Long discountAmount, String note) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        Contract contract = contractRepository.findByRoomIdAndStatus(roomId, ContractStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active contract not found for room id: " + roomId));
        ServicePrice servicePrice = servicePriceRepository.findByPropertyId(room.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Service price not found for property id: " + room.getPropertyId()));
        MeterReading meterReading = meterReadingRepository.findByRoomIdAndMonthAndYear(roomId, month, year)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found for room and month"));

        invoiceRepository.findByRoomIdAndMonthAndYear(roomId, month, year)
                .ifPresent(invoice -> {
                    throw new BadRequestException("Invoice already exists for this room and month");
                });

        Long electricityUsage = meterReading.getElectricityNew() - meterReading.getElectricityOld();
        Long waterUsage = meterReading.getWaterNew() - meterReading.getWaterOld();
        Long electricityAmount = electricityUsage * servicePrice.getElectricityPrice();
        Long waterAmount = waterUsage * servicePrice.getWaterPrice();

        Invoice invoice = Invoice.builder()
                .roomId(roomId)
                .contractId(contract.getId())
                .month(month)
                .year(year)
                .rentAmount(contract.getMonthlyRent())
                .electricityOld(meterReading.getElectricityOld())
                .electricityNew(meterReading.getElectricityNew())
                .electricityUsage(electricityUsage)
                .electricityPrice(servicePrice.getElectricityPrice())
                .electricityAmount(electricityAmount)
                .waterOld(meterReading.getWaterOld())
                .waterNew(meterReading.getWaterNew())
                .waterUsage(waterUsage)
                .waterPrice(servicePrice.getWaterPrice())
                .waterAmount(waterAmount)
                .wifiFee(servicePrice.getWifiFee())
                .garbageFee(servicePrice.getGarbageFee())
                .parkingFee(servicePrice.getParkingFee())
                .otherFees(otherFees)
                .discountAmount(discountAmount)
                .status(InvoiceStatus.UNPAID)
                .dueDate(resolveDueDate(month, year, contract.getPaymentDueDay()))
                .note(note)
                .build();
        invoice.setTotalAmount(calculateTotalAmount(invoice));

        return invoiceRepository.save(invoice);
    }

    /**
     * Tính tổng số tiền của hóa đơn (tiền phòng + dịch vụ - giảm giá).
     */
    private Long calculateTotalAmount(Invoice invoice) {
        return valueOrZero(invoice.getRentAmount())
                + valueOrZero(invoice.getElectricityAmount())
                + valueOrZero(invoice.getWaterAmount())
                + valueOrZero(invoice.getWifiFee())
                + valueOrZero(invoice.getGarbageFee())
                + valueOrZero(invoice.getParkingFee())
                + valueOrZero(invoice.getOtherFees())
                - valueOrZero(invoice.getDiscountAmount());
    }

    /**
     * Xác định ngày hạn chót thanh toán của hóa đơn.
     */
    private java.time.LocalDate resolveDueDate(Integer month, Integer year, Integer paymentDueDay) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int dueDay = paymentDueDay == null ? 1 : Math.min(paymentDueDay, yearMonth.lengthOfMonth());
        return yearMonth.atDay(dueDay);
    }

    /**
     * Tránh lỗi NullPointerException khi tính toán số tiền.
     */
    private Long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
