package com.example.boardinghouse.service;

import com.example.boardinghouse.common.exception.BadRequestException;
import com.example.boardinghouse.common.exception.ResourceNotFoundException;
import com.example.boardinghouse.domain.entity.Property;
import com.example.boardinghouse.repository.PropertyRepository;
import com.example.boardinghouse.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PropertyService propertyService;

    @BeforeEach
    void setUpOwner() {
        lenient().when(currentUserService.getOwnerId()).thenReturn("owner-1");
    }

    @Test
    void deletePropertyRejectsPropertyWithRooms() {
        Property property = Property.builder()
                .id("property-1")
                .ownerId("owner-1")
                .name("Nha tro A")
                .address("123 Nguyen Trai")
                .build();

        when(propertyRepository.findByIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(property));
        when(mongoTemplate.count(any(Query.class), eq("rooms"))).thenReturn(2L);

        assertThatThrownBy(() -> propertyService.deleteProperty("property-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot delete property because it still has rooms");

        verify(propertyRepository, never()).save(any(Property.class));
    }

    @Test
    void deletePropertyDeletesWhenNoRoomsExist() {
        Property property = Property.builder()
                .id("property-1")
                .ownerId("owner-1")
                .name("Nha tro A")
                .address("123 Nguyen Trai")
                .build();

        when(propertyRepository.findByIdAndOwnerId("property-1", "owner-1")).thenReturn(Optional.of(property));
        when(mongoTemplate.count(any(Query.class), eq("rooms"))).thenReturn(0L);

        propertyService.deleteProperty("property-1");

        ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);
        verify(propertyRepository).save(captor.capture());
        Property deleted = captor.getValue();
        assertThat(deleted.getId()).isEqualTo("property-1");
        assertThat(deleted.getDeleted()).isTrue();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getDeletedBy()).isEqualTo("owner-1");
    }

    @Test
    void getPropertyByIdRejectsMissingProperty() {
        when(propertyRepository.findByIdAndOwnerId("missing", "owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.getPropertyById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Property not found with id: missing");
    }
}
