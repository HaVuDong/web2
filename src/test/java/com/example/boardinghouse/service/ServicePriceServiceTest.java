package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.ServicePrice;
import com.example.boardinghouse.dto.serviceprice.UpdateServicePriceRequest;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.repository.ServicePriceRepository;
import com.example.boardinghouse.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ServicePriceServiceTest {

    @Mock
    private ServicePriceRepository servicePriceRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ServicePriceService servicePriceService;

    @BeforeEach
    void setUpOwner() {
        lenient().when(currentUserService.getOwnerId()).thenReturn("owner-1");
    }

    @Test
    void getServicePriceCreatesDefaultWhenMissing() {
        when(propertyRepository.findByIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(new com.example.boardinghouse.domain.entity.Property()));
        when(servicePriceRepository.findByPropertyIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.empty());
        when(servicePriceRepository.save(any(ServicePrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServicePrice servicePrice = servicePriceService.getServicePrice("property-1");

        assertThat(servicePrice.getElectricityPrice()).isEqualTo(3500L);
        assertThat(servicePrice.getWaterPrice()).isEqualTo(15000L);
        assertThat(servicePrice.getWifiFee()).isZero();
        assertThat(servicePrice.getPropertyId()).isEqualTo("property-1");
        assertThat(servicePrice.getOwnerId()).isEqualTo("owner-1");
    }

    @Test
    void updateServicePriceUpdatesExistingConfig() {
        ServicePrice existingServicePrice = ServicePrice.builder()
                .id("service-price-1")
                .ownerId("owner-1")
                .propertyId("property-1")
                .electricityPrice(3500L)
                .waterPrice(15000L)
                .wifiFee(0L)
                .garbageFee(0L)
                .parkingFee(0L)
                .build();

        when(propertyRepository.findByIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(new com.example.boardinghouse.domain.entity.Property()));
        when(servicePriceRepository.findByPropertyIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(existingServicePrice));
        when(servicePriceRepository.save(any(ServicePrice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServicePrice servicePrice = servicePriceService.updateServicePrice("property-1", updateRequest());

        assertThat(servicePrice.getElectricityPrice()).isEqualTo(4000L);
        assertThat(servicePrice.getWaterPrice()).isEqualTo(20000L);
        assertThat(servicePrice.getWifiFee()).isEqualTo(100000L);
    }

    @Test
    void getServicePriceRejectsMissingProperty() {
        when(propertyRepository.findByIdAndOwnerId("missing", "owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicePriceService.getServicePrice("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Property not found with id: missing");

        verify(servicePriceRepository, never()).save(any(ServicePrice.class));
    }

    private UpdateServicePriceRequest updateRequest() {
        UpdateServicePriceRequest request = new UpdateServicePriceRequest();
        request.setElectricityPrice(4000L);
        request.setWaterPrice(20000L);
        request.setWifiFee(100000L);
        request.setGarbageFee(50000L);
        request.setParkingFee(150000L);
        return request;
    }
}
