package com.example.boardinghouse.security;

import com.example.boardinghouse.common.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public String getOwnerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BadRequestException("Authenticated user is required");
        }

        if (userDetails.getUser() == null || userDetails.getUser().getId() == null) {
            throw new BadRequestException("Authenticated user id is required");
        }

        return userDetails.getUser().getId();
    }
}
