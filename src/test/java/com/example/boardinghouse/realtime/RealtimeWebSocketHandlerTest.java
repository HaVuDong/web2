package com.example.boardinghouse.realtime;

import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeWebSocketHandlerTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private WebSocketSession session;

    @Mock
    private UserDetails userDetails;

    private RealtimeWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new RealtimeWebSocketHandler(objectMapper, jwtUtils, userDetailsService);
    }

    @Test
    void unauthenticatedSessionDoesNotReceiveEvents() throws Exception {
        handler.broadcast(RealtimeEvent.of("PAYMENT_UPDATED", "data"));

        verify(session, never()).sendMessage(any());
        assertThat(handler.authenticatedSessionCount()).isZero();
    }

    @Test
    void invalidTokenClosesSession() throws Exception {
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        when(jwtUtils.validateJwtToken("invalid")).thenReturn(false);

        handler.handleMessage(session, new TextMessage("""
                {"type":"AUTH","token":"invalid"}
                """));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        assertThat(handler.authenticatedSessionCount()).isZero();
    }

    @Test
    void authenticatedSessionReceivesEvents() throws Exception {
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        when(jwtUtils.validateJwtToken("valid")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid")).thenReturn("owner@gmail.com");
        when(userDetailsService.loadUserByUsername("owner@gmail.com")).thenReturn(userDetails);
        when(userDetails.isEnabled()).thenReturn(true);

        handler.handleMessage(session, new TextMessage("""
                {"type":"AUTH","token":"valid"}
                """));
        handler.broadcast(RealtimeEvent.of("PAYMENT_UPDATED", "data"));

        assertThat(handler.authenticatedSessionCount()).isEqualTo(1);
        verify(session, org.mockito.Mockito.times(2)).sendMessage(any(TextMessage.class));
    }
}
