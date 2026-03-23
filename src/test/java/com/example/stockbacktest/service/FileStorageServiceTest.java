package com.example.stockbacktest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private MultipartFile multipartFile;

    private FileStorageService fileStorageService;

    private String cnDir;
    private String usDir;
    private String hkDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();

        cnDir = tempDir.resolve("cn").toString();
        usDir = tempDir.resolve("us").toString();
        hkDir = tempDir.resolve("hk").toString();

        ReflectionTestUtils.setField(fileStorageService, "cnDir", cnDir);
        ReflectionTestUtils.setField(fileStorageService, "usDir", usDir);
        ReflectionTestUtils.setField(fileStorageService, "hkDir", hkDir);
    }

    // -------------------------------------------------------------------------
    // init()
    // -------------------------------------------------------------------------

    @Test
    void init_shouldCreateAllThreeMarketDirectories() {
        fileStorageService.init();

        assertThat(Path.of(cnDir)).exists().isDirectory();
        assertThat(Path.of(usDir)).exists().isDirectory();
        assertThat(Path.of(hkDir)).exists().isDirectory();
    }

    @Test
    void init_calledMultipleTimes_shouldNotThrow() {
        fileStorageService.init();
        fileStorageService.init(); // idempotent – createDirectories should not fail

        assertThat(Path.of(cnDir)).exists();
    }

    @Test
    void init_withInvalidPath_shouldThrowRuntimeException() {
        // Use a path that cannot be created (null character makes it invalid on all OSes)
        ReflectionTestUtils.setField(fileStorageService, "cnDir", "\0invalid");

        assertThatThrownBy(() -> fileStorageService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not create upload directories");
    }

    // -------------------------------------------------------------------------
    // storeFile()
    // -------------------------------------------------------------------------

    @Test
    void storeFile_withValidCnMarket_shouldWriteFileAndReturnFileName() throws IOException {
        fileStorageService.init();
        String fileName = "test-cn.csv";
        byte[] content = "col1,col2\n1,2".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "cn", fileName);

        assertThat(result).isEqualTo(fileName);
        assertThat(Path.of(cnDir).resolve(fileName)).exists();
        assertThat(Files.readAllBytes(Path.of(cnDir).resolve(fileName))).isEqualTo(content);
    }

    @Test
    void storeFile_withValidUsMarket_shouldWriteFile() throws IOException {
        fileStorageService.init();
        String fileName = "test-us.csv";
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());

        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "us", fileName);

        assertThat(result).isEqualTo(fileName);
        assertThat(Path.of(usDir).resolve(fileName)).exists();
    }

    @Test
    void storeFile_withValidHkMarket_shouldWriteFile() throws IOException {
        fileStorageService.init();
        String fileName = "test-hk.csv";
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());

        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "hk", fileName);

        assertThat(result).isEqualTo(fileName);
        assertThat(Path.of(hkDir).resolve(fileName)).exists();
    }

    @Test
    void storeFile_marketNameCaseInsensitive_shouldStoreSuccessfully() throws IOException {
        fileStorageService.init();
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "CN", "file.csv");

        assertThat(result).isEqualTo("file.csv");
        assertThat(Path.of(cnDir).resolve("file.csv")).exists();
    }

    @Test
    void storeFile_shouldReplaceExistingFile() throws IOException {
        fileStorageService.init();
        String fileName = "duplicate.csv";
        Path existing = Path.of(cnDir).resolve(fileName);
        Files.writeString(existing, "old content");

        byte[] newContent = "new content".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(newContent));

        fileStorageService.storeFile(multipartFile, "cn", fileName);

        assertThat(Files.readAllBytes(existing)).isEqualTo(newContent);
    }

    @Test
    void storeFile_withInvalidMarket_shouldThrowIllegalArgumentException() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "jp", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }

    @Test
    void storeFile_whenIOExceptionOccurs_shouldThrowRuntimeException() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "cn", "file.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file file.csv");
    }

    // -------------------------------------------------------------------------
    // loadFile()
    // -------------------------------------------------------------------------

    @Test
    void loadFile_cnMarket_shouldReturnCorrectPath() {
        Path result = fileStorageService.loadFile("cn", "data.csv");

        assertThat(result).isEqualTo(Path.of(cnDir).resolve("data.csv"));
    }

    @Test
    void loadFile_usMarket_shouldReturnCorrectPath() {
        Path result = fileStorageService.loadFile("us", "data.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("data.csv"));
    }

    @Test
    void loadFile_hkMarket_shouldReturnCorrectPath() {
        Path result = fileStorageService.loadFile("hk", "data.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("data.csv"));
    }

    @Test
    void loadFile_withInvalidMarket_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> fileStorageService.loadFile("xx", "data.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: xx");
    }

    @Test
    void loadFile_withUpperCaseMarket_shouldResolveCorrectly() {
        Path result = fileStorageService.loadFile("US", "report.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("report.csv"));
    }

    @Test
    void loadFile_withMixedCaseMarket_shouldResolveCorrectly() {
        Path result = fileStorageService.loadFile("Hk", "report.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("report.csv"));
    }

    // -------------------------------------------------------------------------
    // fileExists()
    // -------------------------------------------------------------------------

    @Test
    void fileExists_whenFileDoesExist_shouldReturnTrue() throws IOException {
        fileStorageService.init();
        String fileName = "existing.csv";
        Files.writeString(Path.of(cnDir).resolve(fileName), "content");

        boolean result = fileStorageService.fileExists("cn", fileName);

        assertThat(result).isTrue();
    }

    @Test
    void fileExists_whenFileDoesNotExist_shouldReturnFalse() {
        fileStorageService.init();

        boolean result = fileStorageService.fileExists("cn", "ghost.csv");

        assertThat(result).isFalse();
    }

    @Test
    void fileExists_withInvalidMarket_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> fileStorageService.fileExists("de", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: de");
    }

    @ParameterizedTest
    @ValueSource(strings = {"cn", "us", "hk", "CN", "US", "HK", "Cn", "Us", "Hk"})
    void fileExists_allSupportedMarketsAndCases_shouldNotThrowOnMissingFile(String market) {
        fileStorageService.init();

        boolean result = fileStorageService.fileExists(market, "nonexistent.csv");

        assertThat(result).isFalse();
    }

    @Test
    void fileExists_afterStoringFile_shouldReturnTrue() throws IOException {
        fileStorageService.init();
        String fileName = "stored.csv";
        when(multipartFile.getInputStream()).thenReturn(
                new ByteArrayInputStream("payload".getBytes()));

        fileStorageService.storeFile(multipartFile, "us", fileName);

        assertThat(fileStorageService.fileExists("us", fileName)).isTrue();
    }
}
