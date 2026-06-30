package com.example.boardinghouse.security;

import com.example.boardinghouse.domain.entity.IdempotencyRecord;
import com.example.boardinghouse.repository.IdempotencyRecordRepository;
import com.example.boardinghouse.security.CustomUserDetails;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Component
@Order(100)
@Slf4j
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Cập nhật path match bao gồm /api/invoices/*/payments và /payment-link
        boolean isPaymentApi = path.startsWith("/api/payments") || 
                               path.matches("^/api/invoices/[^/]+/payments/.*$") ||
                               path.contains("/payment-link");

        boolean isMutableMethod = HttpMethod.POST.name().equals(method) || 
                                  HttpMethod.PUT.name().equals(method) || 
                                  HttpMethod.PATCH.name().equals(method) || 
                                  HttpMethod.DELETE.name().equals(method);
        
        // Bỏ qua webhook vì webhook có cơ chế xử lý chống trùng lặp riêng dựa trên orderCode
        boolean isWebhook = path.equals("/api/payments/payos/webhook");
        
        return !(isPaymentApi && isMutableMethod && !isWebhook);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // Nếu client không truyền Idempotency-Key, bỏ qua filter và xử lý bình thường
        if (!StringUtils.hasText(idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        
        String ownerId = userDetails.getUser().getId();
        
        Optional<IdempotencyRecord> existingRecordOpt = idempotencyRecordRepository.findByIdempotencyKeyAndOwnerId(idempotencyKey, ownerId);

        if (existingRecordOpt.isPresent()) {
            IdempotencyRecord record = existingRecordOpt.get();
            if ("PROCESSING".equals(record.getStatus())) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Request with this Idempotency-Key is already being processed");
                return;
            }

            if ("COMPLETED".equals(record.getStatus())) {
                log.info("Idempotency hit for key: {}. Returning cached response.", idempotencyKey);
                response.setStatus(record.getResponseStatusCode());
                response.setContentType("application/json");
                if (record.getResponseBody() != null) {
                    objectMapper.writeValue(response.getOutputStream(), record.getResponseBody());
                }
                return;
            }
            
            // Nếu status là FAILED, cho phép retry
        }

        // Tạo record mới (status PROCESSING)
        IdempotencyRecord record = IdempotencyRecord.builder()
                .ownerId(ownerId)
                .idempotencyKey(idempotencyKey)
                .method(request.getMethod())
                .requestPath(request.getRequestURI())
                .status("PROCESSING")
                .build();
        
        if (existingRecordOpt.isPresent()) {
            record.setId(existingRecordOpt.get().getId()); // Ghi đè nếu trước đó là FAILED
        }
        
        record = idempotencyRecordRepository.save(record);

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
            
            // Xử lý thành công
            record.setStatus("COMPLETED");
            record.setResponseStatusCode(responseWrapper.getStatus());
            
            byte[] responseData = responseWrapper.getContentAsByteArray();
            if (responseData.length > 0) {
                try {
                    Map<String, Object> responseBody = objectMapper.readValue(responseData, new TypeReference<Map<String, Object>>() {});
                    record.setResponseBody(responseBody);
                } catch (Exception e) {
                    log.warn("Could not parse response body as JSON for idempotency caching");
                }
            }
            record.setCompletedAt(LocalDateTime.now());
            idempotencyRecordRepository.save(record);
            
        } catch (Exception ex) {
            // Xử lý thất bại, cho phép client retry với cùng một Idempotency-Key
            record.setStatus("FAILED");
            idempotencyRecordRepository.save(record);
            throw ex;
        } finally {
            // Ghi nội dung response ra ngoài stream thật
            responseWrapper.copyBodyToResponse();
        }
    }
}
