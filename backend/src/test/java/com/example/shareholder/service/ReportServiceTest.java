package com.example.shareholder.service;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService verifying that export calls are correctly
 * delegated to ExportToExcelService with all provided arguments.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ExportToExcelService exportToExcelService;

    @Mock
    private HttpServletResponse response;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(exportToExcelService);
    }

    @Test
    void should_DelegateExportToExcel_When_ExportToExcelIsCalled() throws IOException {
        // Domain: ReportService is a thin façade; every export call must reach ExportToExcelService
        List<Map<String, Object>> data = List.of(Map.of("id", 1L, "name", "Test"));
        String[] fields = {"id", "name"};
        String title = "TestReport";

        service.exportToExcel(response, data, fields, title);

        verify(exportToExcelService, times(1)).exportToExcel(response, data, fields, title);
    }

    @Test
    void should_PassExactArguments_When_ExportToExcelIsCalled() throws IOException {
        // Domain: report title, field list, and data payload must not be modified by the façade
        List<Map<String, Object>> data = List.of();
        String[] fields = {"id", "firstname", "lastname"};
        String title = "Osakasluettelo";

        service.exportToExcel(response, data, fields, title);

        verify(exportToExcelService).exportToExcel(
                same(response),
                same(data),
                same(fields),
                eq(title));
    }

    @Test
    void should_PropagateIOException_When_ExportToExcelServiceThrows() throws IOException {
        // Domain: I/O errors during Excel generation must propagate to the caller unchanged
        doThrow(new IOException("disk full")).when(exportToExcelService)
                .exportToExcel(any(), any(), any(), any());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.exportToExcel(response, List.of(), new String[]{}, "title"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("disk full");
    }
}

