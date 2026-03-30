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
    void init_createsAllMarketDirectories() {
        fileStorageService.init();

        assertThat(Path.of(cnDir)).exists().isDirectory();
        assertThat(Path.of(usDir)).exists().isDirectory();
        assertThat(Path.of(hkDir)).exists().isDirectory();
    }

    @Test
    void init_isIdempotent_whenDirectoriesAlreadyExist() {
        fileStorageService.init();
        // calling a second time must not throw
        fileStorageService.init();

        assertThat(Path.of(cnDir)).exists();
        assertThat(Path.of(usDir)).exists();
        assertThat(Path.of(hkDir)).exists();
    }

    @Test
    void init_throwsRuntimeException_whenDirectoryCannotBeCreated() {
        // Point cnDir to an invalid path that cannot be created (null-byte is illegal on all OS)
        ReflectionTestUtils.setField(fileStorageService, "cnDir", tempDir + "/\0invalid");

        assertThatThrownBy(() -> fileStorageService.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not create upload directories");
    }

    // -----------------------------------------------------------------------
    // storeFile()
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"cn", "CN", "Cn"})
    void storeFile_returnFileName_forCnMarketCaseInsensitive(String market) throws IOException {
        fileStorageService.init();
        byte[] content = "test,data\n1,2".getBytes();
        InputStream stream = new ByteArrayInputStream(content);
        when(multipartFile.getInputStream()).thenReturn(stream);

        String result = fileStorageService.storeFile(multipartFile, market, "test.csv");

        assertThat(result).isEqualTo("test.csv");
        assertThat(Path.of(cnDir).resolve("test.csv")).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {"us", "US", "Us"})
    void storeFile_returnFileName_forUsMarketCaseInsensitive(String market) throws IOException {
        fileStorageService.init();
        InputStream stream = new ByteArrayInputStream("price,vol".getBytes());
        when(multipartFile.getInputStream()).thenReturn(stream);

        String result = fileStorageService.storeFile(multipartFile, market, "us_data.csv");

        assertThat(result).isEqualTo("us_data.csv");
        assertThat(Path.of(usDir).resolve("us_data.csv")).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hk", "HK", "Hk"})
    void storeFile_returnFileName_forHkMarketCaseInsensitive(String market) throws IOException {
        fileStorageService.init();
        InputStream stream = new ByteArrayInputStream("hk,stock".getBytes());
        when(multipartFile.getInputStream()).thenReturn(stream);

        String result = fileStorageService.storeFile(multipartFile, market, "hk_data.csv");

        assertThat(result).isEqualTo("hk_data.csv");
        assertThat(Path.of(hkDir).resolve("hk_data.csv")).exists();
    }

    @Test
    void storeFile_replacesExistingFile() throws IOException {
        fileStorageService.init();
        Path existing = Path.of(cnDir).resolve("replace_me.csv");
        Files.writeString(existing, "old content");

        byte[] newContent = "new content".getBytes();
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(newContent));

        fileStorageService.storeFile(multipartFile, "cn", "replace_me.csv");

        assertThat(Files.readString(existing)).isEqualTo("new content");
    }

    @Test
    void storeFile_throwsRuntimeException_forInvalidMarket() throws IOException {
        fileStorageService.init();

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "jp", "test.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }

    @Test
    void storeFile_throwsRuntimeException_whenIOExceptionOccurs() throws IOException {
        fileStorageService.init();
        when(multipartFile.getInputStream()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "cn", "fail.csv"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not store file fail.csv")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void storeFile_throwsIllegalArgumentException_forNullMarket() throws IOException {
        fileStorageService.init();

        // market.toLowerCase() will throw NullPointerException
        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, null, "file.csv"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void storeFile_throwsIllegalArgumentException_forEmptyMarket() throws IOException {
        fileStorageService.init();

        assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, "", "file.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market:");
    }

    // -----------------------------------------------------------------------
    // loadFile()
    // -----------------------------------------------------------------------

    @Test
    void loadFile_returnCorrectPath_forCnMarket() {
        Path result = fileStorageService.loadFile("cn", "data.csv");

        assertThat(result).isEqualTo(Path.of(cnDir).resolve("data.csv"));
    }

    @Test
    void loadFile_returnCorrectPath_forUsMarket() {
        Path result = fileStorageService.loadFile("us", "data.csv");

        assertThat(result).isEqualTo(Path.of(usDir).resolve("data.csv"));
    }

    @Test
    void loadFile_returnCorrectPath_forHkMarket() {
        Path result = fileStorageService.loadFile("hk", "data.csv");

        assertThat(result).isEqualTo(Path.of(hkDir).resolve("data.csv"));
    }

    @Test
    void loadFile_throwsIllegalArgumentException_forInvalidMarket() {
        assertThatThrownBy(() -> fileStorageService.loadFile("jp", "data.csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid market: jp");
    }
}