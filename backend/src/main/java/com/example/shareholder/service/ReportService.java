package com.example.shareholder.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class ReportService {

    private final ExportToExcelService exportToExcelService;

    public ReportService(ExportToExcelService exportToExcelService) {
        this.exportToExcelService = exportToExcelService;
    }

    public void exportToExcel(HttpServletResponse response, List<Map<String, Object>> data, String[] fields, String title) throws IOException {
        exportToExcelService.exportToExcel(response, data, fields, title);
    }
}