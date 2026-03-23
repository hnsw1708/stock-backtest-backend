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
    // Happy-path: known fileType mappings
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "fileType={0} -> filename={2}")
    @CsvSource({
            "sh000001, SH, 000001.SH.csv",
            "sh000300, SH, 000300.SH.csv",
            "sz399006, SZ, 399006.SZ.csv",
            "dji,      US, DJI.csv",
            "nasdaq,   US, NASDAQ.csv",
            "sp500,    US, SP500.csv",
            "hsi,      HK, HSI.csv",
            "hkah,     HK, HKAH.csv"
    })
    @DisplayName("Known fileType produces correct filename and returns 200 OK")
    void uploadFile_knownFileTypes_returnsSuccessWithCorrectFilename(
            String fileType, String market, String expectedFilename) throws Exception {

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("fileType=moneygrow embeds market in filename and returns 200 OK")
    void uploadFile_moneygrowFileType_embedsMarketInFilename() throws Exception {
        String market = "CNY";
        String fileType = "moneygrow";
        String expectedFilename = "MoneyGrow_CNY.txt";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    // ---------------------------------------------------------------------------
    // Default / unknown fileType
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Unknown fileType falls through to default: fileType + '.csv'")
    void uploadFile_unknownFileType_usesDefaultFilename() throws Exception {
        String market = "US";
        String fileType = "customIndex";
        String expectedFilename = "customIndex.csv";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    // ---------------------------------------------------------------------------
    // Service throws an exception
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("When FileStorageService throws, returns 400 Bad Request with error status")
    void uploadFile_serviceThrows_returnsBadRequest() throws Exception {
        String market = "SH";
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
    }

    @Test
    @DisplayName("When FileStorageService throws IOException, returns 400 with error message")
    void uploadFile_serviceThrowsIOException_returnsBadRequest() throws Exception {
        String market = "HK";
        String fileType = "hsi";

        doThrow(new java.io.IOException("IO error"))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains("IO error");
    }

    // ---------------------------------------------------------------------------
    // Response body structure
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Successful upload response body contains 'message' and 'status' keys")
    void uploadFile_success_responseBodyContainsRequiredKeys() throws Exception {
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("SH", "sh000001", multipartFile);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull()
                .containsKeys("message", "status");
    }

    @Test
    @DisplayName("Error response body contains 'message' and 'status' keys")
    void uploadFile_error_responseBodyContainsRequiredKeys() throws Exception {
        doThrow(new RuntimeException("oops"))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("SH", "sh000001", multipartFile);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull()
                .containsKeys("message", "status");
    }

    // ---------------------------------------------------------------------------
    // Verify storeFile is called with correct market argument
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Market path variable is forwarded to FileStorageService unchanged")
    void uploadFile_marketForwardedToService() throws Exception {
        String market = "NASDAQ_MARKET";
        String fileType = "nasdaq";

        fileUploadController.uploadFile(market, fileType, multipartFile);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq("NASDAQ.csv"));
    }

    @Test
    @DisplayName("FileStorageService.storeFile is called exactly once per request")
    void uploadFile_storeFileCalledExactlyOnce() throws Exception {
        fileUploadController.uploadFile("US", "sp500", multipartFile);

        verify(fileStorageService, times(1)).storeFile(any(), any(), any());
    }

    // ---------------------------------------------------------------------------
    // Edge cases: empty / blank strings
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Empty string fileType falls through to default mapping")
    void uploadFile_emptyFileType_usesDefaultMapping() throws Exception {
        String market = "SH";
        String fileType = "";
        String expectedFilename = ".csv";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("moneygrow with empty market produces 'MoneyGrow_.txt'")
    void uploadFile_moneygrowWithEmptyMarket_producesFilenameWithNoMarket() throws Exception {
        String market = "";
        String fileType = "moneygrow";
        String expectedFilename = "MoneyGrow_.txt";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    // ---------------------------------------------------------------------------
    // Java 21 text-block helper: exception with multi-line message
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Exception message is preserved verbatim in error response")
    void uploadFile_exceptionMessagePreservedInResponse() throws Exception {
        String detailedError = """
                Storage backend unavailable:\
                 connection refused on port 9000""";

        doThrow(new RuntimeException(detailedError))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("SH", "sh000300", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains(detailedError);
    }
}
