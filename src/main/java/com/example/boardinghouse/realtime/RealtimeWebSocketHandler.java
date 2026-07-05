package com.example.boardinghouse.realtime;

import com.example.boardinghouse.security.CustomUserDetailsService;
import com.example.boardinghouse.security.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xử lý các kết nối WebSocket đến server.
 * Quản lý danh sách các session đang mở, xác thực token qua WebSocket, và gửi tin nhắn (broadcast) cho client.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final ObjectMapper objectMapper;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final Map<String, WebSocketSession> authenticatedSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionOwnerIds = new ConcurrentHashMap<>();

    /**
     * Nhận và xử lý tin nhắn dạng Text từ client gửi lên.
     * Ban đầu dùng để nhận chuỗi xác thực (AUTH), sau đó dùng để phản hồi PING/PONG.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (authenticatedSessions.containsKey(session.getId())) {
            handleAuthenticatedMessage(session, message);
            return;
        }

        authenticate(session, message);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        authenticatedSessions.remove(session.getId());
        sessionOwnerIds.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        authenticatedSessions.remove(session.getId());
        sessionOwnerIds.remove(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * Gửi (broadcast) một sự kiện đến toàn bộ các client đang kết nối thành công.
     */
    public void broadcast(RealtimeEvent<?> event) {
        broadcastToOwner(event, null);
    }

    /**
     * Gửi (broadcast) một sự kiện chỉ đến các client của một ownerId cụ thể.
     */
    public void broadcastToOwner(RealtimeEvent<?> event, String targetOwnerId) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception exception) {
            log.error("Unable to serialize realtime event {}", event.type(), exception);
            return;
        }

        authenticatedSessions.forEach((sessionId, session) -> {
            if (targetOwnerId != null) {
                String sessionOwnerId = sessionOwnerIds.get(sessionId);
                if (sessionOwnerId == null || !sessionOwnerId.equals(targetOwnerId)) {
                    return; // Skip this session because it doesn't belong to the target owner
                }
            }

            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                } else {
                    authenticatedSessions.remove(sessionId);
                    sessionOwnerIds.remove(sessionId);
                }
            } catch (IOException exception) {
                authenticatedSessions.remove(sessionId);
                sessionOwnerIds.remove(sessionId);
                closeQuietly(session, CloseStatus.SERVER_ERROR);
                log.warn("Unable to send realtime event to session {}", sessionId, exception);
            }
        });
    }

    int authenticatedSessionCount() {
        return authenticatedSessions.size();
    }

    /**
     * Xử lý xác thực JWT gửi từ client ngay khi mở kết nối WebSocket.
     */
    private void authenticate(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode request = objectMapper.readTree(message.getPayload());
            if (!"AUTH".equals(request.path("type").asText())) {
                closeQuietly(session, CloseStatus.POLICY_VIOLATION);
                return;
            }

            String token = request.path("token").asText();
            if (token.isBlank() || !jwtUtils.validateJwtToken(token)) {
                closeQuietly(session, CloseStatus.POLICY_VIOLATION);
                return;
            }

            String username = jwtUtils.getUserNameFromJwtToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!userDetails.isEnabled()) {
                closeQuietly(session, CloseStatus.POLICY_VIOLATION);
                return;
            }

            WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                    session,
                    SEND_TIME_LIMIT_MS,
                    BUFFER_SIZE_LIMIT_BYTES
            );
            authenticatedSessions.put(session.getId(), safeSession);
            
            // Note: Assuming userDetails is instance of CustomUserDetails, we extract the owner ID
            if (userDetails instanceof com.example.boardinghouse.security.CustomUserDetails customUserDetails) {
                sessionOwnerIds.put(session.getId(), customUserDetails.getUser().getId());
            }

            safeSession.sendMessage(new TextMessage(
                    objectMapper.writeValueAsString(Map.of("type", "AUTHENTICATED"))
            ));
        } catch (Exception exception) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
        }
    }

    private void handleAuthenticatedMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode request = objectMapper.readTree(message.getPayload());
        if ("PING".equals(request.path("type").asText())) {
            WebSocketSession safeSession = authenticatedSessions.get(session.getId());
            if (safeSession != null && safeSession.isOpen()) {
                safeSession.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            }
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus closeStatus) {
        try {
            if (session.isOpen()) {
                session.close(closeStatus);
            }
        } catch (IOException exception) {
            log.debug("Unable to close websocket session {}", session.getId(), exception);
        }
    }
}
