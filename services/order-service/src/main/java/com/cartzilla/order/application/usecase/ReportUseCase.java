package com.cartzilla.order.application.usecase;

import com.cartzilla.order.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** F18 — UC-09: tổng hợp báo cáo admin read-only, có filter khoảng ngày. */
@Service
@RequiredArgsConstructor
public class ReportUseCase {

    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public Summary summary(Instant from, Instant to) {
        return new Summary(
                reportRepository.totalRevenue(from, to),
                reportRepository.totalOrders(from, to),
                reportRepository.countByStatus(from, to),
                reportRepository.countByPaymentStatus(from, to),
                reportRepository.revenueByMethod(from, to));
    }

    @Transactional(readOnly = true)
    public List<ReportRepository.CountByKey> orderStatus(Instant from, Instant to) {
        return reportRepository.countByStatus(from, to);
    }

    @Transactional(readOnly = true)
    public List<ReportRepository.TopProduct> topProducts(Instant from, Instant to, int limit) {
        return reportRepository.topProducts(from, to, limit);
    }

    public record Summary(
            java.math.BigDecimal totalRevenue,
            long totalOrders,
            List<ReportRepository.CountByKey> ordersByStatus,
            List<ReportRepository.CountByKey> ordersByPaymentStatus,
            List<ReportRepository.MethodRevenue> revenueByMethod) {}
}
