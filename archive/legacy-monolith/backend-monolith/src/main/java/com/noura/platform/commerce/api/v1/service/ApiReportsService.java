package com.noura.platform.commerce.api.v1.service;

import com.noura.platform.commerce.api.v1.dto.reports.ReportSaleRowDto;
import com.noura.platform.commerce.api.v1.dto.reports.ReportShiftRowDto;
import com.noura.platform.commerce.api.v1.dto.reports.ReportsSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ApiReportsService {
    ReportsSummaryDto summary(LocalDate from, LocalDate to, String cashier, String terminal);

    Page<ReportSaleRowDto> sales(LocalDate from, LocalDate to, Pageable pageable);

    Page<ReportShiftRowDto> shifts(LocalDate from, LocalDate to, String cashier, String terminal, Pageable pageable);
}
