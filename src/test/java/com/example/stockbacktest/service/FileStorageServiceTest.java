package com.example.stockbacktest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MultipartFile multipartFile;

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

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
    void init_shouldBeIdempotent_whenDirectoriesAlreadyExist() {
        fileStorageService.init();
        // calling a second time must not throw
        fileStorageService.init();

        assertThat(Path.of(cnDir)).exists();
        assertThat(Path.of(usDir)).exists();
        assertThat(Path.of(hkDir)).exists();
    }

    @Test
    void init_shouldThrowRuntimeException_whenDirectoryCannotBeCreated() {
        // Point cnDir to an invalid path (a file acting as a parent directory)
        // by using a path that contains an existing file as a path component.
        // We achieve this by using a non-writable root on Linux (/proc/...) or
        // a very long invalid path segment across platforms.
        // Simpler approach: make cnDir a child of a file (not a directory).
        Path fileAsParent = tempDir.resolve("not-a-dir.txt");
        try {
            Files.writeString(fileAsParent, "content");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Use the file as the parent directory — this will fail
        ReflectionTestUtils.setField(fileStorageService, "cnDir",
                fileAsParent.resolve("cn").toString());

        assertThatThrownBy(() -> fileStorageService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not create upload directories");
    }

    // -------------------------------------------------------------------------
    // storeFile()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "cn, test-cn.csv",
            "CN, test-cn-upper.csv",
            "us, test-us.csv",
            "US, test-us-upper.csv",
            "hk, test-hk.csv",
            "HK, test-hk-upper.csv"
    })
    void storeFile_shouldReturnFileName_forValidMarkets(String market, String fileName) throws IOException {
        fileStorageService.init();

        var content = """
                date,open,close
                2024-01-01,100.0,105.0
                """;
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, market, fileName);

        assertThat(result).isEqualTo(fileName);
    }

    @Test
    void storeFile_shouldPersistFileContent_inCorrectDirectory() throws IOException {
        fileStorageService.init();

        var content = """
                date,open,close
                2024-06-01,200.0,210.0
                """;
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        fileStorageService.storeFile(multipartFile, "cn", "data.csv");

        Path stored = Path.of(cnDir).resolve("data.csv");
        assertThat(stored).exists();
        assertThat(Files.readString(stored)).isEqualTo(content);
    }

    @Test
    void storeFile_shouldReplaceExistingFile() throws IOException {
        fileStorageService.init();
        Path existing = Path.of(usDir).resolve("data.csv");
        Files.writeString(existing, "old content");

        var newContent = "new content";
        InputStream inputStream = new ByteArrayInputStream(newContent.getBytes());
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        fileStorageService.storeFile(multipartFile, "us", "data.csv");

        assertThat(Files.readString(existing)).isEqualTo(newContent);
    }

    @Test
    void storeFile_shouldThrowRuntimeException_whenIOExceptionOccurs() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenThrow(new IOException("Disk full"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "hk", "fail.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file fail.csv")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void storeFile_shouldThrowIllegalArgumentException_forInvalidMarket() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenReturn(InputStream.nullInputStream());

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "jp", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }

    // -------------------------------------------------------------------------
    // loadFile()
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "cn, report.csv",
            "us, report.csv",
            "hk, report.csv"
    })
    void loadFile_shouldReturnCorrectPath_forValidMarkets(String market, String filename) {
        fileStorageService.init();

        Path result = fileStorageService.loadFile(market, filename);

        String expectedDir = switch (market) {
            case "cn" -> cnDir;
            case "us" -> usDir;
            case "hk" -> hkDir;
            default -> throw new IllegalArgumentException("Unexpected market: " + market);
        };
        assertThat(result).isEqualTo(Path.of(expectedDir).resolve(filename));
    }

    @Test
    void loadFile_shouldHandleCaseInsensitiveMarket() {
        fileStorageService.init();

        Path lowerCase = fileStorageService.loadFile("cn", "data.csv");
        Path upperCase = fileStorageService.loadFile("CN", "data.csv");

        assertThat(lowerCase).isEqualTo(upperCase);
    }

    @Test
    void loadFile_shouldThrowIllegalArgumentException_forUnknownMarket() {
        assertThatThrownBy(() -> fileStorageService.loadFile("xx", "data.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: xx");
    }

    @Test
    void loadFile_shouldThrowNullPointerException_whenMarketIsNull() {
        assertThatThrownBy(() -> fileStorageService.loadFile(null, "data.csv"))
                .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // fileExists()
    // -------------------------------------------------------------------------

    @Test
    void fileExists_shouldReturnTrue_whenFileIsPresentInMarketDirectory() throws IOException {
        fileStorageService.init();
        Files.writeString(Path.of(cnDir).resolve("exists.csv"), "data");

        boolean result = fileStorageService.fileExists("cn", "exists.csv");

        assertThat(result).isTrue();
    }

    @Test
    void fileExists_shouldReturnFalse_whenFileIsAbsentFromMarketDirectory() {
        fileStorageService.init();

        boolean result = fileStorageService.fileExists("us", "missing.csv");

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cn", "us", "hk"})
    void fileExists_shouldReturnFalse_forAllMarketsWhenFileDoesNotExist(String market) {
        fileStorageService.init();

        assertThat(fileStorageService.fileExists(market, "ghost.csv")).isFalse();
    }

    @Test
    void fileExists_shouldReturnTrue_afterFileIsStoredViaStoreFile() throws IOException {
        fileStorageService.init();
        var content = "col1,col2\n1,2\n";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        fileStorageService.storeFile(multipartFile, "hk", "uploaded.csv");

        assertThat(fileStorageService.fileExists("hk", "uploaded.csv")).isTrue();
    }

    @Test
    void fileExists_shouldThrowIllegalArgumentException_forInvalidMarket() {
        assertThatThrownBy(() -> fileStorageService.fileExists("de", "data.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: de");
    }

    // -------------------------------------------------------------------------
    // getMarketDirectory (tested indirectly via public methods)
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
            "CN",
            "Cn",
            "cN",
            "cn"
    })
    void marketDirectory_shouldBeCaseInsensitive_forCnMarket(String market) {
        fileStorageService.init();

        Path result = fileStorageService.loadFile(market, "file.csv");

        assertThat(result.toString()).startsWith(cnDir);
    }

    @Test
    void marketDirectory_shouldMapUsCorrectly() {
        fileStorageService.init();

        Path result = fileStorageService.loadFile("US", "file.csv");

        assertThat(result.toString()).startsWith(usDir);
    }

    @Test
    void marketDirectory_shouldMapHkCorrectly() {
        fileStorageService.init();

        Path result = fileStorageService.loadFile("HK", "file.csv");

        assertThat(result.toString()).startsWith(hkDir);
    }

    @Test
    void marketDirectory_shouldThrow_forEmptyMarketString() {
        assertThatThrownBy(() -> fileStorageService.loadFile("", "data.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: ");
    }
}
