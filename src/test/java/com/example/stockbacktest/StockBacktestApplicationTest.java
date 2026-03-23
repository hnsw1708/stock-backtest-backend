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
    private StockBacktestApplication stockBacktestApplication;

    /**
     * Verifies that run() calls fileStorageService.init() exactly once.
     */
    @Test
    void run_shouldCallFileStorageServiceInit() throws Exception {
        // Act
        stockBacktestApplication.run();

        // Assert
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() completes without throwing any exception under normal conditions.
     */
    @Test
    void run_shouldNotThrowExceptionWhenInitSucceeds() {
        // Arrange
        // fileStorageService.init() does nothing by default (Mockito stub)

        // Act & Assert
        assertDoesNotThrow(() -> stockBacktestApplication.run());
    }

    /**
     * Verifies that run() propagates exceptions thrown by fileStorageService.init().
     */
    @Test
    void run_shouldPropagateExceptionWhenInitFails() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Storage init failed"))
                .when(fileStorageService).init();

        // Act & Assert
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> stockBacktestApplication.run()
        );
        assertEquals("Storage init failed", thrown.getMessage());
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() prints the expected startup messages to System.out.
     */
    @Test
    void run_shouldPrintExpectedStartupMessages() throws Exception {
        // Arrange
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput, true, "UTF-8"));

        try {
            // Act
            stockBacktestApplication.run();
        } finally {
            System.setOut(originalOut);
        }

        // Assert
        String output = capturedOutput.toString("UTF-8");
        assertTrue(output.contains("股票回溯策略系统后端服务已启动！"),
                "Output should contain Chinese startup message");
        assertTrue(output.contains("访问地址: http://localhost:8080"),
                "Output should contain the access URL");
    }

    /**
     * Verifies that run() works correctly when called with explicit varargs arguments
     * (simulating command-line args passed by Spring Boot).
     */
    @Test
    void run_shouldWorkWithExplicitArgs() throws Exception {
        // Arrange
        String[] args = {"--server.port=8080", "--spring.profiles.active=test"};

        // Act & Assert
        assertDoesNotThrow(() -> stockBacktestApplication.run(args));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() works correctly when called with an empty args array.
     */
    @Test
    void run_shouldWorkWithEmptyArgs() throws Exception {
        // Arrange
        String[] emptyArgs = {};

        // Act & Assert
        assertDoesNotThrow(() -> stockBacktestApplication.run(emptyArgs));
        verify(fileStorageService, times(1)).init();
    }

    /**
     * Verifies that run() does not interact with fileStorageService beyond calling init().
     */
    @Test
    void run_shouldHaveNoOtherInteractionsWithFileStorageService() throws Exception {
        // Act
        stockBacktestApplication.run();

        // Assert
        verify(fileStorageService, times(1)).init();
        verifyNoMoreInteractions(fileStorageService);
    }

    /**
     * Verifies that run() propagates a checked Exception thrown by fileStorageService.init().
     */
    @Test
    void run_shouldPropagateCheckedExceptionWhenInitFails() throws Exception {
        // Arrange
        doThrow(new Exception("Checked storage error"))
                .when(fileStorageService).init();

        // Act & Assert
        Exception thrown = assertThrows(
                Exception.class,
                () -> stockBacktestApplication.run()
        );
        assertEquals("Checked storage error", thrown.getMessage());
    }

    /**
     * Verifies that multiple sequential run() invocations each call init() once per invocation.
     */
    @Test
    void run_shouldCallInitOnEachInvocation() throws Exception {
        // Act
        stockBacktestApplication.run();
        stockBacktestApplication.run();
        stockBacktestApplication.run();

        // Assert
        verify(fileStorageService, times(3)).init();
    }
}
