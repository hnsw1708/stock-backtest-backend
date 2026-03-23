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

    // ─── init() ───────────────────────────────────────────────────────────────

    @Test
    void init_createsAllMarketDirectories() {
        fileStorageService.init();

        assertThat(Path.of(cnDir)).isDirectory();
        assertThat(Path.of(usDir)).isDirectory();
        assertThat(Path.of(hkDir)).isDirectory();
    }

    @Test
    void init_isIdempotent_doesNotThrowWhenDirectoriesAlreadyExist() throws IOException {
        Files.createDirectories(Path.of(cnDir));
        Files.createDirectories(Path.of(usDir));
        Files.createDirectories(Path.of(hkDir));

        // Should not throw
        fileStorageService.init();

        assertThat(Path.of(cnDir)).isDirectory();
        assertThat(Path.of(usDir)).isDirectory();
        assertThat(Path.of(hkDir)).isDirectory();
    }

    @Test
    void init_throwsRuntimeException_whenDirectoryCreationFails() {
        // Point cnDir to an invalid path (a file used as a directory)
        ReflectionTestUtils.setField(fileStorageService, "cnDir", "\0invalid");

        assertThatThrownBy(() -> fileStorageService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not create upload directories");
    }

    // ─── storeFile() ──────────────────────────────────────────────────────────

    @Test
    void storeFile_cn_savesFileAndReturnsFileName() throws IOException {
        Files.createDirectories(Path.of(cnDir));
        byte[] content = "cn stock data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "cn", "test.csv");

        assertThat(result).isEqualTo("test.csv");
        assertThat(Files.readAllBytes(Path.of(cnDir).resolve("test.csv"))).isEqualTo(content);
    }

    @Test
    void storeFile_us_savesFileAndReturnsFileName() throws IOException {
        Files.createDirectories(Path.of(usDir));
        byte[] content = "us stock data".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = fileStorageService.storeFile(multipartFile, "us", "us_data.csv");

        assertThat(result).isEqualTo("us_data.csv");
        assertThat(Files.readAllBytes(Path.of(usDir).resolve("us_data.csv"))).isEqualTo(content);
    }

    @Test
    void storeFile_hk_savesFileAndReturnsFileName() throws IOException {
        Files.createDirectories(Path.of(hkDir));
        byte[] content = "hk stock data".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = fileStorageService.storeFile(multipartFile, "hk", "hk_data.csv");

        assertThat(result).isEqualTo("hk_data.csv");
        assertThat(Files.readAllBytes(Path.of(hkDir).resolve("hk_data.csv"))).isEqualTo(content);
    }

    @Test
    void storeFile_marketIsCaseInsensitive_CN_uppercase() throws IOException {
        Files.createDirectories(Path.of(cnDir));
        byte[] content = "data".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        String result = fileStorageService.storeFile(multipartFile, "CN", "file.csv");

        assertThat(result).isEqualTo("file.csv");
    }

    @Test
    void storeFile_replacesExistingFile() throws IOException {
        Files.createDirectories(Path.of(cnDir));
        Path existingFile = Path.of(cnDir).resolve("existing.csv");
        Files.writeString(existingFile, "old content");

        byte[] newContent = "new content".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(newContent));

        fileStorageService.storeFile(multipartFile, "cn", "existing.csv");

        assertThat(Files.readAllBytes(existingFile)).isEqualTo(newContent);
    }

    @Test
    void storeFile_throwsRuntimeException_whenInvalidMarket() throws IOException {
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "jp", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }

    @Test
    void storeFile_throwsRuntimeException_whenIOExceptionOccurs() throws IOException {
        Files.createDirectories(Path.of(cnDir));
        when(multipartFile.getInputStream()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "cn", "fail.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file fail.csv");
    }

    // ─── loadFile() ───────────────────────────────────────────────────────────

    @Test
    void loadFile_cn_returnsCorrectPath() {
        Path result = fileStorageService.loadFile("cn", "data.csv");

        assertThat(result).isEqualTo(Path.of(cnDir).resolve("data.csv"));
    }

    @Test
    void loadFile_us_returnsCorrectPath() {
        Path result = fileStorageService.loadFile("us", "us_data.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("us_data.csv"));
    }

    @Test
    void loadFile_hk_returnsCorrectPath() {
        Path result = fileStorageService.loadFile("hk", "hk_data.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("hk_data.csv"));
    }

    @Test
    void loadFile_marketIsCaseInsensitive_US_uppercase() {
        Path result = fileStorageService.loadFile("US", "file.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("file.csv"));
    }

    @Test
    void loadFile_throwsIllegalArgumentException_forUnknownMarket() {
        assertThatThrownBy(() -> fileStorageService.loadFile("xx", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: xx");
    }

    @Test
    void loadFile_throwsNullPointerException_whenMarketIsNull() {
        assertThatThrownBy(() -> fileStorageService.loadFile(null, "file.csv"))
                .isInstanceOf(NullPointerException.class);
    }

    // ─── fileExists() ─────────────────────────────────────────────────────────

    @Test
    void fileExists_returnsFalse_whenFileDoesNotExist() {
        boolean exists = fileStorageService.fileExists("cn", "missing.csv");

        assertThat(exists).isFalse();
    }

    @Test
    void fileExists_returnsTrue_whenFileExists() throws IOException {
        Files.createDirectories(Path.of(cnDir));
        Files.writeString(Path.of(cnDir).resolve("present.csv"), "data");

        boolean exists = fileStorageService.fileExists("cn", "present.csv");

        assertThat(exists).isTrue();
    }

    @Test
    void fileExists_returnsTrue_forUsMarket() throws IOException {
        Files.createDirectories(Path.of(usDir));
        Files.writeString(Path.of(usDir).resolve("us.csv"), "us data");

        assertThat(fileStorageService.fileExists("us", "us.csv")).isTrue();
    }

    @Test
    void fileExists_returnsTrue_forHkMarket() throws IOException {
        Files.createDirectories(Path.of(hkDir));
        Files.writeString(Path.of(hkDir).resolve("hk.csv"), "hk data");

        assertThat(fileStorageService.fileExists("hk", "hk.csv")).isTrue();
    }

    @Test
    void fileExists_returnsFalse_afterFileIsDeleted() throws IOException {
        Files.createDirectories(Path.of(cnDir));
        Path file = Path.of(cnDir).resolve("temp.csv");
        Files.writeString(file, "temp");
        Files.delete(file);

        assertThat(fileStorageService.fileExists("cn", "temp.csv")).isFalse();
    }

    @Test
    void fileExists_throwsIllegalArgumentException_forUnknownMarket() {
        assertThatThrownBy(() -> fileStorageService.fileExists("invalid", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: invalid");
    }

    // ─── market routing (parameterized) ───────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"cn", "CN", "Cn", "cN"})
    void loadFile_allCnCasingVariants_resolveToSameDirectory(String market) {
        Path result = fileStorageService.loadFile(market, "file.csv");

        assertThat(result).isEqualTo(Path.of(cnDir).resolve("file.csv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"us", "US", "Us"})
    void loadFile_allUsCasingVariants_resolveToSameDirectory(String market) {
        Path result = fileStorageService.loadFile(market, "file.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("file.csv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hk", "HK", "Hk"})
    void loadFile_allHkCasingVariants_resolveToSameDirectory(String market) {
        Path result = fileStorageService.loadFile(market, "file.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("file.csv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jp", "eu", "au", "", "  "})
    void loadFile_unsupportedMarkets_throwIllegalArgumentException(String market) {
        assertThatThrownBy(() -> fileStorageService.loadFile(market, "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market:");
    }
}
