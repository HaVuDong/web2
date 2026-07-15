package com.example.boardinghouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${app.resend.api-key}")
    private String resendApiKey;

    @Value("${app.resend.from-email}")
    private String fromEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    public void sendTenantInviteEmail(String toEmail, String tenantName, String password) {
        String subject = "Thông tin đăng nhập tài khoản Khách thuê trọ";
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 8px;">
                <h2 style="color: #7C3AED;">Xin chào %s,</h2>
                <p>Chủ trọ đã tạo tài khoản khách thuê cho bạn trên hệ thống <b>Quản Lý Trọ</b>.</p>
                <p>Bạn có thể đăng nhập bằng thông tin sau:</p>
                <div style="background-color: #f8f9fa; padding: 15px; border-radius: 6px; margin: 15px 0;">
                    <p style="margin: 5px 0;"><strong>Email:</strong> %s</p>
                    <p style="margin: 5px 0;"><strong>Mật khẩu:</strong> %s</p>
                </div>
                <p>Vui lòng đăng nhập và bảo mật thông tin tài khoản của bạn.</p>
                <br>
                <p>Trân trọng,<br><strong>Ban Quản Lý Trọ</strong></p>
            </div>
            """, tenantName, toEmail, password);

        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendMeterReadingReminderEmail(String toEmail, String tenantName, int month, int year) {
        String subject = String.format("Nhắc nhở nhập chỉ số điện nước tháng %d/%d", month, year);
        String htmlContent = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 8px;">
                <h2 style="color: #7C3AED;">Xin chào %s,</h2>
                <p>Đã đến thời điểm nhập chỉ số điện nước cho <b>tháng %d/%d</b>.</p>
                <div style="background-color: #fef3c7; padding: 15px; border-radius: 6px; margin: 15px 0; border-left: 4px solid #f59e0b;">
                    <p style="margin: 5px 0; font-weight: bold; color: #b45309;">⏰ Hạn chót: Trước ngày 5/%d/%d</p>
                    <p style="margin: 5px 0; color: #92400e;">Vui lòng đăng nhập vào ứng dụng và nhập chỉ số điện nước mới cho phòng của bạn.</p>
                </div>
                <p>Nếu bạn không nhập trước hạn, chủ trọ sẽ tự nhập chỉ số thay cho bạn.</p>
                <br>
                <p>Trân trọng,<br><strong>Ban Quản Lý Trọ</strong></p>
            </div>
            """, tenantName, month, year, month, year);

        sendEmail(toEmail, subject, htmlContent);
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = new HashMap<>();
            // Clean up fromEmail if it has brackets: "<otp@...>" just in case
            String cleanFrom = fromEmail != null ? fromEmail.replaceAll("[<>]", "").trim() : "no-reply@example.com";
            body.put("from", "Ban Quan Ly Tro <" + cleanFrom + ">");
            body.put("to", List.of(to));
            body.put("subject", subject);
            body.put("html", html);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            restTemplate.postForObject(RESEND_API_URL, request, String.class);
            log.info("Successfully sent email to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
