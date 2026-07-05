package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.dto.dashboard.DashboardDebtsResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRevenueResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRoomsStatusResponse;
import com.example.boardinghouse.dto.dashboard.DashboardSummaryResponse;
import com.example.boardinghouse.repository.InvoiceRepository;
import com.example.boardinghouse.repository.MaintenanceRepository;
import com.example.boardinghouse.repository.RoomRepository;
import com.example.boardinghouse.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<InvoiceStatus> DEBT_STATUSES = EnumSet.of(
            InvoiceStatus.UNPAID,
            InvoiceStatus.PARTIAL,
            InvoiceStatus.OVERDUE
    );

    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final CurrentUserService currentUserService;

    /**
     * Lấy dữ liệu tổng quan cho trang chủ (Dashboard).
     * Bao gồm: số lượng phòng (tổng, trống, đã thuê, bảo trì),
     * doanh thu dự kiến, đã thu, chưa thu trong tháng hiện tại,
     * số lượng hóa đơn nợ và yêu cầu bảo trì đang chờ xử lý.
     *
     * @return Dữ liệu tổng hợp DashboardSummaryResponse
     */
    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        String ownerId = currentUserService.getOwnerId();
        MonthlyRevenue monthlyRevenue = calculateMonthlyRevenue(ownerId, today.getMonthValue(), today.getYear());
        DashboardRoomsStatusResponse roomsStatus = getRoomsStatus(ownerId);

        return DashboardSummaryResponse.builder()
                .totalRooms(roomsStatus.getTotalRooms())
                .occupiedRooms(roomsStatus.getOccupiedRooms())
                .availableRooms(roomsStatus.getAvailableRooms())
                .maintenanceRooms(roomsStatus.getMaintenanceRooms())
                .monthlyExpectedRevenue(monthlyRevenue.expectedRevenue())
                .monthlyPaidRevenue(monthlyRevenue.paidRevenue())
                .monthlyUnpaidRevenue(monthlyRevenue.unpaidRevenue())
                .unpaidInvoices(countDebtInvoices())
                .pendingMaintenanceRequests(countPendingMaintenanceRequests())
                .build();
    }

    /**
     * Lấy thông tin chi tiết về doanh thu của một tháng/năm cụ thể.
     * Nếu không truyền tháng/năm, mặc định sẽ lấy tháng hiện tại.
     *
     * @param month Tháng cần xem
     * @param year Năm cần xem
     * @return Dữ liệu doanh thu DashboardRevenueResponse
     */
    public DashboardRevenueResponse getRevenue(Integer month, Integer year) {
        Period period = resolvePeriod(month, year);
        MonthlyRevenue revenue = calculateMonthlyRevenue(currentUserService.getOwnerId(), period.month(), period.year());

        return DashboardRevenueResponse.builder()
                .month(period.month())
                .year(period.year())
                .expectedRevenue(revenue.expectedRevenue())
                .paidRevenue(revenue.paidRevenue())
                .unpaidRevenue(revenue.unpaidRevenue())
                .invoiceCount(revenue.invoiceCount())
                .paidInvoices(revenue.paidInvoices())
                .unpaidInvoices(revenue.unpaidInvoices())
                .build();
    }

    /**
     * Lấy danh sách các khoản nợ (hóa đơn chưa thanh toán, thanh toán một phần hoặc quá hạn).
     *
     * @return Tổng số tiền nợ và danh sách hóa đơn nợ
     */
    public DashboardDebtsResponse getDebts() {
        String ownerId = currentUserService.getOwnerId();
        List<Invoice> debtInvoices = invoiceRepository.findByOwnerIdAndStatusIn(ownerId, DEBT_STATUSES);
        Long totalDebt = debtInvoices.stream()
                .mapToLong(invoice -> valueOrZero(invoice.getTotalAmount()))
                .sum();

        return DashboardDebtsResponse.builder()
                .totalDebt(totalDebt)
                .debtInvoiceCount(debtInvoices.size())
                .invoices(debtInvoices)
                .build();
    }

    /**
     * Thống kê trạng thái của tất cả các phòng trong hệ thống.
     * Bao gồm số lượng phòng trống, đã có người, đã đặt cọc và đang bảo trì.
     *
     * @return Thống kê trạng thái phòng
     */
    public DashboardRoomsStatusResponse getRoomsStatus() {
        return getRoomsStatus(currentUserService.getOwnerId());
    }

    private DashboardRoomsStatusResponse getRoomsStatus(String ownerId) {
        return DashboardRoomsStatusResponse.builder()
                .totalRooms(roomRepository.countByOwnerId(ownerId))
                .availableRooms(roomRepository.countByOwnerIdAndStatus(ownerId, RoomStatus.AVAILABLE))
                .occupiedRooms(roomRepository.countByOwnerIdAndStatus(ownerId, RoomStatus.OCCUPIED))
                .reservedRooms(roomRepository.countByOwnerIdAndStatus(ownerId, RoomStatus.RESERVED))
                .maintenanceRooms(roomRepository.countByOwnerIdAndStatus(ownerId, RoomStatus.MAINTENANCE))
                .build();
    }

    /**
     * Tính toán doanh thu trong một tháng/năm cụ thể dựa trên danh sách hóa đơn.
     *
     * @param month Tháng
     * @param year Năm
     * @return Dữ liệu doanh thu hàng tháng (MonthlyRevenue)
     */
    private MonthlyRevenue calculateMonthlyRevenue(String ownerId, Integer month, Integer year) {
        List<Invoice> invoices = invoiceRepository.findByOwnerIdAndMonthAndYear(ownerId, month, year).stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.CANCELLED)
                .toList();

        long expectedRevenue = invoices.stream()
                .mapToLong(invoice -> valueOrZero(invoice.getTotalAmount()))
                .sum();
        long paidRevenue = invoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID)
                .mapToLong(invoice -> valueOrZero(invoice.getTotalAmount()))
                .sum();
        long unpaidRevenue = invoices.stream()
                .filter(invoice -> DEBT_STATUSES.contains(invoice.getStatus()))
                .mapToLong(invoice -> valueOrZero(invoice.getTotalAmount()))
                .sum();
        long paidInvoices = invoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID)
                .count();
        long unpaidInvoices = invoices.stream()
                .filter(invoice -> DEBT_STATUSES.contains(invoice.getStatus()))
                .count();

        return new MonthlyRevenue(
                expectedRevenue,
                paidRevenue,
                unpaidRevenue,
                invoices.size(),
                paidInvoices,
                unpaidInvoices
        );
    }

    /**
     * Đếm tổng số lượng hóa đơn đang ở trạng thái nợ.
     */
    private long countDebtInvoices() {
        return invoiceRepository.countByOwnerIdAndStatusIn(currentUserService.getOwnerId(), DEBT_STATUSES);
    }

    /**
     * Đếm số lượng yêu cầu bảo trì đang ở trạng thái chờ xử lý (PENDING).
     */
    private long countPendingMaintenanceRequests() {
        return maintenanceRepository.countByOwnerIdAndStatus(currentUserService.getOwnerId(), MaintenanceStatus.PENDING);
    }

    /**
     * Xử lý và xác thực thời gian (tháng/năm). Nếu null sẽ lấy thời gian hiện tại.
     */
    private Period resolvePeriod(Integer month, Integer year) {
        LocalDate today = LocalDate.now();
        int resolvedMonth = month == null ? today.getMonthValue() : month;
        int resolvedYear = year == null ? today.getYear() : year;

        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BadRequestException("Month must be from 1 to 12");
        }

        if (resolvedYear < 2000 || resolvedYear > 2100) {
            throw new BadRequestException("Year must be valid");
        }

        return new Period(resolvedMonth, resolvedYear);
    }

    /**
     * Hàm hỗ trợ chuyển đổi giá trị null thành 0L để tránh lỗi NullPointerException khi tính toán.
     */
    private Long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private record MonthlyRevenue(
            Long expectedRevenue,
            Long paidRevenue,
            Long unpaidRevenue,
            long invoiceCount,
            long paidInvoices,
            long unpaidInvoices
    ) {
    }

    private record Period(Integer month, Integer year) {
    }
}
