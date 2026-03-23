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
@DisplayName("FileUploadController Tests")
class FileUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileUploadController fileUploadController;

    // -------------------------------------------------------------------------
    // Happy-path: known fileType mappings
    // -------------------------------------------------------------------------

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
    @DisplayName("Known fileType values should map to correct filenames")
    void uploadFile_knownFileTypes_shouldReturnSuccessWithCorrectFilename(
            String fileType, String expectedFilename) throws Exception {

        // given
        String market = "CN";
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq(market), eq(expectedFilename));

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("fileType=moneygrow should embed market in filename")
    void uploadFile_moneygrowFileType_shouldEmbedMarketInFilename() throws Exception {
        // given
        String market = "US";
        String fileType = "moneygrow";
        String expectedFilename = "MoneyGrow_US.txt";
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq(market), eq(expectedFilename));

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);
        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("moneygrow with different market values should reflect the market in filename")
    void uploadFile_moneygrowFileType_differentMarkets_shouldReflectMarket() throws Exception {
        for (String market : new String[]{"HK", "CN", "EU"}) {
            String expectedFilename = "MoneyGrow_" + market + ".txt";
            doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq(market), eq(expectedFilename));

            ResponseEntity<Map<String, String>> response =
                    fileUploadController.uploadFile(market, "moneygrow", multipartFile);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("message")).contains(expectedFilename);
        }
    }

    // -------------------------------------------------------------------------
    // Default / unknown fileType
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unknown fileType should fall back to <fileType>.csv")
    void uploadFile_unknownFileType_shouldFallbackToCsvFilename() throws Exception {
        // given
        String market = "US";
        String fileType = "customIndex";
        String expectedFilename = "customIndex.csv";
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq(market), eq(expectedFilename));

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);
        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("Empty-string fileType should produce '.csv' as filename")
    void uploadFile_emptyFileType_shouldProduceJustDotCsv() throws Exception {
        // given
        String market = "US";
        String fileType = "";
        String expectedFilename = ".csv";
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq(market), eq(expectedFilename));

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("success");
        assertThat(response.getBody().get("message")).contains(expectedFilename);
    }

    // -------------------------------------------------------------------------
    // Error / exception handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("When storeFile throws, controller should return 400 with error status")
    void uploadFile_storageThrows_shouldReturnBadRequest() throws Exception {
        // given
        String market = "CN";
        String fileType = "sh000001";
        String errorMessage = "Disk full";
        doThrow(new RuntimeException(errorMessage))
                .when(fileStorageService).storeFile(any(MultipartFile.class), anyString(), anyString());

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains(errorMessage);
    }

    @Test
    @DisplayName("When storeFile throws IOException, controller should return 400 with error status")
    void uploadFile_storageThrowsIOException_shouldReturnBadRequest() throws Exception {
        // given
        String market = "US";
        String fileType = "nasdaq";
        String errorMessage = "I/O error";
        doThrow(new java.io.IOException(errorMessage))
                .when(fileStorageService).storeFile(any(MultipartFile.class), anyString(), anyString());

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains(errorMessage);
    }

    @Test
    @DisplayName("Error response body should contain 'File upload failed:' prefix")
    void uploadFile_storageThrows_errorMessageShouldContainPrefix() throws Exception {
        // given
        doThrow(new RuntimeException("Something went wrong"))
                .when(fileStorageService).storeFile(any(MultipartFile.class), anyString(), anyString());

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("HK", "hsi", multipartFile);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).startsWith("File upload failed:");
    }

    // -------------------------------------------------------------------------
    // Response structure validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Successful response should contain both 'message' and 'status' keys")
    void uploadFile_success_responseShouldContainBothKeys() throws Exception {
        // given
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), anyString(), anyString());

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "sh000300", multipartFile);

        // then
        assertThat(response.getBody())
                .isNotNull()
                .containsKeys("message", "status");
    }

    @Test
    @DisplayName("Error response should contain both 'message' and 'status' keys")
    void uploadFile_error_responseShouldContainBothKeys() throws Exception {
        // given
        doThrow(new RuntimeException("fail"))
                .when(fileStorageService).storeFile(any(MultipartFile.class), anyString(), anyString());

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "sh000001", multipartFile);

        // then
        assertThat(response.getBody())
                .isNotNull()
                .containsKeys("message", "status");
    }

    @Test
    @DisplayName("Success message should contain 'File uploaded successfully:' prefix")
    void uploadFile_success_messageShouldContainSuccessPrefix() throws Exception {
        // given
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), anyString(), anyString());

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "dji", multipartFile);

        // then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).startsWith("File uploaded successfully:");
    }

    // -------------------------------------------------------------------------
    // Verify fileStorageService interactions
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("storeFile should be called exactly once on success")
    void uploadFile_success_shouldCallStoreFileExactlyOnce() throws Exception {
        // given
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), anyString(), anyString());

        // when
        fileUploadController.uploadFile("CN", "sz399006", multipartFile);

        // then
        verify(fileStorageService, times(1)).storeFile(any(MultipartFile.class), anyString(), anyString());
    }

    @Test
    @DisplayName("storeFile should receive the correct market path variable")
    void uploadFile_shouldPassCorrectMarketToStorageService() throws Exception {
        // given
        String market = "HK";
        doNothing().when(fileStorageService).storeFile(any(MultipartFile.class), eq(market), anyString());

        // when
        fileUploadController.uploadFile(market, "hkah", multipartFile);

        // then
        verify(fileStorageService).storeFile(multipartFile, market, "HKAH.csv");
    }

    @Test
    @DisplayName("storeFile should receive the correct MultipartFile instance")
    void uploadFile_shouldPassCorrectMultipartFileToStorageService() throws Exception {
        // given
        doNothing().when(fileStorageService).storeFile(eq(multipartFile), anyString(), anyString());

        // when
        fileUploadController.uploadFile("US", "sp500", multipartFile);

        // then
        verify(fileStorageService).storeFile(eq(multipartFile), anyString(), anyString());
    }
}
