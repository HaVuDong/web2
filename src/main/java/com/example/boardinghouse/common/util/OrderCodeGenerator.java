package com.example.boardinghouse.common.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderCodeGenerator {

    public Long generate() {
        long timestampPart = Instant.now().toEpochMilli() * 1000;
        long randomPart = ThreadLocalRandom.current().nextLong(100, 1000);
        return timestampPart + randomPart;
    }
}
