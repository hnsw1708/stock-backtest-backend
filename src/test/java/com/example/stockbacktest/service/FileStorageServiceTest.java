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
    void init_createsAllThreeMarketDirectories() {
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
    void init_throwsRuntimeException_whenDirectoryCannotBeCreated() {
        // Point cnDir to a path whose parent is a file, making directory creation impossible
        Path blocker = tempDir.resolve("blocker");
        try {
            Files.createFile(blocker);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ReflectionTestUtils.setField(fileStorageService, "cnDir", blocker.resolve("cn").toString());

        assertThatThrownBy(() -> fileStorageService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not create upload directories");
    }

    // -----------------------------------------------------------------------
    // storeFile()
    // -----------------------------------------------------------------------

    @Test
    void storeFile_cn_savesFileAndReturnsFileName() throws IOException {
        fileStorageService.init();
        String fileName = "test-cn.csv";
        byte[] content = "date,open,close\n2024-01-01,100,110\n".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "cn", fileName);

        assertThat(result).isEqualTo(fileName);
        assertThat(Path.of(cnDir).resolve(fileName)).exists();
        assertThat(Files.readAllBytes(Path.of(cnDir).resolve(fileName))).isEqualTo(content);
    }

    @Test
    void storeFile_us_savesFileAndReturnsFileName() throws IOException {
        fileStorageService.init();
        String fileName = "test-us.csv";
        byte[] content = "symbol,price\nAAPL,150\n".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "us", fileName);

        assertThat(result).isEqualTo(fileName);
        assertThat(Path.of(usDir).resolve(fileName)).exists();
    }

    @Test
    void storeFile_hk_savesFileAndReturnsFileName() throws IOException {
        fileStorageService.init();
        String fileName = "test-hk.csv";
        byte[] content = "code,price\n0700,350\n".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "hk", fileName);

        assertThat(result).isEqualTo(fileName);
        assertThat(Path.of(hkDir).resolve(fileName)).exists();
    }

    @Test
    void storeFile_marketIsCaseInsensitive_CN_uppercase() throws IOException {
        fileStorageService.init();
        String fileName = "upper-cn.csv";
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());

        when(multipartFile.getInputStream()).thenReturn(inputStream);

        String result = fileStorageService.storeFile(multipartFile, "CN", fileName);

        assertThat(result).isEqualTo(fileName);
        assertThat(Path.of(cnDir).resolve(fileName)).exists();
    }

    @Test
    void storeFile_replacesExistingFileWithSameName() throws IOException {
        fileStorageService.init();
        String fileName = "replace-me.csv";
        Path target = Path.of(cnDir).resolve(fileName);
        Files.writeString(target, "old content");

        byte[] newContent = "new content".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(newContent));

        fileStorageService.storeFile(multipartFile, "cn", fileName);

        assertThat(Files.readAllBytes(target)).isEqualTo(newContent);
    }

    @Test
    void storeFile_throwsRuntimeException_whenIOExceptionOccurs() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "cn", "file.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file file.csv");
    }

    @Test
    void storeFile_throwsIllegalArgumentException_forInvalidMarket() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("x".getBytes()));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "jp", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }

    // -----------------------------------------------------------------------
    // loadFile()
    // -----------------------------------------------------------------------

    @Test
    void loadFile_cn_returnsCorrectPath() {
        Path result = fileStorageService.loadFile("cn", "data.csv");

        assertThat(result).isEqualTo(Path.of(cnDir).resolve("data.csv"));
    }

    @Test
    void loadFile_us_returnsCorrectPath() {
        Path result = fileStorageService.loadFile("us", "data.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("data.csv"));
    }

    @Test
    void loadFile_hk_returnsCorrectPath() {
        Path result = fileStorageService.loadFile("hk", "data.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("data.csv"));
    }

    @Test
    void loadFile_marketIsCaseInsensitive_US_uppercase() {
        Path result = fileStorageService.loadFile("US", "data.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("data.csv"));
    }

    @Test
    void loadFile_throwsIllegalArgumentException_forUnknownMarket() {
        assertThatThrownBy(() -> fileStorageService.loadFile("xx", "data.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: xx");
    }

    @Test
    void loadFile_throwsNullPointerException_whenMarketIsNull() {
        assertThatThrownBy(() -> fileStorageService.loadFile(null, "data.csv"))
                .isInstanceOf(NullPointerException.class);
    }

    // -----------------------------------------------------------------------
    // fileExists()
    // -----------------------------------------------------------------------

    @Test
    void fileExists_returnsFalse_whenFileDoesNotExist() {
        fileStorageService.init();

        boolean exists = fileStorageService.fileExists("cn", "nonexistent.csv");

        assertThat(exists).isFalse();
    }

    @Test
    void fileExists_returnsTrue_whenFileExists() throws IOException {
        fileStorageService.init();
        String fileName = "present.csv";
        Files.writeString(Path.of(cnDir).resolve(fileName), "data");

        boolean exists = fileStorageService.fileExists("cn", fileName);

        assertThat(exists).isTrue();
    }

    @Test
    void fileExists_returnsTrue_afterStoringFile() throws IOException {
        fileStorageService.init();
        String fileName = "stored.csv";
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        fileStorageService.storeFile(multipartFile, "us", fileName);

        assertThat(fileStorageService.fileExists("us", fileName)).isTrue();
    }

    @Test
    void fileExists_returnsFalse_forDifferentMarket() throws IOException {
        fileStorageService.init();
        String fileName = "only-in-cn.csv";
        Files.writeString(Path.of(cnDir).resolve(fileName), "data");

        // File stored in CN should not appear in US
        assertThat(fileStorageService.fileExists("us", fileName)).isFalse();
    }

    @Test
    void fileExists_throwsIllegalArgumentException_forInvalidMarket() {
        assertThatThrownBy(() -> fileStorageService.fileExists("invalid", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: invalid");
    }

    // -----------------------------------------------------------------------
    // getMarketDirectory – exercised via market case-insensitivity
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"cn", "CN", "Cn", "cN"})
    void loadFile_allCnCaseVariants_resolveToSameDirectory(String market) {
        Path result = fileStorageService.loadFile(market, "file.csv");
        assertThat(result).isEqualTo(Path.of(cnDir).resolve("file.csv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"us", "US", "Us", "uS"})
    void loadFile_allUsCaseVariants_resolveToSameDirectory(String market) {
        Path result = fileStorageService.loadFile(market, "file.csv");
        assertThat(result).isEqualTo(Path.of(usDir).resolve("file.csv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hk", "HK", "Hk", "hK"})
    void loadFile_allHkCaseVariants_resolveToSameDirectory(String market) {
        Path result = fileStorageService.loadFile(market, "file.csv");
        assertThat(result).isEqualTo(Path.of(hkDir).resolve("file.csv"));
    }
}
