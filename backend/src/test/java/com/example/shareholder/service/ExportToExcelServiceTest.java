package com.example.shareholder.service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExportToExcelService covering Excel workbook creation,
 * header/data writing, cell type handling, and HTTP response setup.
 */
@ExtendWith(MockitoExtension.class)
class ExportToExcelServiceTest {

    @Mock
    private HttpServletResponse response;

    private ExportToExcelService service;

    @BeforeEach
    void setUp() {
        service = new ExportToExcelService();
    }

    @Test
    void should_CreateNewWorkbook_When_NewReportExcelIsCalled() {
        // Domain: each export starts with a fresh workbook to avoid data leakage between reports
        service.newReportExcel();
        assertThat(service.workbook).isNotNull();
    }

    @Test
    void should_SetContentTypeAndHeader_When_InitResponseIsCalledWithFileName() {
        // Domain: response headers must instruct the browser to download the Excel file
        service.initResponseForExportExcel(response, "TestReport");

        verify(response).setContentType("application/octet-stream");
        verify(response).setHeader(eq("Content-Disposition"), contains("TestReport"));
    }

    @Test
    void should_CreateSheetWithTitleAndHeaders_When_WriteTableHeaderExcelIsCalled() {
        // Domain: Excel file must have a sheet with a title row and labelled column headers
        service.newReportExcel();
        String[] fields = {"id", "name", "email"};
        service.writeTableHeaderExcel("TestSheet", "MyTitle", fields);

        assertThat(service.sheet).isNotNull();
        assertThat(service.sheet.getSheetName()).isEqualTo("TestSheet");
        // Row 0 = title, Row 1 = headers
        assertThat((Object) service.sheet.getRow(0)).isNotNull();
        assertThat((Object) service.sheet.getRow(1)).isNotNull();
    }

    @Test
    void should_WriteIntegerCell_When_ValueIsInteger() {
        // Domain: numeric share counts must be stored as numeric cells, not text
        service.newReportExcel();
        service.sheet = service.workbook.createSheet("S");
        CellStyle style = service.workbook.createCellStyle();
        var row = service.sheet.createRow(0);

        service.createCell(row, 0, 42, style);

        assertThat(row.getCell(0).getNumericCellValue()).isEqualTo(42.0);
    }

    @Test
    void should_WriteDoubleCell_When_ValueIsDouble() {
        // Domain: price values are stored as doubles in Excel
        service.newReportExcel();
        service.sheet = service.workbook.createSheet("S");
        CellStyle style = service.workbook.createCellStyle();
        var row = service.sheet.createRow(0);

        service.createCell(row, 0, 3.14, style);

        assertThat(row.getCell(0).getNumericCellValue()).isEqualTo(3.14);
    }

    @Test
    void should_WriteBooleanCell_When_ValueIsBoolean() {
        // Domain: transfer-tax-paid flag must be represented as a boolean cell
        service.newReportExcel();
        service.sheet = service.workbook.createSheet("S");
        CellStyle style = service.workbook.createCellStyle();
        var row = service.sheet.createRow(0);

        service.createCell(row, 0, true, style);

        assertThat(row.getCell(0).getBooleanCellValue()).isTrue();
    }

    @Test
    void should_WriteLongCell_When_ValueIsLong() {
        // Domain: entity IDs (Long) must be stored as numeric cells
        service.newReportExcel();
        service.sheet = service.workbook.createSheet("S");
        CellStyle style = service.workbook.createCellStyle();
        var row = service.sheet.createRow(0);

        service.createCell(row, 0, 100L, style);

        assertThat(row.getCell(0).getNumericCellValue()).isEqualTo(100.0);
    }

    @Test
    void should_WriteStringCell_When_ValueIsString() {
        // Domain: text fields like names must be stored as string cells
        service.newReportExcel();
        service.sheet = service.workbook.createSheet("S");
        CellStyle style = service.workbook.createCellStyle();
        var row = service.sheet.createRow(0);

        service.createCell(row, 0, "Helsinki", style);

        assertThat(row.getCell(0).getStringCellValue()).isEqualTo("Helsinki");
    }

    @Test
    void should_ReturnCellStyle_When_GetFontContentExcelIsCalled() {
        // Domain: content rows must use a distinct style from header rows
        service.newReportExcel();
        CellStyle style = service.getFontContentExcel();
        assertThat(style).isNotNull();
    }

    @Test
    void should_WriteDataRows_When_WriteTableDataIsCalled() {
        // Domain: each record in the export must produce exactly one row in the sheet
        service.newReportExcel();
        service.sheet = service.workbook.createSheet("S");
        // Create title row (0) and header row (1) so data starts at row 2
        service.sheet.createRow(0);
        service.sheet.createRow(1);

        String[] fields = {"name", "city"};
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Matti", "city", "Helsinki"),
                Map.of("name", "Maija", "city", "Tampere")
        );

        service.writeTableData(records, fields);

        assertThat((Object) service.sheet.getRow(2)).isNotNull();
        assertThat((Object) service.sheet.getRow(3)).isNotNull();
    }

    @Test
    void should_WriteWorkbookToOutputStream_When_ExportToExcelIsCalled() throws IOException {
        // Domain: the full export pipeline must produce a valid XLSX written to the HTTP response
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(jakarta.servlet.WriteListener l) {}
            @Override public void write(int b) { baos.write(b); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        String[] fields = {"name", "city"};
        List<Map<String, Object>> data = List.of(Map.of("name", "Testi", "city", "Helsinki"));

        service.exportToExcel(response, data, fields, "Report");

        assertThat(baos.size()).isGreaterThan(0);
        // Verify it's a valid XLSX (ZIP-based format starts with PK)
        assertThat(baos.toByteArray()[0]).isEqualTo((byte) 'P');
        assertThat(baos.toByteArray()[1]).isEqualTo((byte) 'K');
    }
}

