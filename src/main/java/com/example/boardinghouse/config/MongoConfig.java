package com.example.boardinghouse.config;

import com.example.boardinghouse.domain.entity.Contract;
import com.example.boardinghouse.domain.enums.ContractStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.domain.Sort;

@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @Bean
    public ApplicationRunner contractIndexes() {
        return args -> mongoTemplate.indexOps(Contract.class)
                .ensureIndex(new Index()
                        .named("unique_active_contract_per_room")
                        .on("roomId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .unique()
                        .partial(PartialIndexFilter.of(Criteria.where("status").is(ContractStatus.ACTIVE))));
    }
}
