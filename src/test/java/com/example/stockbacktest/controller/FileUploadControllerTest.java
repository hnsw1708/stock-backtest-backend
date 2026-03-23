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
@DisplayName("FileUploadController Unit Tests")
class FileUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileUploadController fileUploadController;

    // ---------------------------------------------------------------------------
    // Happy-path: known fileType values mapped to expected filenames
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "fileType={0} market={1} -> filename={2}")
    @CsvSource({
        "sh000001, CN,   000001.SH.csv",
        "sh000300, CN,   000300.SH.csv",
        "sz399006, CN,   399006.SZ.csv",
        "dji,      US,   DJI.csv",
        "nasdaq,   US,   NASDAQ.csv",
        "sp500,    US,   SP500.csv",
        "hsi,      HK,   HSI.csv",
        "hkah,     HK,   HKAH.csv"
    })
    @DisplayName("Known fileTypes should resolve to the correct filename and return 200 OK")
    void uploadFile_knownFileType_returnsOkWithCorrectFilename(
            String fileType, String market, String expectedFilename) throws Exception {

        // when
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("success");
        assertThat(body.get("message")).contains(expectedFilename);

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq(expectedFilename));
    }

    @Test
    @DisplayName("fileType=moneygrow should embed market in filename")
    void uploadFile_moneyGrowFileType_embedsMarketInFilename() throws Exception {
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
    @DisplayName("Unknown fileType should fall back to '<fileType>.csv'")
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

        verify(fileStorageService).storeFile(eq(multipartFile), eq(market), eq(expectedFilename));
    }

    // ---------------------------------------------------------------------------
    // moneygrow with different markets
    // ---------------------------------------------------------------------------

    @ParameterizedTest(name = "moneygrow market={0} -> filename=MoneyGrow_{0}.txt")
    @CsvSource({"CN", "US", "HK", "JP"})
    @DisplayName("moneygrow fileType should always embed the given market")
    void uploadFile_moneyGrow_differentMarkets(String market) throws Exception {
        String expectedFilename = "MoneyGrow_" + market + ".txt";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, "moneygrow", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");
        assertThat(response.getBody()).containsEntry("message",
                "File uploaded successfully: " + expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    // ---------------------------------------------------------------------------
    // Error / exception path
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("When storeFile throws, response should be 400 with error status")
    void uploadFile_serviceThrows_returnsBadRequest() throws Exception {
        String market = "CN";
        String fileType = "sh000001";
        String errorMessage = "Disk quota exceeded";

        doThrow(new RuntimeException(errorMessage))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains("File upload failed:");
        assertThat(body.get("message")).contains(errorMessage);
    }

    @Test
    @DisplayName("When storeFile throws an IOException, response should still be 400")
    void uploadFile_serviceThrowsIOException_returnsBadRequest() throws Exception {
        doThrow(new java.io.IOException("Storage unavailable"))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("US", "nasdaq", multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("error");
        assertThat(body.get("message")).contains("Storage unavailable");
    }

    // ---------------------------------------------------------------------------
    // Response body structure
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Successful upload response must contain both 'message' and 'status' keys")
    void uploadFile_success_responseContainsBothKeys() throws Exception {
        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "sh000300", multipartFile);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKeys("message", "status");
        assertThat(body).hasSize(2);
    }

    @Test
    @DisplayName("Error response must contain both 'message' and 'status' keys")
    void uploadFile_error_responseContainsBothKeys() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(fileStorageService)
                .storeFile(any(), any(), any());

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile("CN", "sh000001", multipartFile);

        Map<String, String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKeys("message", "status");
        assertThat(body).hasSize(2);
    }

    // ---------------------------------------------------------------------------
    // Verify storeFile is called with correct market argument
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("storeFile must be invoked with the market path variable")
    void uploadFile_passesMarketToService() throws Exception {
        String market = "HK";
        String fileType = "hsi";

        fileUploadController.uploadFile(market, fileType, multipartFile);

        verify(fileStorageService).storeFile(multipartFile, "HK", "HSI.csv");
        verifyNoMoreInteractions(fileStorageService);
    }

    @Test
    @DisplayName("storeFile must be invoked with the MultipartFile provided by the caller")
    void uploadFile_passesMultipartFileToService() throws Exception {
        fileUploadController.uploadFile("US", "sp500", multipartFile);

        verify(fileStorageService).storeFile(eq(multipartFile), any(), any());
    }

    // ---------------------------------------------------------------------------
    // Edge-case: empty string fileType -> default branch
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Empty fileType string should fall back to default pattern '<fileType>.csv'")
    void uploadFile_emptyFileType_usesDefaultPattern() throws Exception {
        String market = "CN";
        String fileType = "";
        String expectedFilename = ".csv";

        ResponseEntity<Map<String, String>> response =
                fileUploadController.uploadFile(market, fileType, multipartFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("message", "File uploaded successfully: " + expectedFilename);

        verify(fileStorageService).storeFile(multipartFile, market, expectedFilename);
    }

    // ---------------------------------------------------------------------------
    // Verify storeFile is NOT called when exception occurs before service
    // (service throws on first call - just confirm error path is isolated)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Exactly one storeFile call even on success")
    void uploadFile_success_storeFileCalledExactlyOnce() throws Exception {
        fileUploadController.uploadFile("US", "dji", multipartFile);

        verify(fileStorageService, times(1)).storeFile(any(), any(), any());
    }
}
