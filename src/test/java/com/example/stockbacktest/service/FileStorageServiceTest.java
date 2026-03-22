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
    void init_shouldCreateAllThreeDirectories() {
        fileStorageService.init();

        assertThat(Path.of(cnDir)).isDirectory();
        assertThat(Path.of(usDir)).isDirectory();
        assertThat(Path.of(hkDir)).isDirectory();
    }

    @Test
    void init_shouldBeIdempotent_whenDirectoriesAlreadyExist() {
        fileStorageService.init();
        // calling init a second time must not throw
        fileStorageService.init();

        assertThat(Path.of(cnDir)).isDirectory();
        assertThat(Path.of(usDir)).isDirectory();
        assertThat(Path.of(hkDir)).isDirectory();
    }

    @Test
    void init_shouldThrowRuntimeException_whenDirectoryCreationFails() {
        // Point cnDir to an invalid path that cannot be created on most OS
        // (a file is placed where the directory should be)
        Path blocker = tempDir.resolve("blocker");
        try {
            Files.createFile(blocker);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Sub-path inside a regular file is not creatable
        ReflectionTestUtils.setField(fileStorageService, "cnDir", blocker.resolve("subdir").toString());

        assertThatThrownBy(() -> fileStorageService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not create upload directories");
    }

    // -------------------------------------------------------------------------
    // storeFile()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"cn", "CN", "Cn"})
    void storeFile_shouldStoreFileInCnDirectory_caseInsensitive(String market) throws IOException {
        fileStorageService.init();
        byte[] content = "cn stock data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, market, "test.csv");

        assertThat(result).isEqualTo("test.csv");
        assertThat(Files.readAllBytes(Path.of(cnDir).resolve("test.csv"))).isEqualTo(content);
    }

    @ParameterizedTest
    @ValueSource(strings = {"us", "US", "Us"})
    void storeFile_shouldStoreFileInUsDirectory_caseInsensitive(String market) throws IOException {
        fileStorageService.init();
        byte[] content = "us stock data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, market, "us_test.csv");

        assertThat(result).isEqualTo("us_test.csv");
        assertThat(Files.readAllBytes(Path.of(usDir).resolve("us_test.csv"))).isEqualTo(content);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hk", "HK", "Hk"})
    void storeFile_shouldStoreFileInHkDirectory_caseInsensitive(String market) throws IOException {
        fileStorageService.init();
        byte[] content = "hk stock data".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, market, "hk_test.csv");

        assertThat(result).isEqualTo("hk_test.csv");
        assertThat(Files.readAllBytes(Path.of(hkDir).resolve("hk_test.csv"))).isEqualTo(content);
    }

    @Test
    void storeFile_shouldOverwriteExistingFile() throws IOException {
        fileStorageService.init();

        // First write
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("old content".getBytes()));
        fileStorageService.storeFile(multipartFile, "cn", "overwrite.csv");

        // Second write (overwrite)
        byte[] newContent = "new content".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(newContent));
        fileStorageService.storeFile(multipartFile, "cn", "overwrite.csv");

        assertThat(Files.readAllBytes(Path.of(cnDir).resolve("overwrite.csv"))).isEqualTo(newContent);
    }

    @Test
    void storeFile_shouldThrowRuntimeException_whenIoExceptionOccurs() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "cn", "fail.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file fail.csv")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void storeFile_shouldThrowIllegalArgumentException_forUnknownMarket() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "jp", "any.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }

    // -------------------------------------------------------------------------
    // loadFile()
    // -------------------------------------------------------------------------

    @Test
    void loadFile_shouldReturnCorrectPath_forCnMarket() {
        Path result = fileStorageService.loadFile("cn", "data.csv");

        assertThat(result).isEqualTo(Path.of(cnDir).resolve("data.csv"));
    }

    @Test
    void loadFile_shouldReturnCorrectPath_forUsMarket() {
        Path result = fileStorageService.loadFile("us", "data.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("data.csv"));
    }

    @Test
    void loadFile_shouldReturnCorrectPath_forHkMarket() {
        Path result = fileStorageService.loadFile("hk", "data.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("data.csv"));
    }

    @Test
    void loadFile_shouldThrowIllegalArgumentException_forInvalidMarket() {
        assertThatThrownBy(() -> fileStorageService.loadFile("xx", "data.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: xx");
    }

    @Test
    void loadFile_shouldBeCaseInsensitive_forMarket() {
        Path lower = fileStorageService.loadFile("cn", "file.csv");
        Path upper = fileStorageService.loadFile("CN", "file.csv");
        Path mixed = fileStorageService.loadFile("Cn", "file.csv");

        assertThat(lower).isEqualTo(upper).isEqualTo(mixed);
    }

    // -------------------------------------------------------------------------
    // fileExists()
    // -------------------------------------------------------------------------

    @Test
    void fileExists_shouldReturnTrue_whenFileExistsOnDisk() throws IOException {
        fileStorageService.init();
        Path target = Path.of(cnDir).resolve("existing.csv");
        Files.writeString(target, "some data");

        assertThat(fileStorageService.fileExists("cn", "existing.csv")).isTrue();
    }

    @Test
    void fileExists_shouldReturnFalse_whenFileDoesNotExist() {
        fileStorageService.init();

        assertThat(fileStorageService.fileExists("cn", "nonexistent.csv")).isFalse();
    }

    @Test
    void fileExists_shouldReturnFalse_forUsMarket_whenFileAbsent() {
        fileStorageService.init();

        assertThat(fileStorageService.fileExists("us", "missing.csv")).isFalse();
    }

    @Test
    void fileExists_shouldReturnTrue_forHkMarket_whenFilePresent() throws IOException {
        fileStorageService.init();
        Path target = Path.of(hkDir).resolve("hk_data.csv");
        Files.writeString(target, "hk data");

        assertThat(fileStorageService.fileExists("hk", "hk_data.csv")).isTrue();
    }

    @Test
    void fileExists_shouldThrowIllegalArgumentException_forInvalidMarket() {
        assertThatThrownBy(() -> fileStorageService.fileExists("de", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: de");
    }

    @Test
    void fileExists_shouldBeCaseInsensitive_forMarketParameter() throws IOException {
        fileStorageService.init();
        Path target = Path.of(usDir).resolve("spy.csv");
        Files.writeString(target, "spy data");

        assertThat(fileStorageService.fileExists("US", "spy.csv")).isTrue();
        assertThat(fileStorageService.fileExists("us", "spy.csv")).isTrue();
        assertThat(fileStorageService.fileExists("Us", "spy.csv")).isTrue();
    }
}
