package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.ApiPaths;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** Admin Media API — upload file ảnh sản phẩm lên Cloudinary. */
@RestController
@RequestMapping(ApiPaths.ADMIN_MEDIA)
@RequiredArgsConstructor
public class AdminMediaController {

    private final Cloudinary cloudinary;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File upload không được để rỗng");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Định dạng file không hợp lệ, chỉ chấp nhận file ảnh (.jpg, .png, .webp, .gif)");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "cartzilla/products"
            ));

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Upload ảnh thành công", Map.of(
                            "url", secureUrl,
                            "publicId", publicId
                    )));
        } catch (IOException e) {
            throw new BusinessException("Lỗi khi upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }
}
