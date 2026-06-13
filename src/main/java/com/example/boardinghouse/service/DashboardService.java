package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.dto.dashboard.DashboardDebtsResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRevenueResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRoomsStatusResponse;
import com.example.boardinghouse.dto.dashboard.DashboardSummaryResponse;
import com.example.boardinghouse.repository.InvoiceRepository;
import com.example.boardinghouse.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
    private final MongoTemplate mongoTemplate;

    public DashboardSummaryResponse getSummary() {
        LocalDate today = LocalDate.now();
        MonthlyRevenue monthlyRevenue = calculateMonthlyRevenue(today.getMonthValue(), today.getYear());
        DashboardRoomsStatusResponse roomsStatus = getRoomsStatus();

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

    public DashboardRevenueResponse getRevenue(Integer month, Integer year) {
        Period period = resolvePeriod(month, year);
        MonthlyRevenue revenue = calculateMonthlyRevenue(period.month(), period.year());

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

    public DashboardDebtsResponse getDebts() {
        List<Invoice> debtInvoices = invoiceRepository.findAll().stream()
                .filter(invoice -> DEBT_STATUSES.contains(invoice.getStatus()))
                .toList();
        Long totalDebt = debtInvoices.stream()
                .mapToLong(invoice -> valueOrZero(invoice.getTotalAmount()))
                .sum();

        return DashboardDebtsResponse.builder()
                .totalDebt(totalDebt)
                .debtInvoiceCount(debtInvoices.size())
                .invoices(debtInvoices)
                .build();
    }

    public DashboardRoomsStatusResponse getRoomsStatus() {
        return DashboardRoomsStatusResponse.builder()
                .totalRooms(roomRepository.count())
                .availableRooms(roomRepository.countByStatus(RoomStatus.AVAILABLE))
                .occupiedRooms(roomRepository.countByStatus(RoomStatus.OCCUPIED))
                .reservedRooms(roomRepository.countByStatus(RoomStatus.RESERVED))
                .maintenanceRooms(roomRepository.countByStatus(RoomStatus.MAINTENANCE))
                .build();
    }

    private MonthlyRevenue calculateMonthlyRevenue(Integer month, Integer year) {
        List<Invoice> invoices = invoiceRepository.findByMonthAndYear(month, year).stream()
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

    private long countDebtInvoices() {
        return DEBT_STATUSES.stream()
                .mapToLong(status -> invoiceRepository.findByStatus(status).size())
                .sum();
    }

    private long countPendingMaintenanceRequests() {
        Query query = Query.query(Criteria.where("status").is("PENDING"));
        return mongoTemplate.count(query, "maintenance_requests");
    }

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
