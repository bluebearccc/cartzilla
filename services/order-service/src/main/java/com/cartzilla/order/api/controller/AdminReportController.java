package com.cartzilla.order.api.controller;

import com.cartzilla.order.application.usecase.ReportUseCase;
import com.cartzilla.order.domain.repository.ReportRepository;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * F18 — UC-09: báo cáo admin (doanh thu, đơn theo trạng thái/thanh toán, top sản phẩm).
 * Role ADMIN enforce bởi gateway JwtAuth + AdminRoleInterceptor (WebConfig).
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportUseCase reportUseCase;

    /** GET /api/admin/reports/summary?fromDate=&toDate= */
    @GetMapping("/summary")
    public ApiResponse<ReportUseCase.Summary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(reportUseCase.summary(from(fromDate), to(toDate)));
    }

    /** GET /api/admin/reports/order-status?fromDate=&toDate= */
    @GetMapping("/order-status")
    public ApiResponse<List<ReportRepository.CountByKey>> orderStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(reportUseCase.orderStatus(from(fromDate), to(toDate)));
    }

    /** GET /api/admin/reports/top-products?limit=&fromDate=&toDate= */
    @GetMapping("/top-products")
    public ApiResponse<List<ReportRepository.TopProduct>> topProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(reportUseCase.topProducts(from(fromDate), to(toDate), limit));
    }

    private Instant from(LocalDate d) {
        return d == null ? null : d.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant to(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }
}
