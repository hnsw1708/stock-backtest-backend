package com.example.stockbacktest.controller;

import com.example.stockbacktest.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileUploadController fileUploadController;

    // ---------------------------------------------------------------------------
    // Happy-path: known fileType values → expected filename
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "fileType={1} → filename={2}")
    @CsvSource({
        "CN,   sh000001, 000001.SH.csv",
        "CN,   sh000300, 000300.SH.csv",
        "CN,   sz399006, 399006.SZ.csv",
        "US,   dji,      DJI.csv",
        "US,   nasdaq,   NASDAQ.csv",
        "US,   sp500,    SP500.csv",
        "HK,   hsi,      HSI.csv",
        "HK,   hkah,     HKAH.csv"
    })
    @DisplayName("uploadFile returns 200 OK with correct filename for known fileTypes")
    void uploadFile_knownFileType_returns200WithCorrectFilename(
            String market, String fileType, String expectedFilename) throws Exception {

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market.trim(), fileType.trim(), multipartFile);

        // Assert HTTP status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert body
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename.trim());

        // Verify service was called with the expected filename
        verify(fileStorageService).storeFile(
                eq(multipartFile), eq(market.trim()), eq(expectedFilename.trim()));
    }

    @Test
    @DisplayName("uploadFile with fileType 'moneygrow' appends market to filename")
    void uploadFile_moneygrowFileType_appendsMarket() throws Exception {
        String market = "CN";
        String fileType = "moneygrow";
        String expectedFilename = "MoneyGrow_CN.txt";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq(expectedFilename));
    }

    @Test
    @DisplayName("uploadFile with fileType 'moneygrow' uses provided market value in filename")
    void uploadFile_moneygrowFileType_differentMarket() throws Exception {
        String market = "US";
        String fileType = "moneygrow";
        String expectedFilename = "MoneyGrow_US.txt";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq(expectedFilename));
    }

    // ---------------------------------------------------------------------------
    // Default branch: unknown fileType → <fileType>.csv
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("uploadFile with unknown fileType falls back to '<fileType>.csv'")
    void uploadFile_unknownFileType_fallsBackToDefaultFilename() throws Exception {
        String market = "EU";
        String fileType = "someUnknownIndex";
        String expectedFilename = "someUnknownIndex.csv";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq(expectedFilename));
    }

    @Test
    @DisplayName("uploadFile with empty string fileType falls back to '.csv'")
    void uploadFile_emptyStringFileType_fallsBackToDotCsv() throws Exception {
        String market = "CN";
        String fileType = "";
        String expectedFilename = ".csv";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq(expectedFilename));
    }

    // ---------------------------------------------------------------------------
    // Error / exception handling
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("uploadFile returns 400 Bad Request when service throws RuntimeException")
    void uploadFile_serviceThrowsRuntimeException_returns400() throws Exception {
        String market = "CN";
        String fileType = "sh000001";
        String errorMessage = "Disk full";

        doThrow(new RuntimeException(errorMessage))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains(errorMessage);

        verify(fileStorageService).storeFile(any(), eq(market), eq("000001.SH.csv"));
    }

    @Test
    @DisplayName("uploadFile returns 400 Bad Request when service throws checked Exception")
    void uploadFile_serviceThrowsCheckedException_returns400() throws Exception {
        String market = "US";
        String fileType = "nasdaq";
        String errorMessage = "IO failure";

        doThrow(new RuntimeException(new Exception(errorMessage)))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).startsWith("File upload failed:");
    }

    @Test
    @DisplayName("uploadFile error message contains the exception message text")
    void uploadFile_errorResponse_containsExceptionMessage() throws Exception {
        String specificError = "Connection refused to storage backend";

        doThrow(new IllegalStateException(specificError))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("HK", "hsi", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("status", "error");
        assertThat(response.getBody().get("message")).contains(specificError);
    }

    // ---------------------------------------------------------------------------
    // Response structure invariants
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Successful response body always contains 'message' and 'status' keys")
    void uploadFile_successResponse_hasExpectedKeys() throws Exception {
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "sh000300", multipartFile);

        assertThat(response.getBody())
                .isNotNull()
                .containsKeys("message", "status");
    }

    @Test
    @DisplayName("Error response body always contains 'message' and 'status' keys")
    void uploadFile_errorResponse_hasExpectedKeys() throws Exception {
        doThrow(new RuntimeException("oops"))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "sh000300", multipartFile);

        assertThat(response.getBody())
                .isNotNull()
                .containsKeys("message", "status");
    }

    @Test
    @DisplayName("Successful response 'message' field includes the generated filename")
    void uploadFile_successMessage_includesGeneratedFilename() throws Exception {
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "sz399006", multipartFile);

        assertThat(response.getBody())
                .isNotNull();
        assertThat(response.getBody().get("message"))
                .contains("399006.SZ.csv")
                .contains("successfully");
    }

    // ---------------------------------------------------------------------------
    // Service interaction verification
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("fileStorageService.storeFile is called exactly once per upload request")
    void uploadFile_callsStoreFileExactlyOnce() throws Exception {
        fileUploadController.uploadFile("US", "sp500", multipartFile);

        verify(fileStorageService, times(1)).storeFile(any(), any(), any());
        verifyNoMoreInteractions(fileStorageService);
    }

    @Test
    @DisplayName("fileStorageService.storeFile receives the original MultipartFile reference")
    void uploadFile_passesOriginalMultipartFileToService() throws Exception {
        fileUploadController.uploadFile("HK", "hkah", multipartFile);

        verify(fileStorageService).storeFile(eq(multipartFile), any(), any());
    }

    @Test
    @DisplayName("fileStorageService.storeFile receives correct market path variable")
    void uploadFile_passesMarketToService() throws Exception {
        String market = "HK";
        fileUploadController.uploadFile(market, "hsi", multipartFile);

        verify(fileStorageService).storeFile(any(), eq(market), any());
    }
}
