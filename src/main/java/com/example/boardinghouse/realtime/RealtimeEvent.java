package com.example.boardinghouse.realtime;

import java.time.LocalDateTime;

/**
 * Class bọc (wrapper) chung cho tất cả các sự kiện realtime gửi qua WebSocket.
 * Định dạng chung bao gồm loại sự kiện, thời gian xảy ra và dữ liệu payload.
 */
public record RealtimeEvent<T>(
        String type,
        LocalDateTime occurredAt,
        T data
) {

    public static <T> RealtimeEvent<T> of(String type, T data) {
        return new RealtimeEvent<>(type, LocalDateTime.now(), data);
    }
}
