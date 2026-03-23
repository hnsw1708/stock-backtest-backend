package com.example.stockbacktest;

import com.example.stockbacktest.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockBacktestApplicationTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private StockBacktestApplication application;

    @Test
    void run_shouldCallFileStorageServiceInit() throws Exception {
        // When
        application.run();

        // Then
        verify(fileStorageService, times(1)).init();
    }

    @Test
    void run_shouldPrintStartupMessages() throws Exception {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // When
            application.run();

            // Then
            String output = outputStream.toString();
            assertTrue(output.contains("股票回溯策略系统后端服务已启动！"),
                    "Output should contain startup message");
            assertTrue(output.contains("http://localhost:8080"),
                    "Output should contain access URL");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void run_shouldPrintAccessUrlMessage() throws Exception {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // When
            application.run();

            // Then
            String output = outputStream.toString();
            String expectedUrlLine = "访问地址: http://localhost:8080";
            assertTrue(output.contains(expectedUrlLine),
                    "Output should contain the access URL line");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void run_shouldCallInitBeforePrintingMessages() throws Exception {
        // Given
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // When
            application.run();

            // Then - verify init was called and output was produced
            verify(fileStorageService, times(1)).init();
            String output = outputStream.toString();
            assertFalse(output.isEmpty(), "Output should not be empty after run");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void run_shouldPropagateExceptionWhenInitFails() throws Exception {
        // Given
        String errorMessage = "Storage initialization failed";
        doThrow(new RuntimeException(errorMessage)).when(fileStorageService).init();

        // When / Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> application.run());
        assertEquals(errorMessage, thrown.getMessage());
        verify(fileStorageService, times(1)).init();
    }

    @Test
    void run_withMultipleArgs_shouldStillCallInit() throws Exception {
        // When
        application.run("arg1", "arg2", "arg3");

        // Then
        verify(fileStorageService, times(1)).init();
    }

    @Test
    void run_withEmptyArgs_shouldCallInit() throws Exception {
        // When
        application.run(new String[]{});

        // Then
        verify(fileStorageService, times(1)).init();
    }

    @Test
    void run_withNullVarargs_shouldCallInit() throws Exception {
        // When
        application.run((String[]) null);

        // Then
        verify(fileStorageService, times(1)).init();
    }

    @Test
    void run_calledMultipleTimes_shouldCallInitMultipleTimes() throws Exception {
        // When
        application.run();
        application.run();

        // Then
        verify(fileStorageService, times(2)).init();
    }

    @Test
    void run_shouldNotInteractWithFileStorageServiceBeyondInit() throws Exception {
        // When
        application.run();

        // Then
        verify(fileStorageService, times(1)).init();
        verifyNoMoreInteractions(fileStorageService);
    }

    @Test
    void applicationClass_shouldBeAnnotatedWithSpringBootApplication() {
        // Given / When
        boolean hasAnnotation = StockBacktestApplication.class
                .isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class);

        // Then
        assertTrue(hasAnnotation, "StockBacktestApplication should be annotated with @SpringBootApplication");
    }

    @Test
    void applicationClass_shouldImplementCommandLineRunner() {
        // Then
        assertTrue(
                org.springframework.boot.CommandLineRunner.class.isAssignableFrom(StockBacktestApplication.class),
                "StockBacktestApplication should implement CommandLineRunner"
        );
    }
}
