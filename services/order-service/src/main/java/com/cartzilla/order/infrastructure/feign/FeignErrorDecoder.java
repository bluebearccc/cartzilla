package com.cartzilla.order.infrastructure.feign;

import com.cartzilla.web.exception.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Chuyển HTTP error từ downstream service thành BusinessException
 * để upstream handler xử lý nhất quán.
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 400 -> new BusinessException("Bad request calling " + methodKey);
            case 404 -> new BusinessException("Resource not found calling " + methodKey);
            case 409 -> new BusinessException("Conflict calling " + methodKey);
            case 422 -> new BusinessException("Validation failed calling " + methodKey);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
