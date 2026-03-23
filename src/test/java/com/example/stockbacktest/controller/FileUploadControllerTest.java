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
@DisplayName("FileUploadController Tests")
class FileUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileUploadController fileUploadController;

    // --- Success path tests ---

    @Test
    @DisplayName("Upload file returns 200 OK with success status")
    void uploadFile_success_returns200() throws Exception {
        // Arrange
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq("cn"), eq("000001.SH.csv"));

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("cn", "sh000001", multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("success");
        assertThat(response.getBody().get("message")).contains("000001.SH.csv");
    }

    @Test
    @DisplayName("Upload file delegates to FileStorageService with correct arguments")
    void uploadFile_delegatesToService() throws Exception {
        // Arrange
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq("us"), eq("NASDAQ.csv"));

        // Act
        fileUploadController.uploadFile("us", "nasdaq", multipartFile);

        // Assert
        verify(fileStorageService, times(1)).storeFile(multipartFile, "us", "NASDAQ.csv");
    }

    // --- Filename generation via uploadFile (public API) ---

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
    @DisplayName("Known fileType values produce correct fixed filenames")
    void uploadFile_knownFileTypes_correctFilename(String fileType, String expectedFilename) throws Exception {
        // Arrange
        doNothing().when(fileStorageService).storeFile(any(), any(), eq(expectedFilename.strip()));

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("anyMarket", fileType.strip(), multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains(expectedFilename.strip());
        verify(fileStorageService).storeFile(multipartFile, "anyMarket", expectedFilename.strip());
    }

    @Test
    @DisplayName("fileType=moneygrow produces filename containing market")
    void uploadFile_moneygrowFileType_includesMarketInFilename() throws Exception {
        // Arrange
        String market = "cn";
        String expectedFilename = "MoneyGrow_" + market + ".txt";
        doNothing().when(fileStorageService).storeFile(any(), eq(market), eq(expectedFilename));

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, "moneygrow", multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("message")).contains(expectedFilename);
        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("Unknown fileType falls back to fileType+'.csv'")
    void uploadFile_unknownFileType_fallbackToCsv() throws Exception {
        // Arrange
        String market = "eu";
        String fileType = "unknownIndex";
        String expectedFilename = fileType + ".csv";
        doNothing().when(fileStorageService).storeFile(any(), eq(market), eq(expectedFilename));

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("success");
        assertThat(response.getBody().get("message")).contains(expectedFilename);
    }

    // --- Error / exception handling ---

    @Test
    @DisplayName("When service throws exception, returns 400 Bad Request with error status")
    void uploadFile_serviceThrows_returns400() throws Exception {
        // Arrange
        String exceptionMessage = "Disk full";
        doThrow(new RuntimeException(exceptionMessage))
                .when(fileStorageService).storeFile(any(), any(), any());

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("cn", "sh000001", multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("error");
        assertThat(response.getBody().get("message")).contains(exceptionMessage);
    }

    @Test
    @DisplayName("Error response message contains 'File upload failed' prefix")
    void uploadFile_serviceThrows_errorMessagePrefix() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("bad input"))
                .when(fileStorageService).storeFile(any(), any(), any());

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("us", "sp500", multipartFile);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).startsWith("File upload failed:");
    }

    @Test
    @DisplayName("Success response message contains 'File uploaded successfully' prefix")
    void uploadFile_success_messagePrefix() throws Exception {
        // Arrange
        doNothing().when(fileStorageService).storeFile(any(), any(), any());

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("hk", "hsi", multipartFile);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).startsWith("File uploaded successfully:");
    }

    @Test
    @DisplayName("Response body always contains both 'message' and 'status' keys on success")
    void uploadFile_success_responseContainsBothKeys() throws Exception {
        // Arrange
        doNothing().when(fileStorageService).storeFile(any(), any(), any());

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("cn", "sh000300", multipartFile);

        // Assert
        assertThat(response.getBody()).containsKeys("message", "status");
    }

    @Test
    @DisplayName("Response body always contains both 'message' and 'status' keys on failure")
    void uploadFile_failure_responseContainsBothKeys() throws Exception {
        // Arrange
        doThrow(new RuntimeException("oops"))
                .when(fileStorageService).storeFile(any(), any(), any());

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("cn", "dji", multipartFile);

        // Assert
        assertThat(response.getBody()).containsKeys("message", "status");
    }

    @Test
    @DisplayName("moneygrow with different market values produces distinct filenames")
    void uploadFile_moneygrow_differentMarkets_distinctFilenames() throws Exception {
        // Arrange
        doNothing().when(fileStorageService).storeFile(any(), any(), any());

        // Act
        ResponseEntity<Map<String, String>> responseCn =
                fileUploadController.uploadFile("cn", "moneygrow", multipartFile);
        ResponseEntity<Map<String, String>> responseUs =
                fileUploadController.uploadFile("us", "moneygrow", multipartFile);

        // Assert
        assertThat(responseCn.getBody().get("message")).contains("MoneyGrow_cn.txt");
        assertThat(responseUs.getBody().get("message")).contains("MoneyGrow_us.txt");
    }

    @Test
    @DisplayName("Service is called exactly once per upload request")
    void uploadFile_serviceCalledExactlyOnce() throws Exception {
        // Arrange
        doNothing().when(fileStorageService).storeFile(any(), any(), any());

        // Act
        fileUploadController.uploadFile("cn", "hkah", multipartFile);

        // Assert
        verify(fileStorageService, times(1)).storeFile(any(), any(), any());
    }

    @Test
    @DisplayName("When service throws checked-like RuntimeException with null message, response handles it gracefully")
    void uploadFile_serviceThrowsNullMessageException_handledGracefully() throws Exception {
        // Arrange
        doThrow(new RuntimeException((String) null))
                .when(fileStorageService).storeFile(any(), any(), any());

        // Act
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("cn", "sz399006", multipartFile);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("status")).isEqualTo("error");
        // message should exist even if exception message is null
        assertThat(response.getBody()).containsKey("message");
    }
}
