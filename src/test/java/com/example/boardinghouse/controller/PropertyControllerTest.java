package com.example.boardinghouse.controller;

import com.example.boardinghouse.domain.entity.Property;
import com.example.boardinghouse.domain.entity.User;
import com.example.boardinghouse.domain.enums.UserRole;
import com.example.boardinghouse.security.CustomUserDetails;
import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtAuthFilter;
import com.example.boardinghouse.service.PropertyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PropertyService propertyService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllPropertiesReturnsProperties() throws Exception {
        Property property = Property.builder()
                .id("property-1")
                .name("Nha tro A")
                .address("123 Nguyen Trai")
                .description("Gan truong dai hoc")
                .createdBy("owner-1")
                .build();

        when(propertyService.getAllProperties()).thenReturn(List.of(property));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("property-1"))
                .andExpect(jsonPath("$.data[0].name").value("Nha tro A"));
    }

    @Test
    void createPropertyReturnsCreatedProperty() throws Exception {
        Property property = Property.builder()
                .id("property-1")
                .name("Nha tro A")
                .address("123 Nguyen Trai")
                .description("Gan truong dai hoc")
                .createdBy("owner-1")
                .build();

        when(propertyService.createProperty(any(), any())).thenReturn(property);

        mockMvc.perform(post("/api/properties")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(ownerAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nha tro A",
                                  "address": "123 Nguyen Trai",
                                  "description": "Gan truong dai hoc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Property created successfully"))
                .andExpect(jsonPath("$.data.createdBy").value("owner-1"));
    }

    @Test
    void createPropertyRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(ownerAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "address": "123 Nguyen Trai"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Authentication ownerAuthentication() {
        CustomUserDetails userDetails = ownerUserDetails();
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private CustomUserDetails ownerUserDetails() {
        User user = User.builder()
                .id("owner-1")
                .email("owner@gmail.com")
                .name("Chu Tro")
                .role(UserRole.OWNER)
                .isActive(true)
                .build();

        return new CustomUserDetails(user);
    }
}
