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

    /**
     * Verifies that run() calls fileStorageService.init() exactly once.
     */
    @Test
    void run_shouldCallFileStorageServiceInit() throws Exception {
        // Act
        application.run();

        // Assert
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() completes without throwing any exception
     * under normal conditions.
     */
    @Test
    void run_shouldNotThrowExceptionOnSuccess() {
        assertDoesNotThrow(() -> application.run());
    }

    /**
     * Verifies that run() prints the expected startup messages to stdout.
     */
    @Test
    void run_shouldPrintStartupMessages() throws Exception {
        // Arrange
        var originalOut = System.out;
        var outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream, true, "UTF-8"));

        try {
            // Act
            application.run();

            // Assert
            var output = outputStream.toString("UTF-8");
            assertTrue(output.contains("股票回溯策略系统后端服务已启动！"),
                    "Expected Chinese startup message to be printed");
            assertTrue(output.contains("http://localhost:8080"),
                    "Expected URL to be printed");
        } finally {
            System.setOut(originalOut);
        }
    }

    /**
     * Verifies that run() accepts varargs arguments (including none)
     * without issues — mirrors CommandLineRunner contract.
     */
    @Test
    void run_shouldAcceptVarArgsWithoutError() {
        assertDoesNotThrow(() -> application.run("--server.port=8080", "--spring.profiles.active=test"));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that when fileStorageService.init() throws an exception,
     * run() propagates it to the caller (Spring Boot will handle it).
     */
    @Test
    void run_shouldPropagateExceptionFromFileStorageServiceInit() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Storage init failed"))
                .when(fileStorageService).init();

        // Act & Assert
        var exception = assertThrows(RuntimeException.class, () -> application.run());
        assertEquals("Storage init failed", exception.getMessage());
    }

    /**
     * Verifies that when fileStorageService.init() throws a checked Exception,
     * run() propagates it correctly.
     */
    @Test
    void run_shouldPropagateCheckedExceptionFromFileStorageServiceInit() throws Exception {
        // Arrange
        doThrow(new Exception("Checked storage error"))
                .when(fileStorageService).init();

        // Act & Assert
        var exception = assertThrows(Exception.class, () -> application.run());
        assertEquals("Checked storage error", exception.getMessage());
    }

    /**
     * Verifies that no interaction beyond init() occurs on fileStorageService
     * during the run() method (no unexpected calls).
     */
    @Test
    void run_shouldHaveNoUnexpectedInteractionsOnFileStorageService() throws Exception {
        // Act
        application.run();

        // Assert
        verify(fileStorageService).init();
        verifyNoMoreInteractions(fileStorageService);
    }

    /**
     * Verifies run() is idempotent in the sense that calling it multiple times
     * results in init() being called each time.
     */
    @Test
    void run_calledMultipleTimes_shouldCallInitEachTime() throws Exception {
        // Act
        application.run();
        application.run();
        application.run();

        // Assert
        verify(fileStorageService, times(3)).init();
    }

    /**
     * Verifies that the application class is annotated with @SpringBootApplication
     * (structural / meta test).
     */
    @Test
    void applicationClass_shouldBeAnnotatedWithSpringBootApplication() {
        var annotation = StockBacktestApplication.class
                .getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class);
        assertNotNull(annotation, "@SpringBootApplication annotation must be present");
    }

    /**
     * Verifies that StockBacktestApplication implements CommandLineRunner.
     */
    @Test
    void applicationClass_shouldImplementCommandLineRunner() {
        assertTrue(
                org.springframework.boot.CommandLineRunner.class
                        .isAssignableFrom(StockBacktestApplication.class),
                "StockBacktestApplication must implement CommandLineRunner"
        );
    }
}
