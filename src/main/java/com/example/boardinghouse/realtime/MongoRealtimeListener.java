package com.example.boardinghouse.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe các sự kiện thay đổi dữ liệu từ MongoDB (Save, Delete).
 * Khi có bất kỳ dữ liệu nào thay đổi, phát tín hiệu GLOBAL_UPDATE qua WebSocket.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoRealtimeListener extends AbstractMongoEventListener<Object> {

    private final RealtimeEventPublisher publisher;

    @Override
    public void onAfterSave(AfterSaveEvent<Object> event) {
        log.debug("Entity saved: {}, broadcasting GLOBAL_UPDATE", event.getSource().getClass().getSimpleName());
        publisher.publishGlobalUpdate();
    }

    @Override
    public void onAfterDelete(AfterDeleteEvent<Object> event) {
        log.debug("Entity deleted: {}, broadcasting GLOBAL_UPDATE", event.getType().getSimpleName());
        publisher.publishGlobalUpdate();
    }
}
