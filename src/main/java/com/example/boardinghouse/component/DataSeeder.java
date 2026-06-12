package com.example.boardinghouse.component;

import com.example.boardinghouse.domain.entity.User;
import com.example.boardinghouse.domain.enums.UserRole;
import com.example.boardinghouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${owner.seed.email}")
    private String seedEmail;

    @Value("${owner.seed.password}")
    private String seedPassword;

    @Value("${owner.seed.name}")
    private String seedName;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByEmail(seedEmail)) {
            User owner = User.builder()
                    .email(seedEmail)
                    .passwordHash(passwordEncoder.encode(seedPassword))
                    .name(seedName)
                    .role(UserRole.OWNER)
                    .isActive(true)
                    .build();
            userRepository.save(owner);
            System.out.println("Owner seeded successfully: " + seedEmail);
        }
    }
}
