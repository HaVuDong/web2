package com.example.boardinghouse.config;

import com.example.boardinghouse.domain.entity.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.bson.Document;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        log.info("Khởi tạo MongoDB Indexes (Unique constraints)...");
        
        // 1. Room: Số phòng duy nhất trong một nhà trọ (khi chưa bị xóa)
        mongoTemplate.indexOps(Room.class).ensureIndex(
                new Index()
                        .on("propertyId", Sort.Direction.ASC)
                        .on("roomNumber", Sort.Direction.ASC)
                        .unique()
                        .partial(() -> new Document("deleted", false))
        );

        // 2. Property: Tên nhà trọ duy nhất cho một chủ (khi chưa bị xóa)
        mongoTemplate.indexOps(Property.class).ensureIndex(
                new Index()
                        .on("ownerId", Sort.Direction.ASC)
                        .on("name", Sort.Direction.ASC)
                        .unique()
                        .partial(() -> new Document("deleted", false))
        );

        // 3. Contract: Chỉ có 1 hợp đồng ACTIVE cho một phòng
        mongoTemplate.indexOps(Contract.class).ensureIndex(
                new Index()
                        .on("roomId", Sort.Direction.ASC)
                        .unique()
                        .partial(() -> new Document("deleted", false).append("status", "ACTIVE"))
        );

        // 4. MeterReading: Một tháng chỉ có 1 chỉ số cho một phòng
        mongoTemplate.indexOps(MeterReading.class).ensureIndex(
                new Index()
                        .on("roomId", Sort.Direction.ASC)
                        .on("month", Sort.Direction.ASC)
                        .on("year", Sort.Direction.ASC)
                        .unique()
                        .partial(() -> new Document("deleted", false))
        );

        // 5. Invoice: Một tháng chỉ có 1 hóa đơn cho một phòng
        mongoTemplate.indexOps(Invoice.class).ensureIndex(
                new Index()
                        .on("roomId", Sort.Direction.ASC)
                        .on("month", Sort.Direction.ASC)
                        .on("year", Sort.Direction.ASC)
                        .unique()
                        .partial(() -> new Document("deleted", false))
        );



        log.info("MongoDB Indexes đã được khởi tạo thành công.");
    }
}
