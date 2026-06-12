package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.ServicePrice;
import com.example.boardinghouse.dto.serviceprice.UpdateServicePriceRequest;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.repository.ServicePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServicePriceService {

    private static final long DEFAULT_ELECTRICITY_PRICE = 3500L;
    private static final long DEFAULT_WATER_PRICE = 15000L;
    private static final long DEFAULT_FEE = 0L;

    private final ServicePriceRepository servicePriceRepository;
    private final PropertyRepository propertyRepository;

    public ServicePrice getServicePrice(String propertyId) {
        ensurePropertyExists(propertyId);
        return servicePriceRepository.findByPropertyId(propertyId)
                .orElseGet(() -> servicePriceRepository.save(defaultServicePrice(propertyId)));
    }

    public ServicePrice updateServicePrice(String propertyId, UpdateServicePriceRequest request) {
        ensurePropertyExists(propertyId);
        ServicePrice servicePrice = servicePriceRepository.findByPropertyId(propertyId)
                .orElseGet(() -> defaultServicePrice(propertyId));

        servicePrice.setElectricityPrice(request.getElectricityPrice());
        servicePrice.setWaterPrice(request.getWaterPrice());
        servicePrice.setWifiFee(request.getWifiFee());
        servicePrice.setGarbageFee(request.getGarbageFee());
        servicePrice.setParkingFee(request.getParkingFee());

        return servicePriceRepository.save(servicePrice);
    }

    private ServicePrice defaultServicePrice(String propertyId) {
        return ServicePrice.builder()
                .propertyId(propertyId)
                .electricityPrice(DEFAULT_ELECTRICITY_PRICE)
                .waterPrice(DEFAULT_WATER_PRICE)
                .wifiFee(DEFAULT_FEE)
                .garbageFee(DEFAULT_FEE)
                .parkingFee(DEFAULT_FEE)
                .build();
    }

    private void ensurePropertyExists(String propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }
    }
}
