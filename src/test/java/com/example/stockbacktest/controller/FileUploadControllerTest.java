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
import static org.mockito.ArgumentMatchers.anyString;
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

    // -----------------------------------------------------------------------
    // Happy-path: known fileType → deterministic filename
    // -----------------------------------------------------------------------

    @ParameterizedTest(name = "fileType={0} should produce filename={1}")
    @CsvSource({
        "sh000001, 000001.SH.csv",
        "sh000300, 000300.SH.csv",
        "sz399006, 399006.SZ.csv",
        "dji,      DJI.csv",
        "nasdaq,   NASDAQ.csv",
        "sp500,    SP500.csv",
        "hsi,      HSI.csv",
        "hkah,     HKAH.csv"
    })
    @DisplayName("Known fileType values should map to the correct filename")
    void uploadFile_knownFileType_returnsSuccessWithCorrectFilename(
            String fileType, String expectedFilename) throws Exception {

        String market = "SH";
        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

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
    @DisplayName("fileType=moneygrow should embed the market value in the filename")
    void uploadFile_moneygrowFileType_embedsMarketInFilename() throws Exception {
        String market = "HK";
        String fileType = "moneygrow";
        String expectedFilename = "MoneyGrow_HK.txt";

        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

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
    @DisplayName("Unknown fileType should fall back to fileType+'.csv'")
    void uploadFile_unknownFileType_fallsBackToFileTypeDotCsv() throws Exception {
        String market = "US";
        String fileType = "customIndex";
        String expectedFilename = "customIndex.csv";

        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq(expectedFilename));
    }

    // -----------------------------------------------------------------------
    // Success response structure
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Successful upload response body contains both 'status' and 'message' keys")
    void uploadFile_success_responseBodyContainsBothKeys() throws Exception {
        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("SH", "sh000001", multipartFile);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull()
                .containsKeys("status", "message");
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).startsWith("File uploaded successfully:");
    }

    // -----------------------------------------------------------------------
    // Error / exception handling
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("When service throws, response is 400 BAD REQUEST with error status")
    void uploadFile_serviceThrows_returns400WithErrorStatus() throws Exception {
        String errorMessage = "Disk full";
        doThrow(new RuntimeException(errorMessage))
                .when(fileStorageService).storeFile(any(), anyString(), anyString());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("SH", "sh000001", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains("File upload failed:");
        assertThat(body.get("message")).contains(errorMessage);
    }

    @Test
    @DisplayName("Error response body contains both 'status' and 'message' keys")
    void uploadFile_serviceThrows_responseBodyContainsBothKeys() throws Exception {
        doThrow(new RuntimeException("IO error"))
                .when(fileStorageService).storeFile(any(), anyString(), anyString());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("SH", "dji", multipartFile);

        assertThat(response.getBody())
                .isNotNull()
                .containsKeys("status", "message");
    }

    @Test
    @DisplayName("When service throws IOException, error message is propagated")
    void uploadFile_serviceThrowsCheckedException_errorMessagePropagated() throws Exception {
        String ioMessage = "Cannot write to storage";
        doThrow(new java.io.IOException(ioMessage))
                .when(fileStorageService).storeFile(any(), anyString(), anyString());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("US", "nasdaq", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains(ioMessage);
    }

    // -----------------------------------------------------------------------
    // Edge cases: market value is passed through to the service
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Market path variable is forwarded to FileStorageService unchanged")
    void uploadFile_marketIsPassedToService() throws Exception {
        String market = "CN";
        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

        fileUploadController.uploadFile(market, "hsi", multipartFile);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), anyString());
    }

    @Test
    @DisplayName("Empty string market with moneygrow produces filename MoneyGrow_.txt")
    void uploadFile_emptyMarketWithMoneygrow_filenameContainsEmptyMarket() throws Exception {
        String market = "";
        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, "moneygrow", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains("MoneyGrow_.txt");
    }

    @Test
    @DisplayName("MultipartFile is forwarded to FileStorageService as-is")
    void uploadFile_multipartFileIsPassedToService() throws Exception {
        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

        fileUploadController.uploadFile("SH", "sp500", multipartFile);

        verify(fileStorageService, times(1)).storeFile(eq(multipartFile), anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // Case sensitivity: fileType values must match exactly
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Uppercase fileType should fall back to default (not match case-insensitively)")
    void uploadFile_upperCaseFileType_usesDefaultMapping() throws Exception {
        String fileType = "DJI"; // uppercase — should NOT match the 'dji' case
        String expectedFilename = "DJI.csv"; // default: fileType + ".csv"

        doNothing().when(fileStorageService).storeFile(any(), anyString(), anyString());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("US", fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains(expectedFilename);
    }

    // -----------------------------------------------------------------------
    // Verify service is never called when we do not invoke uploadFile
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Service is not called without invoking uploadFile")
    void noInteractionWithServiceByDefault() {
        verifyNoInteractions(fileStorageService);
    }
}
