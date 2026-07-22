package com.cartzilla.product.api.controller;

import com.cartzilla.product.api.exception.GlobalExceptionHandler;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMediaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private AdminMediaController adminMediaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminMediaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/admin/media/upload — Upload ảnh thành công và trả về Cloudinary URL")
    void uploadImage_success() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of(
                        "secure_url", "https://res.cloudinary.com/demo/image/upload/v1234/cartzilla/products/sample.jpg",
                        "public_id", "cartzilla/products/sample"
                ));

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/media/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Upload ảnh thành công"))
                .andExpect(jsonPath("$.data.url").value("https://res.cloudinary.com/demo/image/upload/v1234/cartzilla/products/sample.jpg"))
                .andExpect(jsonPath("$.data.publicId").value("cartzilla/products/sample"));
    }

    @Test
    @DisplayName("POST /api/admin/media/upload — Từ chối file rỗng (422 BusinessException)")
    void uploadImage_emptyFile_throwsException() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        mockMvc.perform(multipart("/api/admin/media/upload").file(emptyFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File upload không được để rỗng"));
    }

    @Test
    @DisplayName("POST /api/admin/media/upload — Từ chối file không phải định dạng ảnh (422 BusinessException)")
    void uploadImage_invalidContentType_throwsException() throws Exception {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "document.txt", "text/plain", "hello world".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/media/upload").file(txtFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Định dạng file không hợp lệ, chỉ chấp nhận file ảnh (.jpg, .png, .webp, .gif)"));
    }
}
