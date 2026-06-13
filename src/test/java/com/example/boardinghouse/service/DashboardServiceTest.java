package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.domain.entity.Invoice;
import com.example.boardinghouse.domain.enums.InvoiceStatus;
import com.example.boardinghouse.domain.enums.MaintenanceStatus;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.dto.dashboard.DashboardDebtsResponse;
import com.example.boardinghouse.dto.dashboard.DashboardRevenueResponse;
import com.example.boardinghouse.dto.dashboard.DashboardSummaryResponse;
import com.example.boardinghouse.repository.InvoiceRepository;
import com.example.boardinghouse.repository.MaintenanceRepository;
import com.example.boardinghouse.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummaryCalculatesRoomsRevenueDebtsAndMaintenance() {
        LocalDate today = LocalDate.now();
        when(roomRepository.count()).thenReturn(20L);
        when(roomRepository.countByStatus(RoomStatus.AVAILABLE)).thenReturn(4L);
        when(roomRepository.countByStatus(RoomStatus.OCCUPIED)).thenReturn(15L);
        when(roomRepository.countByStatus(RoomStatus.RESERVED)).thenReturn(0L);
        when(roomRepository.countByStatus(RoomStatus.MAINTENANCE)).thenReturn(1L);
        when(invoiceRepository.findByMonthAndYear(today.getMonthValue(), today.getYear()))
                .thenReturn(List.of(
                        invoice("invoice-1", InvoiceStatus.PAID, 100L),
                        invoice("invoice-2", InvoiceStatus.UNPAID, 200L),
                        invoice("invoice-3", InvoiceStatus.CANCELLED, 50L)
                ));
        when(invoiceRepository.findByStatus(InvoiceStatus.UNPAID))
                .thenReturn(List.of(invoice("invoice-4", InvoiceStatus.UNPAID, 300L)));
        when(invoiceRepository.findByStatus(InvoiceStatus.PARTIAL)).thenReturn(List.of());
        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE))
                .thenReturn(List.of(invoice("invoice-5", InvoiceStatus.OVERDUE, 400L)));
        when(maintenanceRepository.countByStatus(MaintenanceStatus.PENDING)).thenReturn(2L);

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.getTotalRooms()).isEqualTo(20L);
        assertThat(response.getOccupiedRooms()).isEqualTo(15L);
        assertThat(response.getAvailableRooms()).isEqualTo(4L);
        assertThat(response.getMaintenanceRooms()).isEqualTo(1L);
        assertThat(response.getMonthlyExpectedRevenue()).isEqualTo(300L);
        assertThat(response.getMonthlyPaidRevenue()).isEqualTo(100L);
        assertThat(response.getMonthlyUnpaidRevenue()).isEqualTo(200L);
        assertThat(response.getUnpaidInvoices()).isEqualTo(2L);
        assertThat(response.getPendingMaintenanceRequests()).isEqualTo(2L);
    }

    @Test
    void getRevenueCalculatesPaidAndUnpaidRevenue() {
        when(invoiceRepository.findByMonthAndYear(6, 2026))
                .thenReturn(List.of(
                        invoice("invoice-1", InvoiceStatus.PAID, 100L),
                        invoice("invoice-2", InvoiceStatus.UNPAID, 200L),
                        invoice("invoice-3", InvoiceStatus.OVERDUE, 300L),
                        invoice("invoice-4", InvoiceStatus.CANCELLED, 400L)
                ));

        DashboardRevenueResponse response = dashboardService.getRevenue(6, 2026);

        assertThat(response.getMonth()).isEqualTo(6);
        assertThat(response.getYear()).isEqualTo(2026);
        assertThat(response.getExpectedRevenue()).isEqualTo(600L);
        assertThat(response.getPaidRevenue()).isEqualTo(100L);
        assertThat(response.getUnpaidRevenue()).isEqualTo(500L);
        assertThat(response.getInvoiceCount()).isEqualTo(3L);
        assertThat(response.getPaidInvoices()).isEqualTo(1L);
        assertThat(response.getUnpaidInvoices()).isEqualTo(2L);
    }

    @Test
    void getDebtsReturnsOnlyDebtInvoices() {
        when(invoiceRepository.findAll()).thenReturn(List.of(
                invoice("invoice-1", InvoiceStatus.PAID, 100L),
                invoice("invoice-2", InvoiceStatus.UNPAID, 200L),
                invoice("invoice-3", InvoiceStatus.PARTIAL, 50L),
                invoice("invoice-4", InvoiceStatus.OVERDUE, 70L),
                invoice("invoice-5", InvoiceStatus.CANCELLED, 10L)
        ));

        DashboardDebtsResponse response = dashboardService.getDebts();

        assertThat(response.getDebtInvoiceCount()).isEqualTo(3L);
        assertThat(response.getTotalDebt()).isEqualTo(320L);
        assertThat(response.getInvoices())
                .extracting(Invoice::getId)
                .containsExactly("invoice-2", "invoice-3", "invoice-4");
    }

    @Test
    void getRevenueRejectsInvalidMonth() {
        assertThatThrownBy(() -> dashboardService.getRevenue(13, 2026))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Month must be from 1 to 12");
    }

    private Invoice invoice(String id, InvoiceStatus status, Long totalAmount) {
        return Invoice.builder()
                .id(id)
                .month(6)
                .year(2026)
                .status(status)
                .totalAmount(totalAmount)
                .build();
    }
}
