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

    // ---------------------------------------------------------------------------
    // Happy-path: known fileType values
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "fileType={0} should produce filename={2}")
    @CsvSource({
        "sh,   sh000001,  000001.SH.csv",
        "sh,   sh000300,  000300.SH.csv",
        "sz,   sz399006,  399006.SZ.csv",
        "us,   dji,       DJI.csv",
        "us,   nasdaq,    NASDAQ.csv",
        "us,   sp500,     SP500.csv",
        "hk,   hsi,       HSI.csv",
        "hk,   hkah,      HKAH.csv"
    })
    @DisplayName("Known fileType values produce correct filenames and 200 OK")
    void uploadFile_knownFileType_returnsSuccess(String market, String fileType, String expectedFilename)
            throws Exception {

        doNothing().when(fileStorageService).storeFile(any(), eq(market), eq(expectedFilename));

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService, times(1)).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("fileType=moneygrow produces market-specific filename")
    void uploadFile_moneygrow_producesMarketSpecificFilename() throws Exception {
        var market = "CN";
        var expectedFilename = "MoneyGrow_CN.txt";

        doNothing().when(fileStorageService).storeFile(any(), eq(market), eq(expectedFilename));

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, "moneygrow", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    @Test
    @DisplayName("Unknown fileType falls back to '<fileType>.csv'")
    void uploadFile_unknownFileType_fallsBackToCsvName() throws Exception {
        var market = "us";
        var fileType = "customIndex";
        var expectedFilename = "customIndex.csv";

        doNothing().when(fileStorageService).storeFile(any(), eq(market), eq(expectedFilename));

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);
    }

    // ---------------------------------------------------------------------------
    // Error / exception path
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Service throws RuntimeException → 400 Bad Request with error status")
    void uploadFile_serviceThrowsException_returnsBadRequest() throws Exception {
        var market = "sh";
        var fileType = "sh000001";
        var errorMessage = "Disk full";

        doThrow(new RuntimeException(errorMessage))
                .when(fileStorageService).storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains(errorMessage);
    }

    @Test
    @DisplayName("Service throws checked Exception → 400 Bad Request with error status")
    void uploadFile_serviceThrowsCheckedException_returnsBadRequest() throws Exception {
        var market = "hk";
        var fileType = "hsi";
        var errorMessage = "IO failure";

        doThrow(new Exception(errorMessage))
                .when(fileStorageService).storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains(errorMessage);
    }

    // ---------------------------------------------------------------------------
    // Response structure / contract
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Success response always contains 'message' and 'status' keys")
    void uploadFile_successResponse_containsRequiredKeys() throws Exception {
        doNothing().when(fileStorageService).storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("us", "nasdaq", multipartFile);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull()
                        .containsKey("message")
                        .containsKey("status");
    }

    @Test
    @DisplayName("Error response always contains 'message' and 'status' keys")
    void uploadFile_errorResponse_containsRequiredKeys() throws Exception {
        doThrow(new RuntimeException("oops"))
                .when(fileStorageService).storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("us", "nasdaq", multipartFile);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull()
                        .containsKey("message")
                        .containsKey("status");
    }

    @Test
    @DisplayName("Success message contains the generated filename")
    void uploadFile_successMessage_containsFilename() throws Exception {
        doNothing().when(fileStorageService).storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("us", "sp500", multipartFile);

        assertThat(response.getBody())
                .isNotNull()
                .extractingByKey("message")
                .asString()
                .contains("SP500.csv");
    }

    @Test
    @DisplayName("moneygrow with empty market string still constructs filename correctly")
    void uploadFile_moneygrow_emptyMarket_constructsFilename() throws Exception {
        var market = "";
        var expectedFilename = "MoneyGrow_.txt";

        doNothing().when(fileStorageService).storeFile(any(), eq(market), eq(expectedFilename));

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, "moneygrow", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .extractingByKey("message")
                .asString()
                .contains(expectedFilename);
    }

    @Test
    @DisplayName("FileStorageService is called exactly once per successful upload")
    void uploadFile_callsStoreFileExactlyOnce() throws Exception {
        doNothing().when(fileStorageService).storeFile(any(), any(), any());

        fileUploadController.uploadFile("sh", "sh000300", multipartFile);

        verify(fileStorageService, times(1)).storeFile(multipartFile, "sh", "000300.SH.csv");
        verifyNoMoreInteractions(fileStorageService);
    }

    @Test
    @DisplayName("FileStorageService is NOT called when exception bubbles from storeFile")
    void uploadFile_afterException_noFurtherInteractions() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(fileStorageService).storeFile(any(), any(), any());

        fileUploadController.uploadFile("hk", "hkah", multipartFile);

        // storeFile was invoked once; no other method should be called
        verify(fileStorageService, times(1)).storeFile(any(), any(), any());
        verifyNoMoreInteractions(fileStorageService);
    }
}
