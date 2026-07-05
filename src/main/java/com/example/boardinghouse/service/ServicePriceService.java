package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.ServicePrice;
import com.example.boardinghouse.dto.serviceprice.UpdateServicePriceRequest;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.repository.ServicePriceRepository;
import com.example.boardinghouse.security.CurrentUserService;
import com.example.boardinghouse.realtime.RealtimeEventPublisher;
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
    private final CurrentUserService currentUserService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    /**
     * Lấy bảng giá dịch vụ của một tòa nhà.
     * Nếu chưa có, tự động tạo mới với bảng giá mặc định và lưu vào CSDL.
     *
     * @param propertyId ID của tòa nhà
     * @return Bảng giá dịch vụ hiện tại
     */
    public ServicePrice getServicePrice(String propertyId) {
        String ownerId = currentUserService.getOwnerId();
        ensurePropertyExists(propertyId, ownerId);
        return servicePriceRepository.findByPropertyIdAndOwnerId(propertyId, ownerId)
                .orElseGet(() -> servicePriceRepository.save(defaultServicePrice(propertyId, ownerId)));
    }

    /**
     * Cập nhật thông tin bảng giá dịch vụ cho tòa nhà (giá điện, nước, rác, wifi...).
     * Nếu bảng giá chưa tồn tại thì sẽ khởi tạo trước rồi mới cập nhật.
     */
    public ServicePrice updateServicePrice(String propertyId, UpdateServicePriceRequest request) {
        String ownerId = currentUserService.getOwnerId();
        ensurePropertyExists(propertyId, ownerId);
        ServicePrice servicePrice = servicePriceRepository.findByPropertyIdAndOwnerId(propertyId, ownerId)
                .orElseGet(() -> defaultServicePrice(propertyId, ownerId));

        servicePrice.setElectricityPrice(request.getElectricityPrice());
        servicePrice.setWaterPrice(request.getWaterPrice());
        servicePrice.setWifiFee(request.getWifiFee());
        servicePrice.setGarbageFee(request.getGarbageFee());
        servicePrice.setParkingFee(request.getParkingFee());

        ServicePrice saved = servicePriceRepository.save(servicePrice);
        realtimeEventPublisher.publishGlobalUpdate();
        return saved;
    }

    /**
     * Khởi tạo bảng giá dịch vụ mặc định (ví dụ: điện 3500đ, nước 15000đ).
     */
    private ServicePrice defaultServicePrice(String propertyId, String ownerId) {
        return ServicePrice.builder()
                .ownerId(ownerId)
                .propertyId(propertyId)
                .electricityPrice(DEFAULT_ELECTRICITY_PRICE)
                .waterPrice(DEFAULT_WATER_PRICE)
                .wifiFee(DEFAULT_FEE)
                .garbageFee(DEFAULT_FEE)
                .parkingFee(DEFAULT_FEE)
                .build();
    }

    /**
     * Kiểm tra xem tòa nhà có tồn tại trong CSDL hay không.
     */
    private void ensurePropertyExists(String propertyId, String ownerId) {
        if (propertyRepository.findByIdAndOwnerId(propertyId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }
    }
}
