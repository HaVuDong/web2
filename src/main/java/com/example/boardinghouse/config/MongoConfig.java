package com.example.boardinghouse.config;

import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.enums.ContractStatus;
import com.example.boardinghouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.domain.Sort;

/**
 * Cấu hình cho MongoDB.
 * Kích hoạt tính năng tự động cập nhật thời gian (Auditing) cho các Entity.
 */
@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;

    @Value("${app.data.backfill-owner-missing:false}")
    private boolean backfillOwnerMissing;

    /**
     * Tạo index (chỉ mục) cho collection Contract trong MongoDB.
     * Đảm bảo rằng tại một thời điểm, mỗi phòng (roomId) chỉ có tối đa MỘT hợp đồng ở trạng thái ACTIVE (đang hoạt động).
     */
    @Bean
    public ApplicationRunner contractIndexes() {
        return args -> mongoTemplate.indexOps(Contract.class)
                .ensureIndex(new Index()
                        .named("unique_active_contract_per_room_v2")
                        .on("ownerId", Sort.Direction.ASC)
                        .on("roomId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .unique()
                        .partial(PartialIndexFilter.of(Criteria.where("status").is(ContractStatus.ACTIVE))));
    }

    @Bean
    public ApplicationRunner ownerBackfillRunner() {
        return args -> {
            if (!backfillOwnerMissing) {
                return;
            }

            userRepository.findAll().stream().findFirst().ifPresent(user -> {
                String ownerId = user.getId();
                backfillOwner("properties", ownerId);
                backfillOwner("rooms", ownerId);
                backfillOwner("tenants", ownerId);
                backfillOwner("contracts", ownerId);
                backfillOwner("invoices", ownerId);
                backfillOwner("payments", ownerId);
                backfillOwner("meter_readings", ownerId);
                backfillOwner("maintenance_requests", ownerId);
                backfillOwner("service_prices", ownerId);
            });
        };
    }

    private void backfillOwner(String collectionName, String ownerId) {
        Query query = Query.query(new Criteria().orOperator(
                Criteria.where("ownerId").exists(false),
                Criteria.where("ownerId").is(null)
        ));
        mongoTemplate.updateMulti(query, new Update().set("ownerId", ownerId), collectionName);
    }
}
