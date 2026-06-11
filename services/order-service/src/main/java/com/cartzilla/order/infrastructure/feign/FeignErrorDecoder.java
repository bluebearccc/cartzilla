package com.cartzilla.order.infrastructure.feign;

import com.cartzilla.web.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.nio.charset.StandardCharsets;

/**
 * Chuyển HTTP error từ downstream service thành BusinessException
 * để upstream handler xử lý nhất quán.
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = null;
        try {
            if (response.body() != null) {
                String body = feign.Util.toString(response.body().asReader(StandardCharsets.UTF_8));
                var jsonNode = objectMapper.readTree(body);
                if (jsonNode.has("message")) {
                    message = jsonNode.get("message").asText();
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi đọc body, dùng fallback message
        }

        if (message == null || message.isBlank()) {
            return switch (response.status()) {
                case 400 -> new BusinessException("Yêu cầu không hợp lệ khi gọi " + methodKey);
                case 404 -> new BusinessException("Không tìm thấy tài nguyên khi gọi " + methodKey);
                case 409 -> new BusinessException("Dữ liệu xung đột khi gọi " + methodKey);
                case 422 -> new BusinessException("Lỗi xác thực dữ liệu khi gọi " + methodKey);
                default -> defaultDecoder.decode(methodKey, response);
            };
        }

        return new BusinessException(message);
    }
}
