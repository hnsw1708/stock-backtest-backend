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

    // -----------------------------------------------------------------------
    // init()
    // -----------------------------------------------------------------------

    @Test
    void init_shouldCreateAllMarketDirectories() {
        fileStorageService.init();

        assertThat(Path.of(cnDir)).isDirectory();
        assertThat(Path.of(usDir)).isDirectory();
        assertThat(Path.of(hkDir)).isDirectory();
    }

    @Test
    void init_calledTwice_shouldNotThrow() {
        fileStorageService.init();
        // calling a second time must be idempotent (createDirectories is idempotent)
        fileStorageService.init();

        assertThat(Path.of(cnDir)).isDirectory();
    }

    @Test
    void init_withInvalidPath_shouldThrowRuntimeException() {
        // Use a path that cannot be created (null byte in path)
        ReflectionTestUtils.setField(fileStorageService, "cnDir", "\0invalid");

        assertThatThrownBy(() -> fileStorageService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not create upload directories");
    }

    // -----------------------------------------------------------------------
    // storeFile()
    // -----------------------------------------------------------------------

    @Test
    void storeFile_cnMarket_shouldWriteFileAndReturnFileName() throws IOException {
        fileStorageService.init();
        var content = "date,open,close\n2024-01-01,100,110";
        InputStream is = new ByteArrayInputStream(content.getBytes());
        when(multipartFile.getInputStream()).thenReturn(is);

        String result = fileStorageService.storeFile(multipartFile, "cn", "stock.csv");

        assertThat(result).isEqualTo("stock.csv");
        assertThat(Files.readString(Path.of(cnDir).resolve("stock.csv"))).isEqualTo(content);
    }

    @Test
    void storeFile_usMarket_shouldWriteFileAndReturnFileName() throws IOException {
        fileStorageService.init();
        InputStream is = new ByteArrayInputStream("data".getBytes());
        when(multipartFile.getInputStream()).thenReturn(is);

        String result = fileStorageService.storeFile(multipartFile, "us", "nasdaq.csv");

        assertThat(result).isEqualTo("nasdaq.csv");
        assertThat(Path.of(usDir).resolve("nasdaq.csv")).exists();
    }

    @Test
    void storeFile_hkMarket_shouldWriteFileAndReturnFileName() throws IOException {
        fileStorageService.init();
        InputStream is = new ByteArrayInputStream("hk data".getBytes());
        when(multipartFile.getInputStream()).thenReturn(is);

        String result = fileStorageService.storeFile(multipartFile, "hk", "hsi.csv");

        assertThat(result).isEqualTo("hsi.csv");
        assertThat(Path.of(hkDir).resolve("hsi.csv")).exists();
    }

    @Test
    void storeFile_marketCaseInsensitive_uppercaseShouldWork() throws IOException {
        fileStorageService.init();
        InputStream is = new ByteArrayInputStream("data".getBytes());
        when(multipartFile.getInputStream()).thenReturn(is);

        String result = fileStorageService.storeFile(multipartFile, "CN", "upper.csv");

        assertThat(result).isEqualTo("upper.csv");
        assertThat(Path.of(cnDir).resolve("upper.csv")).exists();
    }

    @Test
    void storeFile_shouldReplaceExistingFile() throws IOException {
        fileStorageService.init();
        Path target = Path.of(cnDir).resolve("existing.csv");
        Files.writeString(target, "old content");

        InputStream is = new ByteArrayInputStream("new content".getBytes());
        when(multipartFile.getInputStream()).thenReturn(is);

        fileStorageService.storeFile(multipartFile, "cn", "existing.csv");

        assertThat(Files.readString(target)).isEqualTo("new content");
    }

    @Test
    void storeFile_invalidMarket_shouldThrowIllegalArgumentException() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "jp", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }

    @Test
    void storeFile_ioExceptionFromMultipartFile_shouldThrowRuntimeException() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "cn", "fail.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file fail.csv");
    }

    // -----------------------------------------------------------------------
    // loadFile()
    // -----------------------------------------------------------------------

    @Test
    void loadFile_cnMarket_shouldReturnCorrectPath() {
        Path result = fileStorageService.loadFile("cn", "stock.csv");

        assertThat(result).isEqualTo(Path.of(cnDir).resolve("stock.csv"));
    }

    @Test
    void loadFile_usMarket_shouldReturnCorrectPath() {
        Path result = fileStorageService.loadFile("us", "nasdaq.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("nasdaq.csv"));
    }

    @Test
    void loadFile_hkMarket_shouldReturnCorrectPath() {
        Path result = fileStorageService.loadFile("hk", "hsi.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("hsi.csv"));
    }

    @Test
    void loadFile_invalidMarket_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> fileStorageService.loadFile("xx", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: xx");
    }

    @Test
    void loadFile_marketCaseInsensitive_mixedCaseShouldWork() {
        Path result = fileStorageService.loadFile("HK", "hsi.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("hsi.csv"));
    }

    // -----------------------------------------------------------------------
    // fileExists()
    // -----------------------------------------------------------------------

    @Test
    void fileExists_whenFilePresent_shouldReturnTrue() throws IOException {
        fileStorageService.init();
        Path file = Path.of(cnDir).resolve("present.csv");
        Files.writeString(file, "content");

        boolean exists = fileStorageService.fileExists("cn", "present.csv");

        assertThat(exists).isTrue();
    }

    @Test
    void fileExists_whenFileAbsent_shouldReturnFalse() {
        fileStorageService.init();

        boolean exists = fileStorageService.fileExists("us", "missing.csv");

        assertThat(exists).isFalse();
    }

    @Test
    void fileExists_invalidMarket_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> fileStorageService.fileExists("de", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: de");
    }

    @ParameterizedTest
    @ValueSource(strings = {"cn", "CN", "Cn", "cN"})
    void fileExists_cnMarketVariousCase_whenFileAbsent_shouldReturnFalse(String market) {
        fileStorageService.init();

        boolean exists = fileStorageService.fileExists(market, "nonexistent.csv");

        assertThat(exists).isFalse();
    }

    @Test
    void fileExists_afterStoringFile_shouldReturnTrue() throws IOException {
        fileStorageService.init();
        InputStream is = new ByteArrayInputStream("row1,row2".getBytes());
        when(multipartFile.getInputStream()).thenReturn(is);

        fileStorageService.storeFile(multipartFile, "hk", "stored.csv");

        assertThat(fileStorageService.fileExists("hk", "stored.csv")).isTrue();
    }
}
