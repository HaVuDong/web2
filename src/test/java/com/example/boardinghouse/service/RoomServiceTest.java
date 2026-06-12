package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Room;
import com.example.boardinghouse.domain.enums.RoomStatus;
import com.example.boardinghouse.dto.room.CreateRoomRequest;
import com.example.boardinghouse.dto.room.UpdateRoomStatusRequest;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private RoomService roomService;

    @Test
    void createRoomCreatesAvailableRoomWhenStatusIsMissing() {
        CreateRoomRequest request = createRoomRequest("101");
        when(propertyRepository.existsById("property-1")).thenReturn(true);
        when(roomRepository.findByPropertyIdAndRoomNumber("property-1", "101")).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room room = roomService.createRoom("property-1", request);

        assertThat(room.getPropertyId()).isEqualTo("property-1");
        assertThat(room.getRoomNumber()).isEqualTo("101");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    void createRoomRejectsMissingProperty() {
        CreateRoomRequest request = createRoomRequest("101");
        when(propertyRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> roomService.createRoom("missing", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Property not found with id: missing");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void createRoomRejectsDuplicateRoomNumberInSameProperty() {
        CreateRoomRequest request = createRoomRequest("101");
        Room existingRoom = Room.builder()
                .id("room-1")
                .propertyId("property-1")
                .roomNumber("101")
                .build();

        when(propertyRepository.existsById("property-1")).thenReturn(true);
        when(roomRepository.findByPropertyIdAndRoomNumber("property-1", "101")).thenReturn(Optional.of(existingRoom));

        assertThatThrownBy(() -> roomService.createRoom("property-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Room number already exists in this property");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void updateRoomStatusUpdatesStatus() {
        Room room = Room.builder()
                .id("room-1")
                .propertyId("property-1")
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .build();

        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room updatedRoom = roomService.updateRoomStatus("room-1", RoomStatus.MAINTENANCE);

        assertThat(updatedRoom.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    void deleteRoomRejectsActiveContract() {
        Room room = Room.builder()
                .id("room-1")
                .propertyId("property-1")
                .roomNumber("101")
                .build();

        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(mongoTemplate.count(any(Query.class), eq("contracts"))).thenReturn(1L);

        assertThatThrownBy(() -> roomService.deleteRoom("room-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot delete room because it has an active contract");

        verify(roomRepository, never()).delete(any(Room.class));
    }

    @Test
    void deleteRoomDeletesWhenNoActiveContractExists() {
        Room room = Room.builder()
                .id("room-1")
                .propertyId("property-1")
                .roomNumber("101")
                .build();

        when(roomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(mongoTemplate.count(any(Query.class), eq("contracts"))).thenReturn(0L);

        roomService.deleteRoom("room-1");

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("room-1");
    }

    private CreateRoomRequest createRoomRequest(String roomNumber) {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber(roomNumber);
        request.setFloor(1);
        request.setArea(20.0);
        request.setBaseRent(2_500_000L);
        request.setDeposit(2_500_000L);
        request.setMaxTenants(2);
        request.setNote("Gan cau thang");
        return request;
    }
}
