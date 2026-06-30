package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
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
import com.example.boardinghouse.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private MeterReadingRepository meterReadingRepository;

    @Mock
    private ServicePriceRepository servicePriceRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private InvoiceService invoiceService;

    @BeforeEach
    void setUpOwner() {
        lenient().when(currentUserService.getOwnerId()).thenReturn("owner-1");
    }

    @Test
    void generateInvoiceCalculatesTotalAndSnapshotsPrices() {
        ServicePrice servicePrice = servicePrice();
        when(roomRepository.findByIdAndOwnerId("room-1", "owner-1")).thenReturn(Optional.of(room("room-1", RoomStatus.OCCUPIED)));
        when(contractRepository.findByRoomIdAndOwnerIdAndStatus("room-1", "owner-1", ContractStatus.ACTIVE)).thenReturn(Optional.of(contract()));
        when(servicePriceRepository.findByPropertyIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(servicePrice));
        when(meterReadingRepository.findByRoomIdAndOwnerIdAndMonthAndYear("room-1", "owner-1", 6, 2026)).thenReturn(Optional.of(meterReading()));
        when(invoiceRepository.findByRoomIdAndOwnerIdAndMonthAndYear("room-1", "owner-1", 6, 2026)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice invoice = invoiceService.generateInvoice(generateRequest(25_000L, 10_000L));
        servicePrice.setElectricityPrice(4000L);

        assertThat(invoice.getElectricityUsage()).isEqualTo(20L);
        assertThat(invoice.getWaterUsage()).isEqualTo(10L);
        assertThat(invoice.getElectricityPrice()).isEqualTo(3500L);
        assertThat(invoice.getElectricityAmount()).isEqualTo(70_000L);
        assertThat(invoice.getWaterAmount()).isEqualTo(150_000L);
        assertThat(invoice.getTotalAmount()).isEqualTo(3_035_000L);
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
    }

    @Test
    void generateInvoiceRejectsDuplicateInvoice() {
        when(roomRepository.findByIdAndOwnerId("room-1", "owner-1")).thenReturn(Optional.of(room("room-1", RoomStatus.OCCUPIED)));
        when(contractRepository.findByRoomIdAndOwnerIdAndStatus("room-1", "owner-1", ContractStatus.ACTIVE)).thenReturn(Optional.of(contract()));
        when(servicePriceRepository.findByPropertyIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(servicePrice()));
        when(meterReadingRepository.findByRoomIdAndOwnerIdAndMonthAndYear("room-1", "owner-1", 6, 2026)).thenReturn(Optional.of(meterReading()));
        when(invoiceRepository.findByRoomIdAndOwnerIdAndMonthAndYear("room-1", "owner-1", 6, 2026))
                .thenReturn(Optional.of(Invoice.builder().id("invoice-1").build()));

        assertThatThrownBy(() -> invoiceService.generateInvoice(generateRequest(0L, 0L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invoice already exists for this room and month");
    }

    @Test
    void generateMonthlyInvoicesCreatesForOccupiedRoomsAndSkipsMissingData() {
        Room room1 = room("room-1", RoomStatus.OCCUPIED);
        Room room2 = room("room-2", RoomStatus.OCCUPIED);
        when(propertyRepository.findByIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(new com.example.boardinghouse.domain.entity.Property()));
        when(roomRepository.findByPropertyIdAndOwnerId("property-1", "owner-1")).thenReturn(List.of(room1, room2, room("room-3", RoomStatus.AVAILABLE)));
        when(roomRepository.findByIdAndOwnerId("room-1", "owner-1")).thenReturn(Optional.of(room1));
        when(roomRepository.findByIdAndOwnerId("room-2", "owner-1")).thenReturn(Optional.of(room2));
        when(contractRepository.findByRoomIdAndOwnerIdAndStatus("room-1", "owner-1", ContractStatus.ACTIVE)).thenReturn(Optional.of(contract()));
        when(contractRepository.findByRoomIdAndOwnerIdAndStatus("room-2", "owner-1", ContractStatus.ACTIVE)).thenReturn(Optional.empty());
        when(servicePriceRepository.findByPropertyIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(servicePrice()));
        when(meterReadingRepository.findByRoomIdAndOwnerIdAndMonthAndYear("room-1", "owner-1", 6, 2026)).thenReturn(Optional.of(meterReading()));
        when(invoiceRepository.findByRoomIdAndOwnerIdAndMonthAndYear("room-1", "owner-1", 6, 2026)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateMonthlyInvoiceRequest request = new GenerateMonthlyInvoiceRequest();
        request.setPropertyId("property-1");
        request.setMonth(6);
        request.setYear(2026);

        MonthlyInvoiceGenerationResponse response = invoiceService.generateMonthlyInvoices(request);

        assertThat(response.getCreatedInvoices()).hasSize(1);
        assertThat(response.getSkippedRooms()).containsExactly("room-2");
        assertThat(response.getErrors().get(0)).contains("Active contract not found");
    }

    @Test
    void updateInvoiceRejectsPaidInvoice() {
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .ownerId("owner-1")
                .status(InvoiceStatus.PAID)
                .build();
        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setOtherFees(10_000L);

        when(invoiceRepository.findByIdAndOwnerId("invoice-1", "owner-1")).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.updateInvoice("invoice-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Paid invoice cannot be edited");
    }

    @Test
    void cancelInvoiceRejectsPaidInvoice() {
        Invoice invoice = Invoice.builder()
                .id("invoice-1")
                .ownerId("owner-1")
                .status(InvoiceStatus.PAID)
                .build();
        when(invoiceRepository.findByIdAndOwnerId("invoice-1", "owner-1")).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice("invoice-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Paid invoice cannot be cancelled");
    }

    private GenerateInvoiceRequest generateRequest(Long otherFees, Long discountAmount) {
        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setRoomId("room-1");
        request.setMonth(6);
        request.setYear(2026);
        request.setOtherFees(otherFees);
        request.setDiscountAmount(discountAmount);
        return request;
    }

    private Room room(String id, RoomStatus status) {
        return Room.builder()
                .id(id)
                .ownerId("owner-1")
                .propertyId("property-1")
                .status(status)
                .build();
    }

    private Contract contract() {
        return Contract.builder()
                .id("contract-1")
                .ownerId("owner-1")
                .roomId("room-1")
                .monthlyRent(2_500_000L)
                .paymentDueDay(5)
                .status(ContractStatus.ACTIVE)
                .build();
    }

    private MeterReading meterReading() {
        return MeterReading.builder()
                .id("meter-1")
                .ownerId("owner-1")
                .roomId("room-1")
                .month(6)
                .year(2026)
                .electricityOld(100L)
                .electricityNew(120L)
                .waterOld(50L)
                .waterNew(60L)
                .build();
    }

    private ServicePrice servicePrice() {
        return ServicePrice.builder()
                .id("service-price-1")
                .ownerId("owner-1")
                .propertyId("property-1")
                .electricityPrice(3500L)
                .waterPrice(15000L)
                .wifiFee(100_000L)
                .garbageFee(50_000L)
                .parkingFee(150_000L)
                .build();
    }
}
